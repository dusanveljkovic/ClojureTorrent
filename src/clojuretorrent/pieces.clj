(ns clojuretorrent.pieces
  (:require [clojuretorrent.fil :as tfile])
  (:import [java.io RandomAccessFile File]
           [java.util.concurrent.atomic AtomicInteger]))

(def BLOCK_SIZE (bit-shift-left 1 14))

(defn- ensure-dir 
  [path]
  (let [f (File. path)]
    (when-not (.exists f) (.mkdirs f))
    f))

(defn open-files
  [torrent output-dir]
  (let [dir (ensure-dir output-dir)]
    (reduce )))
