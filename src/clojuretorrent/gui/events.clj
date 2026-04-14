(ns clojuretorrent.gui.events
  (:require [clojuretorrent.gui.state :refer [*state]]
            [cljfx.api :as fx]))

(defmulti handle-event :event/type)

(defmethod handle-event :set-filter [{:keys [filter]}]
  (swap! *state fx/swap-context assoc :filter filter))

(defmethod handle-event :default [event]
  (println "Unhandled evene: " event))
