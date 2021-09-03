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
 ::connection-url
 (fn [db]
   (get-in db [:connection :url])))

(re-frame/reg-sub
 ::connection-state
 (fn [db]
   (get-in db [:connection :status])))
