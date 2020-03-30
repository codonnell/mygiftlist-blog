(ns rocks.mygiftlist.server
  (:require [mount.core :refer [defstate]]
            [org.httpkit.server :as http-kit]
            [rocks.mygiftlist.config :as config]
            [rocks.mygiftlist.parser :as parser]
            [rocks.mygiftlist.transit :as transit]
            [com.fulcrologic.fulcro.server.api-middleware
             :refer [handle-api-request
                     wrap-transit-params
                     wrap-transit-response]]
            [ring.util.response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults
                                              site-defaults]]
            [ring.middleware.gzip :as gzip]
            [taoensso.timbre :as log]))

(defn- not-found-handler [_]
  (assoc-in (resp/resource-response "public/index.html")
    [:headers "Content-Type"] "text/html"))

(defn- wrap-api [handler uri]
  (fn [request]
    (if (= uri (:uri request))
      {:status 200
       :body (parser/parser {:ring/request request}
               (:transit-params request))
       :headers {"Content-Type" "application/transit+json"}}
      (handler request))))

(def handler
  (-> not-found-handler
    (wrap-api "/api")
    (wrap-transit-params {:opts {:handlers transit/read-handlers}})
    (wrap-transit-response {:opts {:handlers transit/write-handlers}})
    (wrap-defaults (assoc-in site-defaults
                     [:security :anti-forgery] false))
    gzip/wrap-gzip))

(defstate http-server
  :start
  (http-kit/run-server handler {:port config/port})
  :stop (http-server))
