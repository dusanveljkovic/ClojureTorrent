(ns clojuretorrent.gui.newcore
  (:require [cljfx.api :as fx]
            [clojuretorrent.gui.events :refer [handle-event]]
            [clojuretorrent.gui.style :refer [style]]
            [clojuretorrent.gui.state :refer [*state]]
            [clojuretorrent.gui.views.root :refer [root]]))

(def renderer 
  (fx/create-renderer 
    :middleware (comp 
                  fx/wrap-context-desc 
                  (fx/wrap-map-desc (fn [_] {:fx/type root})))
    :opts {:fx.opt/map-event-handler handle-event
           :fx.opt/type->lifecycle #(or (fx/keyword->lifecycle %)
                                        (fx/fn->lifecycle-with-context %))}))

(defn -main []
  (fx/mount-renderer *state renderer))

(comment
  (add-watch #'style :refresh-app (fn [_ _ _ _] (swap! *state fx/swap-context assoc :style style)))
  (remove-watch #'style :refresh-app)
  (renderer))
