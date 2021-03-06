(ns rocks.mygiftlist.test-helper
  (:require [rocks.mygiftlist.authentication :as auth]
            [rocks.mygiftlist.db :as db]
            [next.jdbc :as jdbc]
            [integrant.core :as ig]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [rocks.mygiftlist.type.user :as user]))

(def system nil)

(defn use-system
  "Test fixture that initializes system components and sets it as the
  value of the `system` var, runs the test, then halts system
  components and resets `system` to nil. If no system components are
  passed in, initializes and halts the full system."
  [& component-keys]
  (fn [test-fn]
    (alter-var-root #'system
      (fn [_]
        (let [ig-config (merge
                          (ig/read-string
                            (slurp (io/resource "system.edn")))
                          (ig/read-string
                            (slurp (io/resource "resources/test.edn"))))]
          (if (seq component-keys)
            (ig/init ig-config component-keys)
            (ig/init ig-config)))))
    (test-fn)
    (ig/halt! system)
    (alter-var-root #'system (constantly nil))))

(def ^:private tables
  [:user])

(defn- double-quote [s]
  (format "\"%s\"" s))

(def ^:private truncate-all-tables
  "SQL vector that truncates all tables"
  [(str "TRUNCATE "
     (str/join " " (mapv (comp double-quote name) tables))
     " CASCADE")])

(defn truncate-after
  "Test fixtures that truncates all database tables after running the
  test. Assumes the `use-system` fixture has started the database
  connection pool."
  [test-fn]
  (test-fn)
  (jdbc/execute-one! (::db/pool system) truncate-all-tables))

(defn authenticate-with
  "Adds authentication claims to `env` for the given user."
  [env {::user/keys [auth0-id]}]
  (assoc-in env [:ring/request ::auth/claims :sub] auth0-id))
