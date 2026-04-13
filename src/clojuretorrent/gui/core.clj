; (ns clojuretorrent.gui.core
;   (:require [cljfx.api :as fx]
;             [manifold.bus :as bus]
;             [manifold.stream :as s]
;             [clojuretorrent.tracker :refer [peers]]
;             [clojuretorrent.gui.common :refer [handle]]
;             [clojuretorrent.gui.file-info]
;             [clojuretorrent.gui.tracker]
;             [clojuretorrent.gui.tcp]))
;
; (System/setProperty "cljfx.debug" "true")
; (def my-bus (bus/event-bus))
;
; (def default-state
;   {:file nil :content nil :tracker-response nil :my-id "-DJ10009999999999999" :bus-messages []})
;
; (defonce *state
;   (atom default-state))
;
; (reset! *state default-state)
;
; @*state
;
; (def sub2 (s/consume #(swap! *state update :bus-messages conj %) (bus/subscribe my-bus "message2")))
;
; (s/close! (bus/subscribe my-bus "message"))
;
; (bus/publish! my-bus "message2" "hey2")
;
;
; (defn root-view [{:keys [file content tracker-response bus-message]}]
;   {:fx/type :stage 
;    :title "Stage djir" 
;    :showing true 
;    :width 800
;    :height 600
;    :scene {:fx/type :scene 
;            :root {:fx/type :v-box 
;                   :padding 30
;                   :spacing 15 
;                   :children [{:fx/type :h-box 
;                               :spacing 15 
;                               :alignment :center-left
;                               :children [{:fx/type :button
;                                           :text "Open .torrent file..."
;                                           :on-action {:event ::open-file}}
;                                          {:fx/type :label
;                                           :text (str file)}]}
;                              {:fx/type :button
;                               :text "Get torrent metadata"
;                               :on-action {:event ::parse}}
;                              {:fx/type :label
;                               :text (str "Bus message: " bus-message)}
;                              {:fx/type :label
;                               :text "Metadata: "
;                               :font {:family "Arial"
;                                      :size 30
;                                      :weight :bold}}
;                              (if (nil? content)
;                                {:fx/type :region} 
;                                {:fx/type clojuretorrent.gui.file-info/file-info 
;                                 :content content})
;                              {:fx/type :button
;                               :disable (nil? content)
;                               :text "Query tracker"
;                               :on-action {:event ::query-tracker}}
;                              (if (nil? tracker-response)
;                                {:fx/type :region}
;                                {:fx/type clojuretorrent.gui.tracker/peer-list
;                                 :peers (peers tracker-response)})]}}})
;
; (def renderer
;   (fx/create-renderer 
;     :middleware (fx/wrap-map-desc #(root-view %))
;     :opts {:fx.opt/map-event-handler
;            (-> handle
;                (fx/wrap-co-effects {:state (fx/make-deref-co-effect *state)})
;                (fx/wrap-effects {:state (fx/make-reset-effect *state)
;                                  :dispatch fx/dispatch-effect}))}))
;
; (fx/mount-renderer *state renderer)
;
; (renderer)
;

(ns clojuretorrent.gui.core 
  (:require [cljfx.api :as fx]
            [cljfx.css :as css]
            [clojure.string :as str]
            [clojure.math :refer [pow]])
  (:import [javafx.scene.paint Color]
           [javafx.stage Stage]))


(defn format-bytes 
  [bytes]
  (let [gib-number (pow 2.0 30)
        mib-number (pow 2.0 20)
        kib-number (pow 2.0 10)]
   (cond 
    (>= bytes gib-number) (format "%.2f GiB" (/ bytes gib-number))
    (>= bytes mib-number) (format "%.1f MiB" (/ bytes mib-number))
    (>= bytes kib-number) (format "%.0f KiB" (/ bytes kib-number))
    :else (format "%d B" bytes))))

(comment 
  (format-bytes 10000000000000))

(defn status-label [status]
  (case status
    :downloading "Downloading"
    :seeding "Seeding"
    :paused "Paused"
    :checking "Checking"
    :error "Error"
    "Unknown"))

(def style
  (css/register ::style 
    (let [font-family "'JetBrains Mono', 'Consolas', monospace"
          base-btn-color "#9bccd4"
          red-btn-color "#c96f61"
          yellow-btn-color "#e3dd6b"
          green-btn-color "#9bd180"
          active-btn-color "#6bc7e3"
          btn-border-color "#8c989c"] 
      {".root" 
       {
        :-fx-font-family font-family
        :-fx-padding "2 8 2 8"}

       ".topbar"
       {}

       ".topbar-button"
       {:-fx-font-size "12px"}

       ".seconbar"
       {}

       ".secondbar-btn"
       {:-fx-font-size "14px"
        :-fx-font-weight 700
        :-fx-border-radius 4 
        :-fx-background-radius 4
        :-fx-padding "2 8 2 8"
        :-fx-border-color btn-border-color}

       ".secondbar-btn:hover" 
       {:-fx-background-color "#80a5ab"}

       ".secondbar-btn-base"
       {:-fx-background-color base-btn-color}

       ".secondbar-btn-red"
       {:-fx-background-color red-btn-color}

       ".secondbar-btn-green"
       {:-fx-background-color green-btn-color}

       ".secondbar-btn-yellow"
       {:-fx-background-color yellow-btn-color}

       ".filter-bar"
       {}
   
       ".filter-btn"
       {:-fx-font-size        "12px"
        :-fx-padding          "4 8 4 8"
        :-fx-border-color btn-border-color
        :-fx-background-radius 4
        :-fx-border-radius 4
        :-fx-cursor           "hand"
        :-fx-background-color base-btn-color}

       ".filter-btn-base"
       {:-fx-background-color base-btn-color}
   
       ".filter-btn-active"
       {:-fx-background-color active-btn-color
        :-fx-font-weight 600}
     
       ".table-header"
       {:-fx-background-color "#0d1b33"
        :-fx-padding          "8 16 8 16"}
   
       ".table-header-label"
       {:-fx-text-fill  "#475569"
        :-fx-font-size  "10px"
        :-fx-font-family font-family}

       ".torrent-row"
       {:-fx-background-color "#1a1a2e"
        :-fx-padding          "10 16 10 16"
        :-fx-border-color     "#1e293b"
        :-fx-border-width     "0 0 1 0"
        :-fx-cursor           "hand"}
 
       ".torrent-row:hover"
       {:-fx-background-color "#1e2a45"}
   
       ".torrent-row-selected"
       {:-fx-background-color "#0f2444"
        :-fx-border-color     "#1d4ed8"
        :-fx-border-width     "0 0 1 0 "}
   
       ".torrent-name"
       {:-fx-text-fill  "#e2e8f0"
        :-fx-font-size  "12px"
        :-fx-font-family font-family}
   
       ".torrent-meta"
       {:-fx-text-fill  "#64748b"
        :-fx-font-size  "10px"
        :-fx-font-family font-family}
   
       ".status-downloading"
       {:-fx-text-fill "#38bdf8"
        :-fx-font-size "10px"
        :-fx-font-family font-family}
   
       ".status-seeding"
       {:-fx-text-fill "#34d399"
        :-fx-font-size "10px"
        :-fx-font-family font-family}
   
       ".status-paused"
       {:-fx-text-fill "#94a3b8"
        :-fx-font-size "10px"
        :-fx-font-family font-family}
   
       ".status-error"
       {:-fx-text-fill "#f87171"
        :-fx-font-size "10px"
        :-fx-font-family font-family}
   
       ".progress-track"
       {:-fx-background-color "#0f2444"
        :-fx-background-radius "1"
        :-fx-pref-height      "3"}
   
       ".progress-bar-dl"
       {:-fx-background-color "#1d4ed8"
        :-fx-background-radius "1"}
   
       ".progress-bar-seed"
       {:-fx-background-color "#059669"
        :-fx-background-radius "1"}
   
       ".progress-bar-paused"
       {:-fx-background-color "#334155"
        :-fx-background-radius "1"}
   
       ".stat-value"
       {:-fx-text-fill  "#94a3b8"
        :-fx-font-size  "11px"
        :-fx-font-family font-family}
   
       ".statusbar"
       {:-fx-background-color "#0d1117"
        :-fx-padding          "5 16 5 16"
        :-fx-border-color     "#0f3460"
        :-fx-border-width     "1 0 0 0"}
   
       ".statusbar-label"
       {:-fx-text-fill  "#475569"
        :-fx-font-size  "10px"
        :-fx-font-family font-family}})))

(def *state 
  (atom 
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
     :style style}))

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
           [(status-chip {:status status})
            {:fx/type :label 
             :text name 
             :style-class "torrent-name"
             :max-width Double/MAX_VALUE
             :h-box/hgrow :always}]}
          (progress-bar {:progress progress :status status :width 500})
          {:fx/type :h-box 
           :spacing 16 
           :children 
           [{:fx/type :label 
             :style-class "torrent-meta"
             :text (format "%.1f%%" (* progress 100))}]}]}]}]}))

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

(defn filter-bar [{:keys [current-filter total downloading seeding paused]}]
  (let [filters [[:all (str "All (" total ")") total ]
                 [:downloading (str "Downloading (" downloading ")") downloading ]
                 [:seeding (str "Seeding (" seeding ")") seeding]
                 [:paused (str "Paused (" paused ")") paused]]
        button-f (fn [[key label count]]
                   {:fx/type :button
                    :text label
                    :style-class (if (= current-filter key)
                                   ["filter-btn" "filter-btn-active"]
                                   ["filter-btn" "filter-btn-base"])
                    :on-action {:event/type ::set-filter :filter key}})]
    {:fx/type :h-box 
     :style-class "filter-bar"
     :spacing 6
     :alignment :center-left
     :children 
     (map button-f filters)}))

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

(defn torrent-list [{:keys [torrents selected-id curr-filter]}]
  (let [visible (case curr-filter 
                  :downloading (filter #(= :downloading (:status %)) torrents)
                  :seeding (filter #(= :seeding (:status %)) torrents)
                  :paused (filter #(= :paused (:status %)) torrents)
                  torrents)]
    {:fx/type :scroll-pane
     :fit-to-width true 
     :v-box/vgrow :always
     :style     "-fx-background-color: #1a1a2e; -fx-border-color: transparent;"
     :content 
     {:fx/type :v-box 
      :children
      (mapv (fn [t] (torrent-row {:torrent t :selected? (= (:id t) selected-id)}))
            visible)}}))


(defn root [{:keys [torrents selected-id curr-filter style]}]
  (let [counts {:total (count torrents)
                :downloading (count (filter #(= :downloading (:status %)) torrents))
                :seeding (count (filter #(= :seeding (:status %)) torrents))
                :paused (count (filter #(= :paused (:status %)) torrents))}]
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
       [(topbar {})
        (secondbar {})
        (filter-bar (merge counts {:current-filter curr-filter}))
        (torrent-list {:torrents torrents 
                       :selected-id selected-id 
                       :filter curr-filter})]}}}))

(defmulti handle-event :event/type)

(defmethod handle-event :default [event]
  (println "Unhandled evene: " event))

(def renderer 
  (fx/create-renderer 
    :middleware (fx/wrap-map-desc #(root %))
    :opts {:fx.opt/map-event-handler handle-event}))

(defonce app 
  (fx/mount-renderer *state renderer))

(comment 
  (renderer)
  (fx/unmount-renderer *state renderer)
  (add-watch #'style :refresh-app (fn [_ _ _ _] (swap! *state assoc :style style)))
  (remove-watch #'style :refresh-app)
  )

(defn -main [& args]
  (fx/mount-renderer *state (renderer)))
