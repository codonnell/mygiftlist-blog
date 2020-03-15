(ns rocks.mygiftlist.authentication
  (:require ["@auth0/auth0-spa-js" :as create-auth0-client]
            [com.wsscode.async.async-cljs :refer [go-promise <!p]]
            [rocks.mygiftlist.config :as config]))

(defonce auth0-client (atom nil))

(defn create-auth0-client! []
  (go-promise
    (reset! auth0-client
      (<!p (create-auth0-client
             #js {:domain config/AUTH0_DOMAIN
                  :client_id config/AUTH0_CLIENT_ID
                  :audience config/AUTH0_AUDIENCE
                  :connection config/AUTH0_CONNECTION})))))

(defn is-authenticated? []
  (go-promise (<!p (.isAuthenticated @auth0-client))))

(defn login []
  (.loginWithRedirect @auth0-client
    #js {:redirect_uri (.. js/window -location -origin)}))

(defn handle-redirect-callback []
  (go-promise (<!p (.handleRedirectCallback @auth0-client))))

(defn logout []
  (.logout @auth0-client
    #js {:returnTo (.. js/window -location -origin)}))

(defn get-access-token []
  (go-promise (<!p (.getTokenSilently @auth0-client))))

(defn get-user-info []
  (go-promise (<!p (.getUser @auth0-client))))
