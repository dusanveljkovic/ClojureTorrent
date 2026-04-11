(ns clojuretorrent.gui.file-info
  (:require [clojuretorrent.gui.common :refer [handle]]
            [clojuretorrent.file :refer [read-file info-hash]]
            )
  (:import [javafx.stage FileChooser]
           [javafx.event ActionEvent]
           [javafx.scene Node]
           [javafx.scene.input MouseButton]))


(defmethod handle :djirtorrent.gui.core/open-file [{:keys [^ActionEvent fx/event state]}]
  (let [window (.getWindow (.getScene ^Node (.getTarget event)))
        chooser (doto (FileChooser.)
                  (.setTitle "Open .torrent file"))]
    (when-let [file (.showOpenDialog chooser window)]
      {:state (assoc state :file file)})))

(defmethod handle :djirtorrent.gui.core/parse [{:keys [state]}]
  {:state (assoc state :content (read-file (state :file)))})


(defn- grid-info [text-val]
  (flatten (map-indexed
    (fn [index [text val]] 
      [{:fx/type :label
        :text text
        :grid-pane/row index
        :grid-pane/column 0}
       {:fx/type :text
        :text val
        :grid-pane/row index
        :grid-pane/column 1}])
   text-val)))

(defn file-info [{:keys [content]}]
  (let [announce (get content "announce" "")
        comment (get content "comment" "")
        created-by (get content "created by" "")
        creation-date (get content "creation date" "")
        info (get content "info")
        name (get info "name" "")
        length (get info "length" 0)
        i-hash (:formatted (info-hash content))]
   {:fx/type :h-box
   :children [ {:fx/type :grid-pane
                 :vgap 10
                 :hgap 10
                 :padding 20
                 :children (into [] (grid-info [["Announce URL: " announce]
                                       ["Comment: " comment]
                                       ["Created by: " created-by]
                                       ["Creation date: "]]))}
              {:fx/type :grid-pane
               :vgap 10
               :hgap 10
               :padding 20
               :children (grid-info [["Filename: " name]
                                     ["Length: " (str (quot length 1048576) " MB")]
                                     ["Info hash: " i-hash]])}
              ]}))
