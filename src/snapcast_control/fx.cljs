(ns snapcast-control.fx
  (:require
   [re-frame.core :as re-frame]
   [snapcast-control.snapcast-client :as sc]))

(re-frame/reg-fx
 ::connect
 (fn [[client {:keys [on-open on-close on-error]}]]
   (sc/connect client on-open on-close on-error)))

(re-frame/reg-fx
 ::disconnect
 (fn [[client]]
   (sc/disconnect client)))

(defn evt->callback [event]
  (fn [msg]
    (prn "response " msg)
    (re-frame/dispatch (conj event (:result msg)))))

(re-frame/reg-fx
 ::send
 (fn [[client method params evt]]
   (sc/send-msg client method params (evt->callback evt))) )

;;Convenience to pull the client out of the db
(re-frame/reg-cofx
 :client
 (fn [{:keys [db] :as cofx} _]
   (assoc cofx :client (get-in db [:connection :client]))))
