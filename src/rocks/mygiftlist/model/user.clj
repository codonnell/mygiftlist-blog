(ns rocks.mygiftlist.model.user
  (:require
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.type.user :as user]))

(defonce users (atom {}))

(defresolver user-by-id [env {::user/keys [id]}]
  {::pc/input #{::user/id}
   ::pc/output [::user/email]}
  (get @users id))

(defmutation insert-user [env {::user/keys [id] :as user}]
  {::pc/params #{::user/id ::user/email}
   ::pc/output [::user/id]}
  (swap! users assoc id user))

(def user-resolvers
  [user-by-id
   insert-user])

(comment
  @users
  )
