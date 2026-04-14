(ns clojuretorrent.gui.views.torrent-list
  (:require [cljfx.api :as fx]
            [clojuretorrent.gui.subs.torrents :as subs.torrents]
            [clojure.string :as str]
            [clojuretorrent.gui.subs.core :as subs]))

(defn status-label [status]
  (case status
    :downloading "Downloading"
    :seeding "Seeding"
    :paused "Paused"
    :checking "Checking"
    :error "Error"
    "Unknown"))

(defn progress-bar [{:keys [progress status width]}]
  (let [bar-class (case status 
                    :downloading "progress-bar-dl"
                    :seeding "progress-bar-seed"
                    "progress-bar-paused")]
    {:fx/type :stack-pane
     :pref-width width
     :children
     [{:fx/type :pane
       :style-class "progress-track"
       :pref-width width
       :pref-height 3}
      {:fx/type :pane
       :style-class bar-class
       :pref-width (* width progress)
       :pref-height 3
       :stack-pane/alignment :center-left}]}))

(defn status-chip [{:keys [status]}]
  {:fx/type :label
   :text (str/upper-case (status-label status))
   :style-class (str "status-" (name status))})

(defn torrent-row [{:keys [torrent selected?]}]
  (let [{:keys [id name status progress size down-speed up-speed peers seeds eta]} torrent]
    {:fx/type :v-box
     :style-class (if selected? ["torrent-row" "torrent-row-selected"] ["torrent-row"])
     :on-mouse-clicked {:event-type ::selected-torrent :id id}
     :children 
     [{:fx/type :h-box
       :spacing 10 
       :alignment :center-left 
       :children
       [{:fx/type :v-box
         :h-box/hgrow :always 
         :spacing 4 
         :children 
         [{:fx/type :h-box 
           :spacing 8 
           :alignment :center-left 
           :children 
           [{:fx/type status-chip 
             :status status}
            {:fx/type :label 
             :text name 
             :style-class "torrent-name"
             :max-width Double/MAX_VALUE
             :h-box/hgrow :always}]}
          ; (progress-bar {:progress progress :status status :width 500})
          {:fx/type :h-box 
           :spacing 16 
           :children 
           [{:fx/type :label 
             :style-class "torrent-meta"
             :text (format "%.1f%%" (* progress 100))}]}]}]}]}))


(defn table-header [_]
  (let [header-names ["STATUS / NAME" "PROGRESS" "SIZE" "DOWN" "UP" "PEERS" "ETA"]
        label-f (fn [header-name]
                  {:fx/type :label
                   :text header-name
                   :style-class "table-header-label"
                   :pref-width (case header-name 
                                 "STATUS / NAME" 300
                                 "PROGRESS" 80
                                 60)
                   :h-box/hgrow (if (= header-name "STATUS / NAME") :always :never)})]
    {:fx/type :h-box
     :style-class "table-header"
     :spacing 0 
     :children 
     (map label-f header-names)}))

(defn torrent-list [{:keys [fx/context]}]
  (let [curr-filter (fx/sub-ctx context subs/sub-current-filter)
        visible (fx/sub-ctx context subs.torrents/sub-state curr-filter)
        selected-id (fx/sub-ctx context subs/sub-selected-id)]
    {:fx/type :scroll-pane
     :fit-to-width true 
     ; :v-box/vgrow :always
     :style     "-fx-background-color: #1a1a2e; -fx-border-color: transparent;"
     :content 
     {:fx/type :v-box 
      :children
      (mapv (fn [t] 
              {:fx/type torrent-row 
               :torrent t 
               :visible? (= (:id t) selected-id)})
            visible)}}))
