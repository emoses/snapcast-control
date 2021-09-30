(ns snapcast-control.snapcast-client
  (:require
   [re-frame.core :as re-frame]
   [snapcast-control.ws :as ws]
   [clojure.string :as s])
  (:use
   [cljs.pprint :only [pprint]]))

(defrecord Client [url
                   socket-id
                   notification-handler
                   next-id
                   ;; id -> {:timestamp, :method, :callback}
                   in-flight])

(defn make-client [url socket-id & notification-handler]
  (->Client url socket-id (first notification-handler) (atom 0) (atom {})))

(defn next-id [client]
  (swap! (:next-id client) inc))

(defn new-msg [client method & params]
  (let [msg {:id (next-id client)
             :jsonrpc "2.0"
             :method method}]
    (if params
      (assoc msg :params params)
      msg)))

(defn notification? [msg]
  (let [method (:method msg)]
    (and method
      (let [vals (s/split method #"\.")]
        (if (>= (count vals) 2)
          (->
           vals
           (nth 1)
           (s/starts-with? "On"))
          false)))))

(defn handle-response [client msg]
  (let [id (:id msg)]
    (when-let [in-flight (@(:in-flight client) id)]
      (swap! (:in-flight client) dissoc id)
      ((:callback in-flight) msg))))

(defn event->msg [e]
  (-> e
      (.-data)
      (#(.parse js/JSON %))
      (js->clj :keywordize-keys true)))

(defn process-message [client msg-event]
  (let [msg (event->msg msg-event)]
    (if (notification? msg)
      (do
        (when-let [f (:notification-handler client)]
          (f msg)))
      (handle-response client msg))))

(defn- send [client msg callback]
  (ws/send (:socket-id client) msg))

(defn send-msg
  ([client method callback]
   (send-msg client method {} callback))
  ([client method params callback]
   (let [msg (new-msg client method)
         id (:id msg)
         m {:method method
            :timestamp (.now js/Date)
            :callback callback}]
     (swap! (:in-flight client) assoc id m)
     (send client msg callback))))

(defn connect [client on-open on-close on-error]
  (ws/connect (:socket-id client) {:url (:url client)
                                   :on-message (partial process-message client)
                                   :on-open on-open
                                   :on-close on-close
                                   :on-error on-error}))

(defn disconnect [client]
  (ws/disconnect (:socket-id client)))
