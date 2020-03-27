(ns rocks.mygiftlist.config
  (:require [clojure.java.io :as io]
            [aero.core :as aero]))

(def ^:private config (aero/read-config (io/resource "config.edn")))

(def port (:port config))
