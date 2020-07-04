(ns rocks.mygiftlist.main
  (:require rocks.mygiftlist.server
            rocks.mygiftlist.parser
            rocks.mygiftlist.db
            rocks.mygiftlist.config
            rocks.mygiftlist.authentication
            [integrant.core :as ig]
            [clojure.java.io :as io]))

(defn -main [& _args]
  (-> "system.edn"
    io/resource
    slurp
    ig/read-string
    ig/init))
