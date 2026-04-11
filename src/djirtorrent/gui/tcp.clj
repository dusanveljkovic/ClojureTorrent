(ns djirtorrent.gui.tcp
  (:require [cljfx.api :as fx]
            [manifold.bus :as bus]
            [manifold.stream :as s]
            [gloss.io :as gio]
            [djirtorrent.gui.common :refer [handle]]
            [djirtorrent.tcp :as tcp]
            )
  (:import [javafx.scene.input MouseButton]))

(def default-state 
  {:info-hash nil 
   :peer-id nil 
   :item nil 
   :messages ["NO MESSAGES"]
   :client nil
   :inspected nil}
  )

(def test-state 
  {:info-hash  (byte-array [63, -102, -84, 21, -116, 125, -24, -33, -54, -79, 113, -22, 88, -95, 122, -85, -33, 127, -68, -109])
   :peer-id (.getBytes "-qB50309999999999999") 
   :item {:ip "0.0.0.0" :port 19325 :peer-id "testID"} 
   :messages ["NO MESSAGES"]
   :client nil
   :inspected nil})

(def *tcp-state
  (atom default-state))

(reset! *tcp-state test-state)

@*tcp-state

(defn publish-to-topic [message-bus topic]
  (fn [message]
    (bus/publish! message-bus topic message)
    message))

(defn subscribe  [message-bus topic]
  (s/consume (fn [msg] (swap! *tcp-state update :messages conj msg)) (bus/subscribe message-bus topic)))


(defn str-repr-bittorrent [msg]
  (let [message-id (:message-id msg)
        payload (if (:payload msg) (str (into [] (.array (gio/contiguous (:payload msg))))) "")]
    (condp = message-id
      0 {:message-id message-id
         :message-id-str "CHOKE"
         :message ""}
      1 {:message-id message-id
         :message-id-str "UNCHOKE"
         :message ""}
      2 {:message-id message-id
         :message-id-str "INTERESTED"
         :message ""}
      3 {:message-id message-id
         :message-id-str "UNINTERESTED"
         :message ""}
      4 {:message-id message-id
         :message-id-str "HAVE"
         :message (str "PIECE INDEX [" payload "]")}
      5 {:message-id message-id
         :message-id-str "BITFIELD"
         :message payload}
      6 {:message-id message-id
         :message-id-str "REQUEST"
         :message payload}
      7 {:message-id message-id
         :message-id-str "PIECE"
         :message payload}
      8 {:message-id message-id
         :message-id-str "CANCEL"
         :message payload}
      9 {:message-id message-id
         :message-id-str "PORT"
         :message (str "NEW PORT [" payload "]")}
      )))

(defn str-repr [msg]
  (cond 
    (:pstr msg) {:message-id "X"
                 :message-id-str "HANDSHAKE"
                 :message (str "PEER ID [" (:peer-id msg) "]\nINFO HASH [" (:info-hash msg) "]" )}
    (:keep-alive msg) {:message-id "X"
                       :message-id-str "KEEP ALIVE"
                       :message ""}
    (:message-id msg) (str-repr-bittorrent msg)))

(defn inspector [{:keys [message]}]
  (let [{:keys [message-id message-id-str message]} (str-repr message)]
   {:fx/type :v-box
     :children [{:fx/type :label
                 :text (str "MESSAGE ID [" message-id "][" message-id-str "]")}
                {:fx/type :label
                 :text message}]}))

(def message-buttons
  {:fx/type :v-box
   :children [{:fx/type :button
               :text "CHOKE"
               :max-width Double/MAX_VALUE
               :on-action {::event ::send-choke}}
              {:fx/type :button
               :max-width Double/MAX_VALUE
               :text "UNCHOKE"
               :on-action {::event ::send-unchoke}}
              {:fx/type :button
               :max-width Double/MAX_VALUE
               :text "INTERESTED"
               :on-action {::event ::send-interested}}
              {:fx/type :button
               :text "NOT INTERESTED"
               :on-action {::event ::send-not-interested}}
              ]})

(defmulti tcp-handle ::event)

(defmethod tcp-handle ::send-choke [{:keys [state]}]
  (tcp/send-choke (:client state)))
(defmethod tcp-handle ::send-unchoke [{:keys [state]}]
  (tcp/send-unchoke (:client state)))
(defmethod tcp-handle ::send-interested [{:keys [state]}]
  (tcp/send-interested (:client state)))
(defmethod tcp-handle ::send-not-interested [{:keys [state]}]
  (tcp/send-not-interested (:client state)))

(defmethod tcp-handle ::open-connection [{:keys [state]}]
  (let [message-bus (bus/event-bus)
        item (state :item)
        ip (item :ip)
        port (item :port)
        info-hash (state :info-hash)
        peer-id (state :peer-id)
        client (tcp/connect-to-peer ip port info-hash peer-id {:custom-transformers [(publish-to-topic message-bus ip)]})] 
    (subscribe message-bus ip)
    {:state (-> state
                (assoc :messages [])
                (assoc :client client) 
                (assoc :message-bus message-bus))}))

(defmethod tcp-handle ::close-connection [{:keys [state]}]
  (let [client (state :client)]
    (tcp/close-peer-connection @client)
    {:state (-> state
                (assoc :messages [])
                (assoc :client nil))}))

(defmethod tcp-handle ::inspect-message [{:keys [fx/event state]}]
  (when (and (= (.getButton event) MouseButton/PRIMARY)
             (= (.getClickCount event) 2))
    (let [selected-message (-> event
                               .getSource
                               .getSelectionModel
                               .getSelectedItem)]
    {:state (assoc state :inspected selected-message)})))

(defn create-peer-window [{:keys [item messages client inspected]}]
  {:fx/type :stage
   :showing true
   :width 300
   :height 300
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :spacing 10
                  :padding 30 
                  :children [{:fx/type :label
                              :text "INFO"}
                             {:fx/type :h-box
                              :spacing 20
                              :children [{:fx/type :label 
                                          :text (str "IP: " (:ip item))}
                                         {:fx/type :label 
                                          :text (str "Port: " (:port item))}
                                         {:fx/type :label 
                                          :text (str "Peer ID: " (:peer-id item))}]}
                             {:fx/type :button
                              :text (if (nil? client) "Open connection" "Close connection")
                              :on-action {::event (if (nil? client) ::open-connection ::close-connection)}
                              :style {:-fx-text-fill (if (nil? client) "green" "red")}
                              :disable false}
                             {:fx/type :label
                              :text "MESSAGES"}
                             {:fx/type :h-box
                              :spacing 10
                              :children [{:fx/type :list-view
                                          :h-box/hgrow :always
                                         :items messages
                                         :on-mouse-clicked {::event ::inspect-message}}
                                         message-buttons]}
                             (if (nil? inspected)
                               {:fx/type :region}
                               {:fx/type inspector
                                :message inspected})
                             ]}}})
(def renderer
  (fx/create-renderer 
    :middleware (fx/wrap-map-desc #(create-peer-window %))
    :opts {:fx.opt/map-event-handler
           (-> tcp-handle
               (fx/wrap-co-effects {:state (fx/make-deref-co-effect *tcp-state)})
               (fx/wrap-effects {:state (fx/make-reset-effect *tcp-state)
                                 :dispatch fx/dispatch-effect}))}))

(fx/mount-renderer *tcp-state renderer)

(renderer)

(defmethod handle :djirtorrent.gui.tracker/open-peer-window [{:keys [fx/event state]}]
  (when (and (= (.getButton event) MouseButton/PRIMARY)
             (= (.getClickCount event) 2))
    (let [selected-item (-> event
                            .getSource
                            .getSelectionModel
                            .getSelectedItem)]
      (swap! *tcp-state assoc :item selected-item)
      (fx/on-fx-thread
        (fx/mount-renderer *tcp-state renderer)))))

