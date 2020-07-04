(ns rocks.mygiftlist.type.util
  (:require [clojure.string :as str]
            [clojure.spec.alpha :as s]))

(s/def ::nonblank-string (s/and string? (complement str/blank?)))
