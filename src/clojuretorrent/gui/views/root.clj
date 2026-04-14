(ns clojuretorrent.gui.views.root
  (:require [clojuretorrent.gui.subs.core :as subs]
            [clojuretorrent.gui.views.topbar :refer [full-topbar]]
            [clojuretorrent.gui.views.torrent-list :refer [torrent-list]]
            [cljfx.api :as fx]
            [cljfx.css :as css]))

(defn root [{:keys [fx/context]}]
  (let [style (fx/sub-ctx context subs/sub-style)]
    {:fx/type :stage
     :showing true
     :title "ClojureTorrent"
     :width 1920
     :height 1080
     :scene
     {:fx/type :scene
      :stylesheets [(::css/url style)]
      :root 
      {:fx/type :v-box
       :style-class "root"
       :spacing 2
       :children
       [{:fx/type full-topbar}
        {:fx/type torrent-list}]
       }}}))

