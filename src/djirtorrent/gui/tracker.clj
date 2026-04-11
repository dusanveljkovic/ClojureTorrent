(ns djirtorrent.gui.tracker
  (:require [djirtorrent.gui.common :refer [handle]]
            [djirtorrent.tracker :refer [send-request]]
            [djirtorrent.file :refer [announce-url info-hash length]]))

(defmethod handle :djirtorrent.gui.core/query-tracker [{:keys [state]}]
  (let [metadata (state :content)
        url (announce-url metadata)
        i-hash (info-hash metadata)
        peer-id (state :my-id)
        left (length metadata)]
   {:state (assoc state :tracker-response (send-request url i-hash peer-id left))}))

(defn peer-list [{:keys [peers]}]
  {:fx/type :table-view
   :column-resize-policy :constrained
   :columns [{:fx/type :table-column
              :text "IP"
              :cell-value-factory #(% :ip)
              :cell-factory {:fx/cell-type :table-cell
                             :describe (fn [x] {:text x})}}
             {:fx/type :table-column
              :text "Port"
              :cell-value-factory #(% :port)
              :cell-factory {:fx/cell-type :table-cell
                             :describe (fn [x] {:text (str x)})}}
             {:fx/type :table-column
              :text "Peer ID"
              :cell-value-factory #(apply str (seq (take 6 (% :peer-id))))
              :cell-factory {:fx/cell-type :table-cell
                             :describe (fn [x] {:text x})}}]
   :items peers
   :on-mouse-clicked {:event ::open-peer-window}})
