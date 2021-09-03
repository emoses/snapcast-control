(ns snapcast-control.fx
  (:require
   [re-frame.core :as re-frame]
   [wscljs.client :as ws]))

(defonce SOCKETS (atom {}))

(defn ws-cleanup
  ([socket-id ] (ws-cleanup socket-id (fn [&rest])))
  ([socket-id on-close]
   (fn [&rest]
     (when on-close (on-close))
     (swap! SOCKETS dissoc socket-id))))

(re-frame/reg-fx
 ::ws-connect
 (fn [[socket-id {:keys [url on-message on-error on-open on-close] :as m}]]
   (prn m)
   (let [socket (ws/create url
                    {:on-message on-message
                     :on-error on-error
                     :on-open on-open
                     :on-close (ws-cleanup socket-id on-close)})]
     (swap! SOCKETS assoc socket-id socket))))

(re-frame/reg-fx
 ::ws-disconnect
 (fn [[socket-id]]
   (let [socket (socket-id @SOCKETS)]
     (ws/close socket))))

(re-frame/reg-fx
 ::ws-send
 (fn [[socket-id msg]]
   (let [socket ( socket-id @SOCKETS)]
     (ws/send msg))))
