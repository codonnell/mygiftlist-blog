(ns rocks.mygiftlist.migrate
  (:require [clojure.string :as str])
  (:import [org.flywaydb.core Flyway]
           [org.flywaydb.core.api Location]))

(defn database-url->datasource-args [database-url]
  (let [{:keys [userInfo host port path]} (bean (java.net.URI. database-url))
        [username password] (str/split userInfo #":")]
    [(str "jdbc:postgresql://" host ":" port path) username password]))

(defn -main [& _args]
  (let [[jdbc-url username password] (database-url->datasource-args (System/getenv "DATABASE_URL"))]
    (.. (Flyway/configure)
      (dataSource jdbc-url username password)
      (locations (into-array Location [(Location. "filesystem:./migrations")]))
      (load)
      (migrate))))
