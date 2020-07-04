(ns rocks.mygiftlist.server
  (:require [integrant.core :as ig]
            [org.httpkit.server :as http-kit]
            [rocks.mygiftlist.authentication :as auth]
            [rocks.mygiftlist.config :as config]
            [rocks.mygiftlist.db :as db]
            [rocks.mygiftlist.parser :as parser]
            [rocks.mygiftlist.transit :as transit]
            [com.fulcrologic.fulcro.server.api-middleware
             :refer [wrap-transit-params
                     wrap-transit-response]]
            [ring.util.response :as resp]
            [ring.middleware.defaults :refer [wrap-defaults
                                              site-defaults]]
            [ring.middleware.gzip :as gzip]))

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

(defn- wrap-healthcheck [handler pool uri]
  (fn [request]
    (if (= uri (:uri request))
      (do (db/execute! pool {:select [1] :from [:user]})
          {:status 200
           :body "Success"
           :headers {"Content-Type" "text/plain"}})
      (handler request))))

(defn handler [{:keys [parser wrap-jwt pool]}]
  (-> not-found-handler
    (wrap-api parser "/api")
    (wrap-healthcheck pool "/heathcheck")
    wrap-jwt
    (wrap-transit-params {:opts {:handlers transit/read-handlers}})
    (wrap-transit-response {:opts {:handlers transit/write-handlers}})
    (wrap-defaults (assoc-in site-defaults
                     [:security :anti-forgery] false))
    gzip/wrap-gzip))

(defmethod ig/init-key ::server
  [_ {::parser/keys [parser]
      ::config/keys [config]
      ::db/keys [pool]
      ::auth/keys   [wrap-jwt]}]
  (http-kit/run-server (handler {:parser   parser
                                 :config   config
                                 :pool pool
                                 :wrap-jwt wrap-jwt})
    {:port (:port config)}))

(defmethod ig/halt-key! ::server
  [_ server]
  (server))
