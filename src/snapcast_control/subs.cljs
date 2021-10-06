(ns snapcast-control.subs
  (:require
   [re-frame.core :as re-frame]
   [snapcast-control.util :as util]))

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
   (util/client-name (clients client-id))))

(re-frame/reg-sub
 ::show-disconnected
 (fn [db]
   (get-in db [:view :show-disconnected])))

(re-frame/reg-sub
 ::filtered-clients
 :<- [::clients]
 :<- [::show-disconnected]
 (fn [[clients show?] _]
   (let [f (if show? (fn [c] true) (fn [c] (:connected c)))]
     (filter f (vals clients)))))
