(ns clojuretorrent.gui.subs.torrents
   (:require [cljfx.api :as fx]
             [clojuretorrent.gui.subs.core :as subs]))

 (defn sub-total [ctx]
   (count (fx/sub-ctx ctx subs/sub-torrents)))

(defn sub-state [ctx state]
  (let [torrents (fx/sub-ctx ctx subs/sub-torrents)]
   (if (= state :all)
    torrents
    (filter #(= state (:status %)) torrents))))

(defn sub-state-count [ctx state]
  (count (fx/sub-ctx ctx sub-state state)))

