(ns clojuretorrent.tcp
  (:require [aleph.tcp :as tcp]
            [manifold.stream :as s]
            [manifold.deferred :as d]
            [taoensso.timbre :as timbre]
            [gloss.core :as gloss]
            [gloss.core.codecs :as gcodecs]
            [gloss.io :as io]
            [djirtorrent.file :as tfile])
  (:import (java.nio ByteBuffer)))


(def handshake-format
  (gloss/ordered-map
    :pstr-len :byte
    :pstr (gloss/string :ascii :length 19)
    :reserved (gloss/finite-block 8)
    :info-hash (gloss/finite-block 20)
    :peer-id (gloss/finite-block 20)))

(def message-format
  (gloss/compile-frame
    (gloss/header 
      :int32
      (fn [length]
        (if (zero? length)
          :keep-alive
          (gloss/ordered-map 
            :message-id :byte
            :payload (gloss/finite-block (dec length)))))
      (fn [body]
        (inc (count (:payload body)))))))

(def mfile (tfile/read-file "./torrents/ubuntu.torrent"))

(defn string-handler [message]
  (let [string-repr (condp = (get message :message-id -1)
                -1 "HANDSHAKE"
                 0 "CHOKE"
                 1 "UNCHOKE"
                 2 "INTERESTED"
                 3 "NOT INTERESTED"
                 4 "HAVE"
                 5 "BITFIELD"
                 6 "REQUEST"
                 7 "PIECE"
                 8 "CANCEL"
                 9 "PORT"
                 )]
    (assoc message :string-repr string-repr)))

(defn wrap-duplex-stream
  [stream protocol]
  (let [out (s/stream)]
    (s/connect 
      (s/map #(io/encode protocol %) out)
      stream)
    
    (s/splice
      out 
      (io/decode-stream stream protocol))))

(def protocol
  (gloss/compile-frame
    (gloss/header-choice
      :byte
      (fn [first-byte]
        (println (= first-byte 19))
        (if (= first-byte 19)
          handshake-format
          message-format))
      (fn [body]
        (cond 
          (:pstr body) (byte 19)
          (:message-id body) (:message-id body)  
          )))))

(defn send-handshake [stream info-hash peer-id]
  (let [handshake {:pstr-len 19
                   :pstr "BitTorrent protocol"
                   :reserved (byte-array [0 0 0 0 0 0 0 0])
                   :info-hash info-hash
                   :peer-id peer-id}]
    (s/put! stream handshake)))

(defn handle-messages [stream custom-transformers]
  (s/consume
    (fn [message]
      (let [transformed-message (reduce (fn [acc f] (f acc)) message custom-transformers)]
        (timbre/info "[C] Got message: " transformed-message)))
    stream))

(defn handle-close [stream]
  (s/on-closed stream
    (fn []
      (timbre/info "Connection closed by peer"))))

(defn connect-to-peer [host port raw-info-hash peer-id & {:keys [custom-transformers] :or {custom-transformers [identity]}}]
  (d/let-flow [client  @(d/chain (tcp/client {:host host :port port}) #(wrap-duplex-stream % protocol))] 
   (println "[C] Connected to peer: " host ":" port)

   (send-handshake client raw-info-hash peer-id)
   (handle-messages client custom-transformers)
   ; (d/let-flow [data (seq (s/take! client))]
   ;   (timbre/info "[C] received: " (seq data)))
   ; (d/let-flow [handshake-response (read-handshake client)]
   ;   (if handshake-response
   ;     (do 
   ;       (timbre/info "[+] Handshake successful: " handshake-response)
   ;
   ;       (handle-messages client custom-transformers))
   ;
   ;     (timbre/error "[!] Handshake failed")))
   (handle-close client)
   client))

(defn close-peer-connection [client]
  (when (s/stream? client)
    (s/close! client)))

(defn send-choke [client]
  (when (s/stream? @client)
    (s/put! @client {:message-id 0 :payload []})))
(defn send-unchoke [client]
  (when (s/stream? @client)
    (s/put! @client {:message-id 1 :payload []})))
(defn send-interested [client]
  (when (s/stream? @client)
    (s/put! @client {:message-id 2 :payload []})))
(defn send-not-interested [client]
  (when (s/stream? @client)
    (s/put! @client {:message-id 3 :payload []})))

; [63, -102, -84, 21, -116, 125, -24, -33, -54, -79, 113, -22, 88, -95,
;  122, -85, -33, 127, -68, -109]

; (def conn (connet-to-peer "0.0.0.0" 19325 (:raw (tfile/info-hash mfile)) (.getBytes "-qB50309999999999999") {:custom-transformers [string-handler]}))
; @conn
; (s/put! @conn {:message-id 2 :payload []})
; @(s/take! @conn)
 ; (s/close! @conn)
;
; (def client @(tcp/client {:host "0.0.0.0" :port 19325}))
;
; (io/decode handshake-format (byte-array (repeat 69 0)))
;
;
