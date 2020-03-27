(ns user
  (:require
   rocks.mygiftlist.server
   [clojure.tools.namespace.repl :as tools-ns :refer [set-refresh-dirs]]
   [mount.core :as mount]))

;; ==================== REPL TOOLING ====================

(set-refresh-dirs "src" "dev")

(defn start
  "Start the web server"
  [] (mount/start))

(defn stop
  "Stop the web server"
  [] (mount/stop))

(defn restart
  "Stop, reload code, and restart the server. If there is a compile error, use:
  ```
  (tools-ns/refresh)
  ```
  to recompile, and then use `start` once things are good."
  []
  (stop)
  (tools-ns/refresh :after 'user/start))
