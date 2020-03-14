(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [taoensso.timbre :as log]))

(defsc Root [this _]
  {:query []
   :initial-state {}}
  (dom/div {}
    "Hello World"))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA Root "app"))

(defn ^:export init []
  (log/info "Application starting...")
  (app/mount! SPA Root "app"))
