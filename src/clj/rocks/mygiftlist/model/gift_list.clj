(ns rocks.mygiftlist.model.gift-list
  (:require
   [com.wsscode.pathom.connect :as pc :refer [defresolver defmutation]]
   [rocks.mygiftlist.db :as db]
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.type.user :as user]))

(defresolver gift-list-by-id
  [{::db/keys [pool] :keys [requester-id]} inputs]
  {::pc/input #{::gift-list/id}
   ::pc/output [::gift-list/id ::gift-list/name ::gift-list/created-at
                {::gift-list/created-by [::user/id]}]
   ::pc/transform pc/transform-batch-resolver}
  (->> {:select [:gl.id :gl.name :gl.created_at :gl.created_by_id]
        :from [[:gift_list :gl]]
        :where [:and
                [:= requester-id :gl.created_by_id]
                [:in :gl.id (mapv ::gift-list/id inputs)]]}
    (db/execute! pool)
    (mapv (fn [{::gift-list/keys [created-by-id] :as gift-list}]
            (-> gift-list
              (assoc-in [::gift-list/created-by ::user/id] created-by-id)
              (dissoc ::gift-list/created-by-id))))
    (pc/batch-restore-sort {::pc/inputs inputs ::pc/key ::gift-list/id})))

(defresolver created-gift-lists
  [{::db/keys [pool] :keys [requester-id]} _]
  {::pc/input #{}
   ::pc/output [{:created-gift-lists [::gift-list/id]}]}
  {:created-gift-lists
   (db/execute! pool
     {:select [:gl.id]
      :from [[:gift_list :gl]]
      :where [:= requester-id :gl.created_by_id]
      :order-by [:gl.created_at]})})

(defmutation create-gift-list
  [{::db/keys [pool] :keys [requester-id]} {::gift-list/keys [id name]}]
  {::pc/params #{::gift-list/id ::gift-list/name}
   ::pc/output [::gift-list/id]}
  (when requester-id
    (db/execute-one! pool
      {:insert-into :gift_list
       :values [{:id id
                 :name name
                 :created_by_id requester-id}]
       :upsert {:on-conflict [:id]
                :do-update-set [:id]}
       :returning [:id]})))

(def gift-list-resolvers
  [gift-list-by-id
   created-gift-lists
   create-gift-list])
