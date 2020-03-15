(ns rocks.mygiftlist.client
  (:require [rocks.mygiftlist.application :refer [SPA]]
            [rocks.mygiftlist.authentication :as auth]
            [com.fulcrologic.fulcro.application :as app]
            [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
            [com.fulcrologic.fulcro.dom :as dom]
            [com.fulcrologic.fulcro.mutations :refer [defmutation]]
            [clojure.core.async :refer [go <!]]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(defsc CurrentUser [this {:user/keys [id email]
                          :ui/keys [loading] :as user}]
  {:query [:user/id :user/email :ui/loading]
   :ident (fn [] [:component/id :current-user])
   :initial-state {:ui/loading true}}
  (cond
    loading (dom/button :.ui.loading.primary.button)
    (and id email) (dom/button :.ui.primary.button
                     {:onClick #(auth/logout)}
                     "Logout")
    :else (dom/button :.ui.primary.button
            {:onClick #(auth/login)}
            "Login/Signup")))

(def ui-current-user (comp/factory CurrentUser))

(defsc Root [this {:root/keys [current-user]}]
  {:query [{:root/current-user (comp/get-query CurrentUser)}]
   :initial-state {:root/current-user {}}}
  (dom/div {}
    (dom/div {} "Hello World")
    (ui-current-user current-user)))

(defmutation set-current-user [user]
  (action [{:keys [state]}]
    (swap! state assoc-in [:component/id :current-user] (assoc user :ui/loading false))))

(defn ^:export refresh []
  (log/info "Hot code reload...")
  (app/mount! SPA Root "app"))

(defn ^:export init []
  (log/info "Application starting...")
  (app/mount! SPA Root "app")
  (go
    (<! (auth/create-auth0-client!))
    (when (str/includes? (.. js/window -location -search) "code=")
      (<! (auth/handle-redirect-callback))
      (.replaceState js/window.history #js {} js/document.title js/window.location.pathname))
    (if-let [authenticated (<! (auth/is-authenticated?))]
      (let [{:strs [sub email]} (js->clj (<! (auth/get-user-info)))]
        (comp/transact! SPA [(set-current-user {:user/id sub :user/email email})]))
      (comp/transact! SPA [(set-current-user {})]))))
