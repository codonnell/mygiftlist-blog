(ns user
  (:require
   rocks.mygiftlist.server
   rocks.mygiftlist.parser
   rocks.mygiftlist.db
   rocks.mygiftlist.config
   rocks.mygiftlist.authentication
   [integrant.core :as ig]
   [integrant.repl :refer [clear go halt prep init reset reset-all]]
   [integrant.repl.state :refer [system]]
   [clojure.java.io :as io]))

(integrant.repl/set-prep!
  (fn []
    (merge
      (ig/read-string (slurp (io/resource "system.edn")))
      (ig/read-string (slurp (io/resource "resources/dev.edn"))))))

(comment
  system
  (go)
  (reset)
  (halt)
  )
