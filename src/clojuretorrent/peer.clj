(ns clojuretorrent.peer
  (:import [java.net Socket InetSocketAddress]
           [java.io DataInputStream DataOutputStream BufferedInputStream BufferedOutputStream]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]) 
  (:require
    [clojuretorrent.file :as tfile]))

(def PROTOCOL_STR "BitTorrent protocol")
(def PROTOCOL_BYTES (.getBytes PROTOCOL_STR StandardCharsets/US_ASCII))
(def RESERVED_BYTES (byte-array 8))

(defn- read-exact
  [^DataInputStream in n]
  (let [buf (byte-array n)]
    (.readFully in buf)
    buf))

(defn- make-handshake
  [info-hash peer-id]
  (let [handshake (ByteBuffer/allocate 68)]
    (.put handshake (byte (alength PROTOCOL_BYTES)))
    (.put handshake PROTOCOL_BYTES)
    (.put handshake RESERVED_BYTES)
    (.put handshake info-hash)
    (.put handshake peer-id)
    (.array handshake)))


(defn- connect 
  "Connects to remote peer at ip:port and returns socket and input and output streams"
  [ip port & {:keys [timeout-ms]
            :or {timeout-ms 10000}}]
  (let [sock (doto (Socket.)
               (.connect (InetSocketAddress. ip port) timeout-ms)
               (.setSoTimeout timeout-ms)
               (.setTcpNoDelay true))
        in (DataInputStream. (BufferedInputStream. (.getInputStream sock)))
        out (DataOutputStream. (BufferedOutputStream. (.getOutputStream sock)))]
    {:socket sock 
     :in in 
     :out out}))

(defn- read-handshake
  [in]
  (let [pstrlen (.readUnsignedByte in)
        pstr (read-exact in (alength PROTOCOL_BYTES))
        _reserved (read-exact in 8)
        remote-info-hash (read-exact in 20)
        remote-peer-id (read-exact in 20)]
    {:pstrlen pstrlen 
     :pstr pstr 
     :remote-info-hash remote-info-hash 
     :remote-peer-id remote-peer-id}))

(defn do-handshake
  [ip port info-hash peer-id]
  (let [{:keys [socket in out]} (connect ip port)
        handshake (make-handshake info-hash peer-id)]
    (.write out handshake)
    (.flush out)
    (let [{:keys [pstrlen pstr remote-info-hash remote-peer-id]} (read-handshake in)]
      (when (not= pstrlen (alength PROTOCOL_BYTES))
        (throw (ex-info "Unexpected pstrlen" {:pstrlen pstrlen})))
      (when (not= (String. pstr StandardCharsets/US_ASCII) PROTOCOL_STR)
        (throw (ex-info "Unknown pstr" {:pstr pstr})))
      (when (not (java.util.Arrays/equals ^bytes info-hash remote-info-hash))
        (throw (ex-info "Info-hash mismatch" {:info-hash info-hash :remote-info-hash remote-info-hash})))
      {:socket socket 
       :in in 
       :out out 
       :peer-id remote-peer-id 
       :ip ip 
       :port port 
       :choked? true 
       :choking? true
       :interested? false 
       :bitfield nil})))

(defn close! 
  [{:keys [socket]}]
  (try (.close socket) (catch Exception _)))

(defn parse-msg
  [msg-id payload payload-len]
  (let [buf-paylaod (ByteBuffer/wrap payload)]
    (case msg-id 
      0 {:type :choke}
      1 {:type :unchoke}
      2 {:type :interested}
      3 {:type :not-interested}
      4 (let [idx (.readInt buf-paylaod)]
          {:type :have :index idx})
      5 {:type :bitfield :bitfield payload}
      6 (let [idx (.readInt buf-paylaod)
              begin (.readInt buf-paylaod)
              length (.readInt buf-paylaod)]
          {:type :request :index idx :begin begin :length length})
      7 (let [idx (.readInt buf-paylaod)
              begin (.readInt buf-paylaod)
              block (read-exact payload (- payload-len 8))]
          {:type :piece :index idx :begin begin :block block})
      (let [_ (when (pos? payload-len) (read-exact buf-paylaod payload-len))]
        {:type :unknown :id msg-id}))))

(defn read-message
  [{:keys [^DataInputStream in]}]
  (let [length (.readInt in)]
    (if (zero? length)
      {:type :keepalive}
      (let [msg-id (.readUnsignedByte in)
            payload-len (- length 1)
            payload (read-exact in payload-len)]
        (parse-msg msg-id payload payload-len)))))

(comment 
  (def info-hash (:raw (tfile/info-hash (tfile/read-file "./torrents/test.torrent"))))
  (def peer-id (.getBytes "-qB50309999999999999"))
  (def handshake (.array (make-handshake info-hash peer-id)))
  
  (def conn (do-handshake "localhost" 62288 info-hash peer-id)))
