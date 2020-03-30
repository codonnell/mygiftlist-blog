(ns rocks.mygiftlist.transit
  (:require [cognitect.transit :as t])
  #?(:clj (:import [java.time Instant])))

(def write-handlers
  #?(:clj
     {Instant (t/write-handler "m"
                (fn [^Instant t] (.toEpochMilli t))
                (fn [^Instant t] (.toString (.toEpochMilli t))))}
     :cljs {}))

(def read-handlers
  #?(:clj
     {"m" (t/read-handler
            (fn [s] (Instant/ofEpochMilli s)))}
     :cljs {}))
