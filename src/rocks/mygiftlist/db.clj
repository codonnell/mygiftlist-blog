(ns rocks.mygiftlist.db
  (:require [rocks.mygiftlist.config :as config]
            [integrant.core :as ig]
            [hikari-cp.core :as pool]
            [next.jdbc :as jdbc]
            [next.jdbc.result-set :as result-set]
            [next.jdbc.prepare :as p]
            [clojure.string :as str]
            [honeysql.core :as sql]
            honeysql-postgres.format))

(defn datasource-options [database-spec]
  (merge {:auto-commit        true
          :read-only          false
          :connection-timeout 30000
          :validation-timeout 5000
          :idle-timeout       600000
          :max-lifetime       1800000
          :minimum-idle       10
          :maximum-pool-size  10
          :pool-name          "db-pool"
          :adapter            "postgresql"
          :register-mbeans    false}
    database-spec))

(defmethod ig/init-key ::pool
  [_ {::config/keys [config]}]
  (pool/make-datasource (datasource-options (config/database-spec config))))

(defmethod ig/halt-key! ::pool
  [_ pool]
  (pool/close-datasource pool))

(defn- qualify
  "Given a kebab-case database table name, returns the namespace that
  attributes coming from that table should have."
  [table]
  (when (seq table)
    (str "rocks.mygiftlist.type." table)))

(defn- snake->kebab [s]
  (str/replace s #"_" "-"))

(defn- as-qualified-kebab-maps [rs opts]
  (result-set/as-modified-maps rs
    (assoc opts
      :qualifier-fn (comp qualify snake->kebab)
      :label-fn snake->kebab)))

(def ^:private query-opts {:builder-fn as-qualified-kebab-maps})

(defn execute! [conn sql-map]
  (jdbc/execute! conn
    (sql/format sql-map :quoting :ansi)
    query-opts))

(defn execute-one! [conn sql-map]
  (jdbc/execute-one! conn
    (sql/format sql-map :quoting :ansi)
    query-opts))

(extend-protocol result-set/ReadableColumn

  ;; Automatically convert java.sql.Array into clojure vector in query
  ;; results
  java.sql.Array
  (read-column-by-label ^clojure.lang.PersistentVector
    [^java.sql.Array v _]
    (vec (.getArray v)))
  (read-column-by-index ^clojure.lang.PersistentVector
    [^java.sql.Array v _2 _3]
    (vec (.getArray v)))

  ;; Output java.time.LocalDate instead of java.sql.Date in query
  ;; results
  java.sql.Date
  (read-column-by-label ^java.time.LocalDate
    [^java.sql.Date v _]
    (.toLocalDate v))
  (read-column-by-index ^java.time.LocalDate
    [^java.sql.Date v _2 _3]
    (.toLocalDate v))

  ;; Output java.time.Instant instead of java.sql.Timestamp in query
  ;; results
  java.sql.Timestamp
  (read-column-by-label ^java.time.Instant
    [^java.sql.Timestamp v _]
    (.toInstant v))
  (read-column-by-index ^java.time.Instant
    [^java.sql.Timestamp v _2 _3]
    (.toInstant v)))


(extend-protocol p/SettableParameter

  ;; Accept java.time.Instant as a query param
  java.time.Instant
  (set-parameter
    [^java.time.Instant v ^java.sql.PreparedStatement ps ^long i]
    (.setTimestamp ps i (java.sql.Timestamp/from v)))

  ;; Accept java.time.LocalDate as a query param
  java.time.LocalDate
  (set-parameter
    [^java.time.LocalDate v ^java.sql.PreparedStatement ps ^long i]
    (.setTimestamp ps i (java.sql.Timestamp/valueOf (.atStartOfDay v)))))
