(ns djirtorrent.file
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [djirtorrent.bencode :refer [read-bstring write-bstring]])
  (:import java.security.MessageDigest
           java.math.BigInteger))

(defn slurp-bytes
  [file]
  (with-open [in (io/input-stream file)
              out (java.io.ByteArrayOutputStream.)]
    (io/copy in out)
    (.toByteArray out)))

(defn read-file
  [filename]
  (->> filename
       slurp-bytes
       read-bstring
       :data))

(defn sha1 [input]
  (let [digest (.digest (MessageDigest/getInstance "SHA-1") input)]
    {:raw digest :formatted (format "%040x" (BigInteger. 1 digest))}))

(defn info-hashh
  [torrent-map]
  (-> (torrent-map "info")
       write-bstring
       (.getBytes "ISO-8859-1")
       sha1))

(def info-hash
  (memoize info-hashh))

(defn announce-url
  [metadata]
  (get metadata "announce"))

(defn length
  [metadata]
  (get-in metadata ["info" "length"]))

