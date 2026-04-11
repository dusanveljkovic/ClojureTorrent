(ns clojuretorrent.gui.common)

(defmulti handle :event)

(defmethod handle :default [event]
  (println "Unknown event: " event))


