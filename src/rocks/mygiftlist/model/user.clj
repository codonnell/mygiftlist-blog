(ns rocks.mygiftlist.model.user
  (:require
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.db :as db]
   [rocks.mygiftlist.type.user :as user]))

(defresolver user-by-id
  [{::db/keys [pool] :keys [requester-auth0-id]} {::user/keys [id]}]
  {::pc/input #{::user/id}
   ::pc/output [::user/id ::user/email ::user/auth0-id ::user/created-at]}
  (db/execute-one! pool
    {:select [:id :email :auth0_id :created_at]
     :from [:user]
     :where [:and
             [:= id :id]
             [:= requester-auth0-id :auth0_id]]}))

(defn- assign-tempid [{::user/keys [id] :as user} tempid]
  (assoc user :tempids {tempid id}))

(defmutation insert-user
  [{::db/keys [pool] :keys [requester-auth0-id]}
   {::user/keys [id auth0-id email] :as user}]
  {::pc/params #{::user/email ::user/auth0-id}
   ::pc/output [::user/id]}
  (when (= auth0-id requester-auth0-id)
    (cond-> (db/execute-one! pool
              {:insert-into :user
               :values [{:auth0_id auth0-id
                         :email email}]
               :upsert {:on-conflict [:auth0_id]
                        :do-update-set [:email]}
               :returning [:id]})
      id (assign-tempid id))))

(def user-resolvers
  [user-by-id
   insert-user])
