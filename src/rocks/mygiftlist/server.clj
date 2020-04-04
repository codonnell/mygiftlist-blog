(ns rocks.mygiftlist.server
  (:require [integrant.core :as ig]
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

(defn- wrap-api [handler parser uri]
  (fn [request]
    (if (= uri (:uri request))
      {:status 200
       :body (parser {:ring/request request}
               (:transit-params request))
       :headers {"Content-Type" "application/transit+json"}}
      (handler request))))

(defn handler [parser]
  (-> not-found-handler
    (wrap-api parser "/api")
    (wrap-transit-params {:opts {:handlers transit/read-handlers}})
    (wrap-transit-response {:opts {:handlers transit/write-handlers}})
    (wrap-defaults (assoc-in site-defaults
                     [:security :anti-forgery] false))
    gzip/wrap-gzip))

(defmethod ig/init-key ::server
  [_ {::parser/keys [parser] ::config/keys [config]}]
  (http-kit/run-server (handler parser) {:port (:port config)}))

(defmethod ig/halt-key! ::server
  [_ server]
  (server))
