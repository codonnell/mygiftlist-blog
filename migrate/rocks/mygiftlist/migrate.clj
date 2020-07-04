(ns rocks.mygiftlist.migrate
  (:require [clojure.string :as str])
  (:import [java.net URI]
           [org.flywaydb.core Flyway]
           [org.flywaydb.core.api Location]))

(defn database-url->datasource-args [database-url]
  (let [{:keys [userInfo host port path]} (bean (URI. database-url))
        [username password] (str/split userInfo #":")]
    {:jdbc-url (str "jdbc:postgresql://" host ":" port path)
     :username username
     :password password}))

(defn migrate [{:keys [database-url]}]
  (let [{:keys [jdbc-url username password]}
        (database-url->datasource-args
          (or database-url (System/getenv "DATABASE_URL")))]
    (.. (Flyway/configure)
      (dataSource jdbc-url username password)
      (locations (into-array Location
                   [(Location. "filesystem:./migrations")]))
      (load)
      (migrate))))
