(ns rocks.mygiftlist.model.user
  (:require
   [rocks.mygiftlist.type.user :as user]
   [edn-query-language.core :as eql]
   [com.fulcrologic.fulcro.algorithms.normalized-state :refer [swap!->]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]))

(defmutation set-current-user [{::user/keys [id auth0-id email]
                                :as user}]
  (action [{:keys [state]}]
    (swap!-> state
      (assoc-in [:component/id :current-user] [::user/id id])
      (assoc-in [::user/id id] user)))
  (remote [_]
    (eql/query->ast1 `[(create-user
                         #::user{:id ~id
                                 :auth0-id ~auth0-id
                                 :email ~email})])))
