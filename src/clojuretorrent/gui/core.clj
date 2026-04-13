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

(def *state 
  (atom 
    {:torrents
      [{:id 1
        :name "test-torrent-1"}
       {:id 2
        :name "test-torrent-2"}
       {:id 3
        :name "test-torrent-3"}
       ]
     :selected-id nil
     :filter :all}) 
  )

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
    (let [font-family "'JetBrains Mono', 'Consolas', monospace"] 
      {".root" 
       {:fx-background-color "#1a1a2e"
        :fx-font-family font-family}

       ".toolbar"
       {:-fx-background-color "#16213e"
        :-fx-padding           "12 16 12 16"
        :-fx-border-color      "#0f3460"
        :-fx-border-width      "0 0 1 0"}

       ".toolbar-btn"
       {:-fx-background-color "#0f3460"
        :-fx-text-fill        "#e2e8f0"
        :-fx-font-size        "12px"
        :-fx-font-family      font-family
        :-fx-padding          "6 14 6 14"
        :-fx-background-radius "3"
        :-fx-cursor           "hand"}

       ".toolbar-btn:hover"
       {:-fx-background-color "#1a4a8a"}

       ".toolbar-btn-danger"
       {:-fx-background-color "#7f1d1d"
        :-fx-text-fill        "#fca5a5"}

       ".toolbar-btn-danger:hover"
       {:-fx-background-color "#991b1b"}

       ".filter-bar"
       {:-fx-background-color "#16213e"
        :-fx-padding          "8 16 8 16"
        :-fx-border-color     "#0f3460"
        :-fx-border-width     "0 0 1 0"}
   
       ".filter-btn"
       {:-fx-background-color "transparent"
        :-fx-text-fill        "#64748b"
        :-fx-font-size        "11px"
        :-fx-font-family      font-family
        :-fx-padding          "4 12 4 12"
        :-fx-background-radius "2"
        :-fx-cursor           "hand"
        :-fx-border-color     "#0f3460"
        :-fx-border-width     "1"
        :-fx-border-radius    "2"}
   
       ".filter-btn-active"
       {:-fx-background-color "#0f3460"
        :-fx-text-fill        "#93c5fd"}
     
       ".table-header"
       {:-fx-background-color "#0d1b33"
        :-fx-padding          "8 16 8 16"}
   
       ".table-header-label"
       {:-fx-text-fill  "#475569"
        :-fx-font-size  "10px"
        :-fx-font-family font-family}
     })))

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
       :pref-width (* width (min progress 1.0))
       :stack-pane/alignment :center-left}]}))

(defn status-chip [{:keys [status]}]
  {:fx/type :label
   :text (str/upper-case (status-label status))
   :style-class (str "status-" (name status))})



(defn toolbar [_]
  {:fx/type :h-box
   :style-class "toolbar"
   :spacing 8
   :alignment :center-left
   :children 
   [{:fx/type :button 
     :text "+ Add Torrent"
     :style-class "toolbar-btn"
     :on-action {:event/type ::add-torrent}}
    {:fx/type :button 
     :text "▶ Resume"
     :style-class "toolbar-btn"
     :on-action {:event/type ::resume-torrent}}
    {:fx/type :button 
     :text "⏸ Pause"
     :style-class "toolbar-btn"
     :on-action {:event/type ::pause-torrent}}
    {:fx/type :pane
     :h-box/hgrow :always}
    {:fx/type :button
     :text "✕ Remove"
     :style-class ["toolbar-btn" "toolbar-btn-danger"]
     :on-action {:event/type ::remove-torrent}}
    ]})

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
                                   ["filter-btn"])
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

(defn root [{:keys [torrents selected-id curr-filter]}]
  (let [counts {:total (count torrents)
                :downloading (count (filter #(= :downloading (:status %)) torrents))
                :seeding (count (filter #(= :seeding (:status %)) torrents))
                :paused (count (filter #(= :paused (:status %)) torrents))}]
    {:fx/type :stage
     :showing true
     :title "ClojureTorrent"
     :width 1000
     :height 600
     :scene
     {:fx/type :scene
      :stylesheets [(::css/url style)]
      :root 
      {:fx/type :v-box
       :style-class "root"
       :children 
       [(toolbar {})
        (filter-bar (merge counts {:current-filter curr-filter}))]}}}))

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
  (app)
  (renderer))

(defn -main [& args]
  (fx/mount-renderer *state (renderer)))
