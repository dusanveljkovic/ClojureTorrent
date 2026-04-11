(ns clojuretorrent.core
  (:require [clojuretorrent.file :refer [read-file info-hash]]
            [clojuretorrent.bencode :refer [write-bstring]]
            [cljfx.api :as fx]))


(def ubuntu-file "./torrents/ubuntu.torrent")

(def output (read-file ubuntu-file))

(info-hash output)


