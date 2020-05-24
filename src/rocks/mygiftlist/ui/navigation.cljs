(ns rocks.mygiftlist.ui.navigation
  (:require
   [rocks.mygiftlist.authentication :as auth]
   [rocks.mygiftlist.routing :as routing]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]))

(defsc LoginLogoutItem [_this {:ui/keys [authenticated]}]
  {:query [:ui/authenticated]
   :ident (fn [] [:component/id :login-logout])
   :initial-state {:ui/authenticated nil}}
  (when (some? authenticated)
    (if authenticated
      (dom/a :.item {:onClick #(auth/logout)} "Logout")
      (dom/a :.item {:onClick #(auth/login)} "Login/Signup"))))

(def ui-login-logout-item (comp/factory LoginLogoutItem))

(defsc Navbar [this {:keys [login-logout]}]
  {:query [{:login-logout (comp/get-query LoginLogoutItem)}]
   :ident (fn [] [:component/id :navbar])
   :initial-state {:login-logout {}}}
  (let [logged-in (:ui/authenticated login-logout)]
    (dom/div :.ui.secondary.menu
      (dom/a :.item
        {:onClick #(comp/transact! this
                     [(routing/route-to {:path (if logged-in
                                                 ["home"]
                                                 ["login"])})])}
        "Home")
      (dom/a :.item
        {:onClick #(comp/transact! this
                     [(routing/route-to {:path ["about"]})])}
        "About")
      (dom/div :.right.menu
        (ui-login-logout-item login-logout)))))

(def ui-navbar (comp/factory Navbar))

(defsc CreatedGiftListItem [this {::gift-list/keys [id name]}]
  {:query [::gift-list/id ::gift-list/name]
   :ident ::gift-list/id}
  (dom/a :.item {:onClick #(comp/transact! this [(routing/route-to
                                                   {:path ["gift-list" id]})])}
    (dom/div {} name)))

(def ui-created-gift-list-item (comp/factory CreatedGiftListItem {:keyfn ::gift-list/id}))

(defsc LeftNav [_this {:keys [created-gift-lists]}]
  {:query [{:created-gift-lists (comp/get-query CreatedGiftListItem)}]
   :ident (fn [] [:component/id :left-nav])
   :initial-state {:created-gift-lists []}}
  (dom/div :.ui.vertical.menu
    (dom/div :.item
      (dom/div :.header "Created Gift Lists")
      (mapv ui-created-gift-list-item created-gift-lists))))

(def ui-left-nav (comp/factory LeftNav))
