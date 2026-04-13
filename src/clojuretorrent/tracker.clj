(ns clojuretorrent.tracker
  (:require [clj-http.client :as client]
            [clojuretorrent.bencode :refer [read-bstring]]
            [clojure.string]))

(defn urlencode-byte-array 
  [b-array]
  (apply str 
         (map (fn [b]
                (let [unsigned-b (bit-and b 0xFF)]
                  (if (or (<= 0x30 unsigned-b 0x39)
                          (<= 0x41 unsigned-b 0x5A)
                          (<= 0x61 unsigned-b 0x7A))
                    (char unsigned-b)
                    (format "%%%02X" unsigned-b))))
              b-array)))

(defn add-query-params
  [url params-map]
  (->> params-map
       (map (fn [[key val]] (str key "=" val)))
       (clojure.string/join "&")
       (str url "?")))

(defn send-request
  "Sends an HTTP request to a BitTorrent tracker to query for torrent information.
  Returns Clojure map containing the data
  
  Options:
    :port - client port, default: 6881
    :downloaded - bytes downloads, default: 0
    :uploaded - bytes uploaded, default: 0"
  [announce-url info-hash peer-id left & {:keys [port downloaded uploaded] :or {port 6881 downloaded 0 uploaded 0}}]
  (let [encoded-hash (urlencode-byte-array (:raw info-hash))
        url-with-params (add-query-params announce-url 
                                          {"info_hash" encoded-hash
                                           "peer_id" peer-id
                                           "port" port
                                           "uploaded" uploaded
                                           "downloaded" downloaded
                                           "left" left})] 
    (->> (client/get url-with-params {:debug true :as :byte-array :save-request? true})
         :body
         read-bstring
         :data)))

(defn peers
  [response]
  (map (fn [x] {:ip (x "ip") :port (x "port") :peer-id (x "peer id")}) (get response "peers")))

