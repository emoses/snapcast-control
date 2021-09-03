(ns snapcast-control.db)

(def default-db
  {:connection {
                :url "ws://home.booty.coopermoses.com:1780"
                :status :disconnected
                }
   :groups {}
   :clients {}
   :streams {}})
