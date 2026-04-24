(ns clojuretorrent.download
  (:require [taoensso.timbre :as timbre]
            [clojuretorrent.file :as tfile]
            [manifold.deferred :as d]
            [clojuretorrent.tcp :as tcp])
  (:import [java.nio ByteBuffer]))

(defn make-download-state
  "Return an atom holding mutable download state for one peer"
  [num-pieces piece-length total-length]
  (atom {:peer-choked? true 
         :peer-interested? false 
         :peer-bitfield (boolean-array num-pieces false)
         :pieces-needed (set (range num-pieces))
         :pieces-done #{}
         :piece-buffers {}
         :num-pieces num-pieces
         :piece-length piece-length
         :total-length total-length}))

(defn bytes->int 
  "Big endian 4 bytes to int"
  [b offset]
  (-> (ByteBuffer/wrap b offset 4) .getInt))

(defn int->bytes 
  "int to big endian 4 bytes"
  [n]
  (-> (ByteBuffer/allocate 4) (.putInt n) .array))

(defn encode-request-payload
  "Builds the request payload"
  [piece-index begin block-len]
  (let [buf (ByteBuffer/allocate 12)]
    (doto buf 
      (.putInt piece-index)
      (.putInt begin)
      (.putInt block-len))
    (.array buf)))

(defn parse-piece-payload
  "Decodes a piece payload into {:index :begin :block}"
  [payload]
  {:index (bytes->int payload 0)
   :begin (bytes->int payload 4)
   :block (java.util.Arrays/copyOfRange payload 8 (alength payload))})

(defn parse-bitfield 
  "Decodes a BITFIELD payload into boolean array of length"
  [payload num-pieces]
  (let [bits (boolean-array num-pieces)]
    (dotimes [i num-pieces]
      (let [byte-index (quot i 8)
            bit-index (- 7 (mod i 8))
            byte-val (aget payload byte-index)]
        (aset bits i (not (zero? (bit-and byte-val (bit-shift-left 1 bit-index)))))))))

(defn piece-length-for 
  [state index]
  (let [{:keys [num-pieces piece-length total-length]} @state]
    (if (< index (dec num-pieces))
      piece-length
      (- total-length (* (dec num-pieces) piece-length)))))

(defn handle-piece! 
  [client state hashes output-path message]
  (let [{:keys [index begin block]} (parse-piece-payload (:payload message))]
    (timbre/info "Piece " index " " begin " " block)))

(defn request-next-piece! 
  [client state]
  (when-not (:peer-choked? @state)
    (let [{:keys [pieces-needed peer-bitfield piece-length total-length num-pieces]} @state 
          available (->> pieces-needed
                         (filter #(aget peer-bitfield %))
                         first)]
      (if available 
        (let [plen (piece-length-for state available)
              blen plen
              payload (encode-request-payload available 0 blen)]
          (timbre/info "Payload " payload))))))

(defn handle-message
  [client state hashes output-path message]
  (case (:message-id message)
    0 (do (swap! state assoc :peer-choked? true)
          (timbre/info "[I] Peer choked"))

    1 (do (swap! state assoc :peer-choked? false)
          (timbre/info "[I] Peer unchoked")
          (request-next-piece! client state))

    4 (let [index (bytes->int (:payload message) 0)]
        (timbre/info "[I] HAVE " index)
        (aset ^booleans (:peer-bitfield @state) index true))

    5 (let [bitfield (parse-bitfield (:payload message) (:num-pieces @state))]
        (timbre/info "[I] Bitfield ")
        (swap! state assoc :peer-bitfield bitfield))

    7 (do (timbre/info "[I] Piece ")
          (handle-piece! client state hashes output-path message))))

(defn download 
  [host port torrent-path output-path]
  (let [file (tfile/read-file torrent-path)
        raw-hash (:raw (tfile/info-hash file))
        peer-id (.getBytes "-CLJ10000000000000000")
        num-pieces (tfile/num-pieces file)
        piece-len (tfile/piece-length file)
        total-len (tfile/length file)
        hashes (tfile/piece-hashes file)
        state (make-download-state num-pieces piece-len total-len)
        message-handler #(handle-message nil state hashes output-path %)]
    (d/chain 
      (tcp/connect-to-peer host port raw-hash peer-id :message-handler message-handler)
      (fn [client]
        (when client 
          (d/chain 
            ; (tcp/send-interested client)
            (fn [_]
              (timbre/info "[I] Sent INTERESTED")
              client)))))))

(comment 
  @(download "localhost" 62288 "./torrents/test.torrent" ""))

