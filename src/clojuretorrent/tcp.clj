(ns clojuretorrent.tcp
  (:require [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [taoensso.timbre :as timbre]
            [gloss.core :as gloss]
            [gloss.io :as io]
            [clojuretorrent.file :as tfile]))


(def handshake-codec
  (gloss/ordered-map
    :pstr-len :byte
    :pstr (gloss/string :ascii :length 19)
    :reserved (gloss/finite-block 8)
    :info-hash (gloss/finite-block 20)
    :peer-id (gloss/finite-block 20)))

(def message-codec
  (gloss/compile-frame
    (gloss/header 
      :int32
      (fn [length]
        (if (zero? length)
          (gloss/compile-frame :byte (constantly 0) (constantly 0)) ; keep-alive
          (gloss/ordered-map 
            :message-id :byte
            :payload (gloss/finite-block (dec length)))))
      (fn [body]
        (if (= body :keep-alive)
          0
          (inc (alength ^bytes (:payload body))))))))

(def mfile (tfile/read-file "./torrents/ubuntu.torrent"))

(def message-id->string 
  {-1 "HANDSHAKE"
   0 "CHOKE"
   1 "UNCHOKE"
   2 "INTERESTED"
   3 "NOT INTERESTED"
   4 "HAVE"
   5 "BITFIELD"
   6 "REQUEST"
   7 "PIECE"
   8 "CANCEL"
   9 "PORT"})

(defn string-handler 
  "Transformer that adds a :string-repr key to a message map"
  [message]
  (if (map? message) 
    (let [id (get message :message-id -1)
          string-repr (get message-id->string id "UNKNOWN")]
      (assoc message :string-repr string-repr))
    message))

(defn wrap-duplex-stream
  "Wraps a raw TCP stream with `codec` returning a duplex stream
  where you put/take Clojure maps instead of raw bytes"
  [stream codec]
  (let [out (s/stream)]
    (s/connect 
      (s/map #(io/encode codec %) out)
      stream)
    (s/splice
      out 
      (io/decode-stream stream codec))))

(defn send-handshake
  "Encodes and sends a BitTorrent handshake on directly onto the raw TCP stream 
  bypassing the codec wrapping. Returns a deferred boolean"
  [tcp-stream info-hash peer-id]
  (let [handshake {:pstr-len 19
                   :pstr "BitTorrent protocol"
                   :reserved (byte-array [0 0 0 0 0 0 0 0])
                   :info-hash info-hash
                   :peer-id peer-id}
        bytes (io/encode handshake-codec handshake)]
    (d/catch 
      (s/put! tcp-stream bytes)
      (fn [e]
        (timbre/error e "[!] Failed to send handshake")
        false))))

(defn read-handshake
  "Reads exactly one handshake frame from the raw TCP stream.
  Returns a deferred resolving to the decoded handshake map, or nil on failure"
  [tcp-stream]
  (d/chain 
    (s/take! tcp-stream)
    (fn [buf]
      (when buf
        (try 
          (io/decode handshake-codec tcp-stream)
          (catch Exception e
            (timbre/error e "[!] Failed to decode handshake")
            nil))))))

(defn handle-messages
  "Consumes all incoming messages from `stream`, running each through the
  `custom-transformers` pipeline before logging. "
  [stream custom-transformers]
  (s/consume
    (fn [message]
      (let [transformed-message (reduce (fn [acc f] (f acc)) message custom-transformers)]
        (timbre/info "[C] Got message: " transformed-message)))
    stream))

(defn handle-close
  "Registers a callbacks that is called when a connection is closed"
  [stream host port]
  (s/on-closed stream
    (fn []
      (timbre/info "[C] Connection closed by peer " host ":" port))))

(defn connect-to-peer
  "Opens a TCP connection to `host`:`port`, exchanges handshakes and starts consuming messages
  
  Options:
    :custom-transformers - seq of (fn [message] message) applied in order
                           to each incoming message (default: [identity])"
  [host port raw-info-hash peer-id & {:keys [custom-transformers] 
                                      :or {custom-transformers [identity]}}]
  (-> (tcp/client {:host host :port port})
      (d/chain 
        (fn [tcp-stream]
          (timbre/info "[C] Connected to peer " host ":" port)
          (d/chain 
            (send-handshake tcp-stream raw-info-hash peer-id)
            (fn [sent?]
              (if-not sent?
                (do (s/close! tcp-stream) nil)
                (read-handshake tcp-stream)))
            (fn [other-handshake]
              (if-not other-handshake
                (do (timbre/error "[!] Handshake failed with " host ":" port) nil)
                (do 
                  (timbre/info "[+] Handshake OK, peer-id: " (seq (:peer-id other-handshake)))
                  tcp-stream)))))
        (fn [tcp-stream]
          (when tcp-stream
            (let [client (wrap-duplex-stream tcp-stream message-codec)]
              (handle-messages client custom-transformers)
              (handle-close client host port)
              client))))
        (d/catch 
          (fn [e]
            (timbre/error e "[!] Failed to connect to peer " host ":" port)
            nil))))

(defn close-peer-connection
  "Closes the peer stream if it is still open"
  [client]
  (when (s/stream? client)
    (s/close! client)))

(defn- send-message
  "Puts a message map onto the stream.
  Returns a deferred resolving to true/false"
  [client msg]
  (if (s/stream? client)
    (d/catch 
      (s/put! client msg)
      (fn [e]
        (timbre/error e "[!] Failed to send message" msg)
        false))
    (do 
      (timbre/warn "[!] send-message called on non-stream" client)
      (d/success-deferred false))))

(defn send-choke          [client] (send-message client {:message-id 0 :payload (byte-array 0)}))
(defn send-unchoke        [client] (send-message client {:message-id 1 :payload (byte-array 0)}))
(defn send-interested     [client] (send-message client {:message-id 2 :payload (byte-array 0)}))
(defn send-not-interested [client] (send-message client {:message-id 3 :payload (byte-array 0)}))

(comment 
  (def mfile (tfile/read-file "./torrents/ubuntu.torrent"))

  (def conn (connect-to-peer "0.0.0.0" 19325
                             (:raw (tfile/info-hash mfile))
                             (.getBytes "-qB50309999999999999")
                             :custom-transformers [string-handler]))

  @conn

  (send-interested @conn)
  (send-choke @conn)

  @(s/take! @conn)

  (close-peer-connection @conn))
