(ns rocks.mygiftlist.client
  (:require
   [rocks.mygiftlist.application :refer [SPA]]
   [rocks.mygiftlist.authentication :as auth]
   [com.fulcrologic.fulcro.algorithms.normalized-state :refer [swap!->]]
   [com.fulcrologic.fulcro.application :as app]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.mutations :refer [defmutation]]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr :refer [defrouter]]
   [clojure.core.async :as async :refer [go <!]]
   [clojure.string :as str]
   [edn-query-language.core :as eql]
   [pushy.core :as pushy]
   [taoensso.timbre :as log]))

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
  (pushy/start! history))

(defn route-to! [path]
  (pushy/set-token! history (path->url path)))

(defmutation route-to
  "Mutation to go to a specific route"
  [{:keys [path]}]
  (action [_]
    (route-to! path)))

(defsc LoginLogoutItem [this {:ui/keys [authenticated]}]
  {:query [:ui/authenticated]
   :ident (fn [] [:component/id :login-logout])
   :initial-state {:ui/authenticated nil}}
  (when (some? authenticated)
    (if authenticated
      (dom/a :.item {:onClick #(auth/logout)} "Logout")
      (dom/a :.item {:onClick #(auth/login)} "Login/Signup"))))

(def ui-login-logout-item (comp/factory LoginLogoutItem))

(defmutation set-authenticated [{:keys [authenticated]}]
  (action [{:keys [state]}]
    (swap!-> state
      (assoc-in [:component/id :login-logout :ui/authenticated] authenticated)
      (assoc :root/loading false))))

(declare LoginForm Home About)

(defsc Navbar [this {:keys [login-logout]}]
  {:query [{:login-logout (comp/get-query LoginLogoutItem)}]
   :ident (fn [] [:component/id :navbar])
   :initial-state {:login-logout {}}}
  (let [logged-in (:ui/authenticated login-logout)]
    (dom/div :.ui.secondary.menu
      (dom/a :.item
        {:onClick #(comp/transact! this
                     [(route-to {:path (dr/path-to (if logged-in
                                                     Home
                                                     LoginForm))})])}
        "Home")
      (dom/a :.item
        {:onClick #(comp/transact! this
                     [(route-to {:path (dr/path-to About)})])}
        "About")
      (dom/div :.right.menu
        (ui-login-logout-item login-logout)))))

(def ui-navbar (comp/factory Navbar))

(defsc LoginForm [this _]
  {:query []
   :ident (fn [] [:component/id :login])
   :route-segment ["login"]
   :initial-state {}}
  (dom/div {}
    (dom/div "In order to view and create gift lists, you need to...")
    (dom/div (dom/button :.ui.primary.button
               {:onClick #(auth/login)}
               "Log in or sign up"))))

(defsc Home [this _]
  {:query []
   :ident (fn [] [:component/id :home])
   :initial-state {}
   :route-segment ["home"]}
  (dom/div {}
    (dom/h3 {} "Home Screen")
    (dom/div {} "Welcome!")
    (dom/button :.ui.primary.button
      {:onClick #(auth/logout)}
      "Logout")))

(defsc About [this _]
  {:query []
   :ident (fn [] [:component/id :home])
   :initial-state {}
   :route-segment ["about"]}
  (dom/div {}
    (dom/h3 {} "About My Gift List")
    (dom/div {} "It's a really cool app!")))

(defn loading-spinner []
  (dom/div :.ui.active.inverted.dimmer
    (dom/div :.ui.loader)))

(defsc Loading [this _]
  {:query []
   :ident (fn [] [:component/id ::loading])
   :initial-state {}
   :route-segment ["loading"]}
  (loading-spinner))

(defrouter MainRouter [_ {:keys [current-state] :as props}]
  {:router-targets [Loading LoginForm Home About]}
  (loading-spinner))

(def ui-main-router (comp/factory MainRouter))

(defsc Root [this {:root/keys [router navbar loading]}]
  {:query [{:root/router (comp/get-query MainRouter)}
           {:root/navbar (comp/get-query Navbar)}
           :root/loading]
   :initial-state {:root/router {}
                   :root/navbar {}
                   :root/loading true}}
  (if loading
    (loading-spinner)
    (dom/div {}
      (ui-navbar navbar)
      (dom/div :.ui.container
        (ui-main-router router)))))

(defmutation set-current-user [user]
  (action [{:keys [state]}]
    (swap! state assoc-in [:component/id :current-user] (assoc user :ui/loading false))))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA Root "app"))

(defn- is-redirect? []
  (str/includes? (.. js/window -location -search) "code="))

(defn- clear-query-params! []
  (.replaceState js/window.history #js {} js/document.title js/window.location.pathname))

(defn ^:export init []
  (log/info "Application starting...")
  (app/mount! SPA Root "app")
  (dr/initialize! SPA)
  (pushy/start! history)
  (go
    (<! (auth/create-auth0-client!))
    (when (is-redirect?)
      (<! (auth/handle-redirect-callback))
      (clear-query-params!))
    (let [authenticated (<! (auth/is-authenticated?))]
      (comp/transact! SPA [(set-authenticated
                             {:authenticated authenticated})])
      (if authenticated
        (do (comp/transact! SPA
              [(route-to {:path (url->path js/window.location.pathname)})])
            (let [{:keys [sub email]} (<! (auth/get-user-info))]
              (comp/transact! SPA [(set-current-user
                                     #:user{:id sub :email email})])))
        (comp/transact! SPA
          [(route-to {:path (dr/path-to LoginForm)})])))))
