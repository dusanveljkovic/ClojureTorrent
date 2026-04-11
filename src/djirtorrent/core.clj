(ns djirtorrent.core
  (:require [djirtorrent.file :refer [read-file info-hash]]
            [djirtorrent.bencode :refer [write-bstring]]
            [cljfx.api :as fx]))


(def ubuntu-file "./torrents/ubuntu.torrent")

(def output (read-file ubuntu-file))

(info-hash output)


