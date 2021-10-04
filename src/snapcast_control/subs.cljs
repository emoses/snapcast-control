(ns snapcast-control.subs
  (:require
   [re-frame.core :as re-frame]))

(re-frame/reg-sub
 ::name
 (fn [db]
   (:name db)))

(re-frame/reg-sub
 ::clients
 (fn [db]
   (:clients db)))

(re-frame/reg-sub
 ::groups
 (fn [db]
   (:groups db)))

(re-frame/reg-sub
 ::connection-url
 (fn [db]
   (get-in db [:connection :url])))

(re-frame/reg-sub
 ::connection-state
 (fn [db]
   (get-in db [:connection :status])))

(re-frame/reg-sub
 ::client-name
 :<- [::clients]
 (fn [clients [_ client-id]]
   (get-in clients [client-id :config :name])))
