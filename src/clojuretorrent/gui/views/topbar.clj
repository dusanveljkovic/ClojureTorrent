(ns clojuretorrent.gui.views.topbar
  (:require [clojuretorrent.gui.subs.core :as subs]
            [clojuretorrent.gui.subs.torrents :as subs.torrents]
            [cljfx.api :as fx]))

(defn topbar [_]
  {:fx/type :h-box
   :style-class "topbar"
   :spacing 8
   :alignment :center-left
   :children 
   [{:fx/type :button 
     :text "File"
     :style-class "topbar-button"
     :on-action {:event/type ::open-file-submenu}}
    {:fx/type :button 
     :text "Edit"
     :style-class "topbar-button"
     :on-action {:event/type ::open-edit-submenu}}
    {:fx/type :button 
     :text "View"
     :style-class "topbar-button"
     :on-action {:event/type ::open-view-submenu}}
    {:fx/type :button
     :text "Tools"
     :style-class "topbar-button"
     :on-action {:event/type ::open-tools-submenu}}
    ]})

(defn secondbar [_]
  {:fx/type :h-box
   :style-class "secondbar"
   :spacing 12
   :alignment :center-left
   :children 
   [{:fx/type :button 
     :text "+ Add Torrent Link"
     :style-class ["secondbar-btn" "secondbar-btn-base"]
     :on-action {:event/type ::add-torrent-link}}
    {:fx/type :button 
     :text "+ Add Torrent File"
     :style-class ["secondbar-btn" "secondbar-btn-base"]
     :on-action {:event/type ::add-torrent-file}}
    {:fx/type :button
     :text "✕ Remove"
     :style-class ["secondbar-btn" "secondbar-btn-red"]
     :on-action {:event/type ::remove-torrent}}
    {:fx/type :button 
     :text "▶ Resume"
     :style-class ["secondbar-btn" "secondbar-btn-green"]
     :on-action {:event/type ::resume-torrent}}
    {:fx/type :button 
     :text "⏸ Pause"
     :style-class ["secondbar-btn" "secondbar-btn-yellow"]
     :on-action {:event/type ::pause-torrent}}]})

(defn filter-bar [{:keys [fx/context]}]
  (let [current-filter (fx/sub-ctx context subs/sub-current-filter)
        total (fx/sub-ctx context subs.torrents/sub-total)
        downloading (fx/sub-ctx context subs.torrents/sub-state-count :downloading)
        seeding (fx/sub-ctx context subs.torrents/sub-state-count :seeding)
        paused (fx/sub-ctx context subs.torrents/sub-state-count :downloading)
        filters [[:all (str "All (" total ")") total ]
                 [:downloading (str "Downloading (" downloading ")") downloading ]
                 [:seeding (str "Seeding (" seeding ")") seeding]
                 [:paused (str "Paused (" paused ")") paused]]
        button-f (fn [[key label count]]
                   {:fx/type :button
                    :text label
                    :style-class (if (= current-filter key)
                                   ["filter-btn" "filter-btn-active"]
                                   ["filter-btn" "filter-btn-base"])
                    :on-action {:event/type :set-filter :filter key}})]
    {:fx/type :h-box 
     :style-class "filter-bar"
     :spacing 6
     :alignment :center-left
     :children 
     (map button-f filters)}))

(defn full-topbar [_]
  {:fx/type :v-box
   :spacing 4
   :children 
   [{:fx/type topbar}
    {:fx/type secondbar}
    {:fx/type filter-bar}]})
