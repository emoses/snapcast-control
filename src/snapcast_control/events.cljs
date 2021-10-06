(ns snapcast-control.events
  (:require
   [re-frame.core :as r]
   [snapcast-control.db :as db]
   [snapcast-control.fx :as fx]
   [snapcast-control.snapcast-client :as sc]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   )
  (:use
   [clojure.pprint :only [pprint]]))

(def SOCKET-ID :default)

(defn extract-clients [groups]
  (mapcat :clients groups))

(defn groups-client-ids-only [groups]
  (map (fn [g]
         (update g :clients #(mapv :id %)))
       groups))

(defn id-map [seq-with-ids]
  (reduce (fn [m c] (assoc m (:id c) c)) {} seq-with-ids))

(defn handle-notification [msg]
  (r/dispatch [::notification msg]))

(r/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(r/reg-event-db
 ::update-connection-url
 [(r/path :connection :url)]
 (fn [_ [_ new]]
   new))

(r/reg-event-db
 ::set-show-disconnected
 [(r/path :view :show-disconnected)]
 (fn [_ [_ new]]
   new))

(r/reg-event-fx
 ::connect
 (fn [{:keys [db]} _]
   (let [dest (str (get-in db [:connection :url]) "/jsonrpc")
         client (sc/make-client dest SOCKET-ID handle-notification)]
     {:db (-> db
              (assoc-in [:connection :status] :connecting)
              (assoc-in [:connection :client] client))
      ::fx/connect [client {:on-open (fn [] (r/dispatch [::connected]))
                              :on-close (fn [] (r/dispatch [::disconnected]))
                              :on-error #(prn (str "Error: " %))}]})))

(r/reg-event-fx
 ::disconnect
 [(r/inject-cofx :client)]
 (fn [{:keys [db client]} _]
   {:db (assoc-in db [:connection :status] :disconnecting)
    ::fx/disconnect [client]}))

(r/reg-event-fx
 ::connected
 [(r/path :connection :status)]
 (fn [{:keys [db]} _]
   {:db :connected
    :dispatch [::refresh]}))

(r/reg-event-db
 ::disconnected
 [ (r/path :connection)]
 (fn [b _]
   (-> b
       (assoc :status :disconnected)
       (assoc :client nil))))

(def notification-dispatch
  {
   "Server.OnUpdate" ::update-server-status
   "Client.OnConnect" ::client-connect
   "Client.OnDisconnect" ::client-disconnect
   "Client.OnVolumeChanged" ::client-volume-changed
   "Client.OnLatencyChanged" ::client-latency-changed
   "Client.OnNameChanged" ::client-name-changed
   "Group.OnMute" ::group-mute
   "Group.OnStreamChanged" ::group-stream-changed
   "Group.OnNameChanged" ::group-name-changed
   "Stream.OnUpdate" ::stream-update
   "Stream.OnMetadata" ::stream-metadata})

(r/reg-event-fx
 ::notification
 (fn [_ [_ msg]]
   (if-let [evt (notification-dispatch (:method msg))]
     {:dispatch [evt (:params msg)]}
     (do
       (prn "Received unhandled notification" (:method msg))
       {}))))

(r/reg-event-fx
 ::refresh
 [(r/inject-cofx :client)]
 (fn [{:keys [client]} _]
   {::fx/send [client "Server.GetStatus" {} [::update-server-status]]}))

(r/reg-event-db
 ::update-server-status
 (fn [db [_ msg]]
   (let [groups (get-in msg [:server :groups])]
     (-> db
         (assoc :groups (-> groups groups-client-ids-only id-map))
         (assoc :clients (-> groups extract-clients id-map ))
         (assoc :streams (id-map (get-in msg [:server :streams])))))))

;;Notification handlers
(r/reg-event-db
 ::client-connect
 (fn [db [_ msg]]
   (assoc-in
    db
    [:clients (:id msg)]
    (:client msg))))

(r/reg-event-db
 ::client-disconnect
 (fn [db [_ msg]]
   (assoc-in
    db
    [:clients (:id msg)]
    (:client msg))))

(r/reg-event-db
 ::client-volume-changed
 (fn [db [_ msg]]
   (assoc-in
    db
    [:clients (:id msg) :config :volume]
    (:volume msg))))

(r/reg-event-db
 ::client-latency-changed
 (fn [db [_ msg]]
   (assoc-in
    db
    [:clients (:id msg) :config :latency]
    (:latency msg))))

(r/reg-event-db
 ::client-name-changed
 (fn [db [_ msg]]
   (assoc-in
    db
    [:clients (:id msg) :config :name]
    (:name msg))))

(r/reg-event-db
 ::group-mute
 (fn [db [_ msg]]
   (assoc-in
    db
    [:groups (:id msg) :muted]
    (:mute msg))))

(r/reg-event-db
 ::group-stream-changed
 (fn [db [_ msg]]
   (assoc-in
    db
    [:groups (:id msg) :stream_id]
    (:stream_id msg))))

(r/reg-event-db
 ::group-name-changed
 (fn [db [_ msg]]
   (assoc-in
    db
    [:groups (:id msg) :name]
    (:name msg))))

(r/reg-event-db
 ::stream-update
 (fn [db [_ msg]]
   (assoc-in
    db
    [:streams (:id msg)]
    (:stream msg))))

(r/reg-event-db
 ::stream-metadata
 (fn [db [_ msg]]
   (assoc-in
    db
    [:streams (:id msg) :meta]
    (:meta msg))))

;; commands
(r/reg-event-fx
 ::set-client-name
 [(r/inject-cofx :client)]
 (fn [{:keys [client]} [_ id name]]
   {::fx/send [client "Client.SetName" {:id id :name name} [::set-client-name-success id]]}))

(r/reg-event-db
 ::set-client-name-success
 (fn [db [_ id {:keys [name]}]]
   (assoc-in
    db
    [:clients id :config :name]
    name)))
