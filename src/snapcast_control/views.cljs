(ns snapcast-control.views
  (:require
   [re-frame.core :as re-frame]
   [snapcast-control.subs :as subs]
   [snapcast-control.events :as events]
   ))

(defn connect-button []
  (let [connection-state @(re-frame/subscribe [::subs/connection-state])]
    (case connection-state
      :connected [:button
                  {:type "button"
                   :on-click #(re-frame/dispatch [::events/disconnect])}
                  "Disconnect"]
      :connecting [:button
                   {:type "button"
                    :disabled true}
                   "Connecting..."]
      :disconnecting [:button
                   {:type "button"
                    :disabled true}
                   "Disconnecting..."]
      :disconnected [:button
                     {:type "button"
                      :on-click #(re-frame/dispatch [::events/connect])}
                     "Connect"])))

(defn refresh-button []
  (let [connection-state @(re-frame/subscribe [::subs/connection-state])
        disabled (not= connection-state :connected)]
    [:button
     {:type "button"
      :on-click #(re-frame/dispatch [::events/refresh])
      :disabled disabled}
     "Refresh"]))

(defn connection-input []
  (let [url @(re-frame/subscribe [::subs/connection-url])
        status @(re-frame/subscribe [::subs/connection-state])]
    [:div.form
     [:label "Server URL"]
     (if (= status :disconnected)
       [:input
        {:type "text"
         :value url
         :on-change #(re-frame/dispatch [::events/update-connection-url (-> % .-target .-value)])}]
       [:span.url url])]))

(defn client [client-data]
  [:div.client {:key (or "none" (:id client-data))}
   [:h3 (get-in client-data [:config :name] )]
   [:dl
    [:dt "Id"] [:dd (:id client-data)]
    [:dt "Connected?"] [:dd (if  (:connected client-data) "True" "False")]]])

(defn clients []
  (let [clients @(re-frame/subscribe [::subs/clients])]
    [:div
     [:h2 "Clients"
      (when-not (empty? clients)
        (map client (vals clients)))]]))

(defn main-panel []
  [:div
   [:div.connection (connection-input) (connect-button)]
   (refresh-button)
   (clients)
   ])
