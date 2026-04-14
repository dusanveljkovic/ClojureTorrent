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




(comment 
  (renderer)
  (fx/unmount-renderer *state renderer)
  (add-watch #'style :refresh-app (fn [_ _ _ _] (swap! *state assoc :style style)))
  (remove-watch #'style :refresh-app)
  )

(defn -main [& args]
  (fx/mount-renderer *state (renderer)))
