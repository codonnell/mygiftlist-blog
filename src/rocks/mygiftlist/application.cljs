(ns rocks.mygiftlist.application
  (:require [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.rendering.keyframe-render2 :as keyframe-render2]
            [com.fulcrologic.fulcro.networking.http-remote :as http-remote]
            [rocks.mygiftlist.transit :as transit]))

(defonce SPA
  (app/fulcro-app
    {:optimized-render! keyframe-render2/render!
     :remotes {:remote (http-remote/fulcro-http-remote
                         {:request-middleware
                          (http-remote/wrap-fulcro-request
                            identity transit/write-handlers)
                          :response-middleware
                          (http-remote/wrap-fulcro-response
                            identity transit/read-handlers)})}}))
