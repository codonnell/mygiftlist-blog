(ns rocks.mygiftlist.routing
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [clojure.string :as str]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
            [edn-query-language.core :as eql]
            [pushy.core :as pushy]))

(defn url->path
  "Given a url of the form \"/gift/123/edit?code=abcdef\", returns a
  path vector of the form [\"gift\" \"123\" \"edit\"]. Assumes the url
  starts with a forward slash. An empty url yields the path [\"home\"]
  instead of []."
  [url]
  (-> url (str/split "?") first (str/split "/") rest vec))

(defn path->url
  "Given a path vector of the form [\"gift\" \"123\" \"edit\"],
  returns a url of the form \"/gift/123/edit\"."
  [path]
  (str/join (interleave (repeat "/") path)))

(defn routable-path?
  "True if there exists a router target for the given path."
  [app path]
  (let [state-map  (app/current-state app)
        root-class (app/root-class app)
        root-query (comp/get-query root-class state-map)
        ast        (eql/query->ast root-query)]
    (some? (dr/ast-node-for-route ast path))))

(def default-route ["home"])

(defonce history (pushy/pushy
                   (fn [path]
                     (dr/change-route SPA path))
                   (fn [url]
                     (let [path (url->path url)]
                       (if (routable-path? SPA path)
                         path
                         default-route)))))

(defn start! []
  (dr/initialize! SPA)
  (pushy/start! history))

(defn route-to! [path]
  (pushy/set-token! history (path->url path)))

(defmutation route-to
  [{:keys [path]}]
  (action [_]
    (route-to! path)))
