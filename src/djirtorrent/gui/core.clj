(ns djirtorrent.gui.core
  (:require [cljfx.api :as fx]
            [manifold.bus :as bus]
            [manifold.stream :as s]
            [djirtorrent.tracker :refer [peers]]
            [djirtorrent.gui.common :refer [handle]]
            [djirtorrent.gui.file-info]
            [djirtorrent.gui.tracker]
            [djirtorrent.gui.tcp]))

(System/setProperty "cljfx.debug" "true")
(def my-bus (bus/event-bus))

(def default-state
  {:file nil :content nil :tracker-response nil :my-id "-DJ10009999999999999" :bus-messages []})

(defonce *state
  (atom default-state))

(reset! *state default-state)

@*state

(def sub2 (s/consume #(swap! *state update :bus-messages conj %) (bus/subscribe my-bus "message2")))

(s/close! (bus/subscribe my-bus "message"))

(bus/publish! my-bus "message2" "hey2")


(defn root-view [{:keys [file content tracker-response bus-message]}]
  {:fx/type :stage 
   :title "Stage djir" 
   :showing true 
   :width 800
   :height 600
   :scene {:fx/type :scene 
           :root {:fx/type :v-box 
                  :padding 30
                  :spacing 15 
                  :children [{:fx/type :h-box 
                              :spacing 15 
                              :alignment :center-left
                              :children [{:fx/type :button
                                          :text "Open .torrent file..."
                                          :on-action {:event ::open-file}}
                                         {:fx/type :label
                                          :text (str file)}]}
                             {:fx/type :button
                              :text "Get torrent metadata"
                              :on-action {:event ::parse}}
                             {:fx/type :label
                              :text (str "Bus message: " bus-message)}
                             {:fx/type :label
                              :text "Metadata: "
                              :font {:family "Arial"
                                     :size 30
                                     :weight :bold}}
                             (if (nil? content)
                               {:fx/type :region} 
                               {:fx/type djirtorrent.gui.file-info/file-info 
                                :content content})
                             {:fx/type :button
                              :disable (nil? content)
                              :text "Query tracker"
                              :on-action {:event ::query-tracker}}
                             (if (nil? tracker-response)
                               {:fx/type :region}
                               {:fx/type djirtorrent.gui.tracker/peer-list
                                :peers (peers tracker-response)})]}}})

(def renderer
  (fx/create-renderer 
    :middleware (fx/wrap-map-desc #(root-view %))
    :opts {:fx.opt/map-event-handler
           (-> handle
               (fx/wrap-co-effects {:state (fx/make-deref-co-effect *state)})
               (fx/wrap-effects {:state (fx/make-reset-effect *state)
                                 :dispatch fx/dispatch-effect}))}))

(fx/mount-renderer *state renderer)

(renderer)

