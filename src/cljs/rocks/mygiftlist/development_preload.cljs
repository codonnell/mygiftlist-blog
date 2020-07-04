(ns rocks.mygiftlist.development-preload
  (:require [com.fulcrologic.fulcro.algorithms.timbre-support :as ts]
            [taoensso.timbre :as log]))

;; Add code to this file that should run when the initial application is loaded in development mode.
;; shadow-cljs already enables console print and plugs in devtools if they are on the classpath,

(js/console.log "Turning logging to :all (in rocks.mygiftlist.development-preload)")
(log/set-level! :debug)
(log/merge-config! {:output-fn ts/prefix-output-fn
                    :appenders {:console (ts/console-appender)}})
