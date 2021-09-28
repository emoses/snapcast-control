(ns snapcast-control.fx
  (:require
   [re-frame.core :as re-frame]
   [snapcast-control.ws :as ws]))

(re-frame/reg-fx
 ::ws-connect
 (fn [[socket-id options]]
   (ws/connect socket-id options)))

(re-frame/reg-fx
 ::ws-disconnect
 (fn [[socket-id]]
   (ws/disconnect socket-id)))

(re-frame/reg-fx
 ::ws-send
 (fn [[socket-id msg]]
   (ws/send socket-id msg)) )
