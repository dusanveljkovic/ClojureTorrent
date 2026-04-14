(ns clojuretorrent.gui.subs.core
  (:require [cljfx.api :as fx]))

(defn sub-torrents [ctx]
  (fx/sub-val ctx :torrents))

(defn sub-current-filter [ctx]
  (fx/sub-val ctx :filter))

(defn sub-style [ctx]
  (fx/sub-val ctx :style))

(defn sub-selected-id [ctx]
  (fx/sub-val ctx :selected-id))
