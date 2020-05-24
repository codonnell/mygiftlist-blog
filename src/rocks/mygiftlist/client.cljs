(ns rocks.mygiftlist.client
  (:require
   [rocks.mygiftlist.application :refer [SPA]]
   [rocks.mygiftlist.authentication :as auth]
   [rocks.mygiftlist.routing :as routing]
   [rocks.mygiftlist.model.user :as m.user]
   [rocks.mygiftlist.type.user :as user]
   [rocks.mygiftlist.ui.navigation :as ui.nav]
   [rocks.mygiftlist.ui.root :as ui.root]
   [com.fulcrologic.fulcro.algorithms.normalized-state :refer [swap!->]]
   [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp]
   [com.fulcrologic.fulcro.data-fetch :as df]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]
   [com.fulcrologic.fulcro.mutations :refer [defmutation]]
   [clojure.core.async :as async :refer [go <!]]
   [clojure.string :as str]
   [taoensso.timbre :as log]))

(defmutation set-authenticated [{:keys [authenticated]}]
  (action [{:keys [state]}]
    (swap!-> state
      (assoc-in [:component/id :login-logout :ui/authenticated] authenticated)
      (assoc :root/loading false))))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA ui.root/Root "app"))

(defn- is-redirect? []
  (str/includes? (.. js/window -location -search) "code="))

(defn- clear-query-params! []
  (.replaceState js/window.history #js {} js/document.title js/window.location.pathname))

(defn ^:export init []
  (log/info "Application starting...")
  (app/mount! SPA ui.root/Root "app")
  (go
    (<! (auth/create-auth0-client!))
    (when (is-redirect?)
      (<! (auth/handle-redirect-callback))
      (clear-query-params!))
    (let [authenticated (<! (auth/is-authenticated?))]
      (comp/transact! SPA [(set-authenticated
                             {:authenticated authenticated})])
      (routing/start!)
      (if authenticated
        (do (df/load! SPA [:component/id :left-nav] ui.nav/LeftNav)
            (let [{:keys [sub email]} (<! (auth/get-user-info))]
              (comp/transact! SPA [(m.user/set-current-user
                                     #::user{:id (tempid/tempid)
                                             :auth0-id sub
                                             :email email})])))
        (comp/transact! SPA
          [(routing/route-to {:path (dr/path-to ui.root/LoginForm)})])))))
