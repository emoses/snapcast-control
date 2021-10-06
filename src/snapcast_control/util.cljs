(ns snapcast-control.util)

(defn client-name [client-data]
  (let [name (get-in client-data [:config :name])]
     (if-not (s/blank? name)
       name
       (get-in client-data [:host :name]))))
