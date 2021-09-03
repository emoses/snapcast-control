(ns snapcast-control.events
  (:require
   [re-frame.core :as re-frame]
   [snapcast-control.db :as db]
   [snapcast-control.fx :as fx]
   [day8.re-frame.tracing :refer-macros [fn-traced]]
   ))

(re-frame/reg-event-db
 ::initialize-db
 (fn-traced [_ _]
   db/default-db))

(re-frame/reg-event-db
 ::update-connection-url
 [(re-frame/path :connection :url)]
 (fn [_ [_ new]]
   new))

(re-frame/reg-event-fx
 ::connect
 (fn [{:keys [db]} _]
   (let [dest (str (get-in db [:connection :url]) "/jsonrpc")]
     {:db (assoc-in db [:connection :status] :connecting)
      ::fx/ws-connect [:main {:url dest
                              :on-message #(js/console.log (str "MSG: " %))
                              :on-open (fn [] (prn "opening")
                                         (re-frame/dispatch [::connected]))
                              :on-close (fn [] ( prn "closing")
                                          (re-frame/dispatch [::disconnected]))
                              :on-error #(prn (str "Error: " %))}]})))

(re-frame/reg-event-fx
 ::disconnect
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:connection :status] :disconnecting)
    ::fx/ws-disconnect [:main]
    }))

(re-frame/reg-event-db
 ::connected
 [ (re-frame/path :connection :status)]
 (fn [_ _]
   :connected))

(re-frame/reg-event-db
 ::disconnected
 [ (re-frame/path :connection :status)]
 (fn [_ _]
   :disconnected))
