(ns snapcast-control.ws
  (:require
   [wscljs.client :as ws]
   [wscljs.format :as fmt]))

(defonce SOCKETS (atom {}))

(defn ws-cleanup
  ([socket-id ] (ws-cleanup socket-id (fn [&rest])))
  ([socket-id on-close]
   (fn [&rest]
     (when on-close (on-close))
     (swap! SOCKETS dissoc socket-id))))

(defn connect  [socket-id {:keys [url on-message on-error on-open on-close] :as m}]
  (let [socket (ws/create url
                          {:on-message on-message
                           :on-error on-error
                           :on-open on-open
                           :on-close (ws-cleanup socket-id on-close)})]
    (swap! SOCKETS assoc socket-id socket)))

(defn disconnect [socket-id]
   (let [socket (socket-id @SOCKETS)]
     (ws/close socket)))

(defn send [socket-id msg]
   (let [socket (socket-id @SOCKETS)]
     (ws/send socket msg fmt/json)))
