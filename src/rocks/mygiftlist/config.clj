(ns rocks.mygiftlist.config
  (:require [integrant.core :as ig]
            [clojure.java.io :as io]
            [aero.core :as aero]))

(defmethod ig/init-key ::config
  [_ {::keys [profile]}]
  (aero/read-config (io/resource "config.edn")
    {:profile profile}))

(defn database-spec [config]
  (:database-spec config))

(defn port [config]
  (:port config))

(defn jwk-endpoint [config]
  (get-in config [:auth :jwk-endpoint]))

(defn jwt-issuer [config]
  (get-in config [:auth :issuer]))

(defn jwt-audience [config]
  (get-in config [:auth :audience]))
