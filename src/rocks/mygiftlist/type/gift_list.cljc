(ns rocks.mygiftlist.type.gift-list
  (:require [clojure.spec.alpha :as s]
            [rocks.mygiftlist.type.util :as t.util]))

(s/def ::name ::t.util/nonblank-string)
