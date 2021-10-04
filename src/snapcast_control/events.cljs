(ns snapcast-control.events
  (:require
   [re-frame.core :as re-frame]
   [snapcast-control.db :as db]
   [snapcast-control.fx :as fx]
   [snapcast-control.snapcast-client :as sc]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   )
  (:use
   [clojure.pprint :only [pprint]]))

(def SOCKET-ID :default)

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::update-connection-url
 [(re-frame/path :connection :url)]
 (fn [_ [_ new]]
   new))

(defn handle-notification [msg]
  (re-frame/dispatch [::notification msg]))

(re-frame/reg-event-fx
 ::connect
 (fn [{:keys [db]} _]
   (let [dest (str (get-in db [:connection :url]) "/jsonrpc")
         client (sc/make-client dest SOCKET-ID handle-notification)]
     {:db (-> db
              (assoc-in [:connection :status] :connecting)
              (assoc-in [:connection :client] client))
      ::fx/connect [client {:on-open (fn [] (re-frame/dispatch [::connected]))
                              :on-close (fn [] (re-frame/dispatch [::disconnected]))
                              :on-error #(prn (str "Error: " %))}]})))

(re-frame/reg-event-fx
 ::disconnect
 [(re-frame/inject-cofx :client)]
 (fn [{:keys [db client]} _]
   {:db (assoc-in db [:connection :status] :disconnecting)
    ::fx/disconnect [client]}))

(re-frame/reg-event-fx
 ::connected
 [(re-frame/path :connection :status)]
 (fn [{:keys [db]} _]
   {:db :connected
    :dispatch [::refresh]}))

(re-frame/reg-event-db
 ::disconnected
 [ (re-frame/path :connection)]
 (fn [b _]
   (-> b
       (assoc :status :disconnected)
       (assoc :client nil))))

(re-frame/reg-event-fx
 ::notification
 (fn [_ [_ msg]]
   (case (:method msg)
     "Server.OnUpdate" {:dispatch [::update-server-status msg]}
     (do (prn "Recieved unhandled notification" (:method msg)) {}))))

(defn extract-clients [groups]
  (mapcat :clients groups))

(defn groups-client-ids-only [groups]
  (map (fn [g]
         (update g :clients #(mapv :id %)))
       groups))

(defn id-map [seq-with-ids]
  (reduce (fn [m c] (assoc m (:id c) c)) {} seq-with-ids))

(re-frame/reg-event-db
 ::update-server-status
 (fn [db [_ msg]]
   (let [groups (get-in msg [:server :groups])]
     (-> db
         (assoc :groups (-> groups groups-client-ids-only id-map))
         (assoc :clients (-> groups extract-clients id-map ))
         (assoc :streams (get-in msg [:server :streams]))))))

(re-frame/reg-event-fx
 ::refresh
 [(re-frame/inject-cofx :client)]
 (fn [{:keys [client]} _]
   {::fx/send [client "Server.GetStatus" {} [::update-server-status]]}))
