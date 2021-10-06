(ns snapcast-control.views
  (:require
   [re-frame.core :as re-frame]
   [reagent.core :as reagent]
   [snapcast-control.subs :as subs]
   [snapcast-control.events :as events]
   [snapcast-control.util :as util]
   [clojure.string :as s]
   ["javascript-time-ago" :default TimeAgo]
   ["javascript-time-ago/locale/en" :default en])
  )

(defn show-bool [b]
  (if b "True" "False"))

(defn s-or [s default]
  (if (s/blank? s)
    default
    s))

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

(.addDefaultLocale TimeAgo en)
(def time-ago-formatter (TimeAgo. "en-US"))
(defn time-ago [sec usec]
  (let [ms (+ (* sec 1000) (quot usec 1000))
        date (js/Date. ms)]
    (.format time-ago-formatter date)))

(defn client [client-data]
  (let [state (reagent/atom {:editing false
                             :name (util/client-name client-data)})]
    (fn [client-data]
      [:div.client
       [:h3 (if-not (:editing @state)
              (let [name  (util/client-name client-data)]
                [:span.client-name {:on-click #(swap! state assoc :editing true)}
                 (s-or name [:span.no-name "None"])])
              [:input {:type "text"
                       :on-change (fn [e]  ( swap! state assoc :name (-> e .-target .-value)))
                       :on-key-down (fn [e]
                                       (case (.-key e)
                                         "Enter" (do
                                                   (.preventDefault e)
                                                   (re-frame/dispatch [::events/set-client-name (:id client-data) (-> e .-target .-value)])
                                                   (swap! state assoc :editing false))
                                         "Escape" (do
                                                 (.preventDefault e)
                                                 (reset! state {:editing false
                                                                :name (util/client-name client-data)}))
                                         :default ))
                       :name (str (:id client-data) "_" name)
                       :value (:name @state)}])]
       [:dl.props
        [:dt "Id"] [:dd (:id client-data)]
        [:dt "Connected?"] [:dd (show-bool (:connected client-data))]
        [:dt "Volume"] [:dd (get-in client-data [:config :volume :percent])]
        [:dt "Muted?"] [:dd (show-bool (get-in client-data [:config :volume :muted]))]
        [:dt "Last seen"] [:dd (time-ago (get-in client-data [:lastSeen :sec])
                                         (get-in client-data [:lastSeen :usec]))]]])))

(defn clients []
  (let [clients @(re-frame/subscribe [::subs/filtered-clients])
        show-disconnected @(re-frame/subscribe [::subs/show-disconnected])]
    [:div.clients
     [:h2 "Clients"]
     [:div.filters
      [:label {:for "show-disconected"} "Show disconnected"]
      [:input
       {:type "checkbox"
        :id "show-disconnected"
        :checked show-disconnected
        :on-change #(re-frame/dispatch [::events/set-show-disconnected (-> % .-target .-checked)])}]]
     (doall (map (fn [c] [:div {:key (:id c)} [client c]]) clients))]))

(defn client-name [client-id]
  (let [name @(re-frame/subscribe [::subs/client-name client-id])]
    [:span.client-name (s-or name client-id)]))

(defn group [group-data]
  [:div.group {:key (:id group-data)}
   [:h3 (or (:name group-data) [:span.no-name "No name"])]
   [:div.group-clients
    [:h4 "Clients"]
    (if (empty? (:clients group-data))
      "None"
      (doall (map (fn [id] [:div {:key id} (client-name id)]) (:clients group-data))))]
   [:dl.props
    [:dt "Id"] [:dd (:id group-data)]
    [:dt "Muted?"] [:dd (show-bool (:muted group-data))]
    [:dt "Stream playing"] [:dd (s-or (:stream_id group-data) "None")]]])

(defn groups []
  (let [groups @(re-frame/subscribe [::subs/groups])]
    [:div.groups
     [:h2 "Groups"]
     (when-not (empty? groups)
       (doall (map group (vals groups))))]))

(defn main-panel []
  [:div
   [:div.connection (connection-input) (connect-button)]
   (refresh-button)
   [groups]
   (clients)
   ])
