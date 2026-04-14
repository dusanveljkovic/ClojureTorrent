(ns clojuretorrent.gui.state
  (:require [clojuretorrent.gui.style :refer [style]]
            [cljfx.api :as fx]
            [clojure.core.cache :as cache]))

(def *state 
  (atom 
    (fx/create-context
      {:torrents
     [{:id 1
       :name "test-torrent-1"
       :status :downloading 
       :progress 0.55
       :size 54535454353
       :down-speed 420000
       :up-speed 10000 
       :peers 42 
       :seeds 100 
       :eta 3111}
      {:id 2
       :name "test-torrent-2"
       :status :seeding 
       :progress 1.0 
       :size 111111113224
       :down-speed 0 
       :up-speed 8900000 
       :peers 7 
       :seed 0 
       :eta nil}
      {:id 3
       :name "test-torrent-3"
       :status :paused 
       :progress 0.12 
       :size 100000434234
       :down-speed 1600000
       :up-speed 1232333
       :peers 15 
       :seed 100 
       :eta 1233}
      ]
     :selected-id nil
     :filter :all
     :style style}
      cache/lru-cache-factory)))

