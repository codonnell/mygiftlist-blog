(ns rocks.mygiftlist.model.gift-list-test
  (:require [rocks.mygiftlist.db :as db]
            [rocks.mygiftlist.parser :as parser]
            [rocks.mygiftlist.type.gift-list :as gift-list]
            [rocks.mygiftlist.type.user :as user]
            [rocks.mygiftlist.model.gift-list :as m.gift-list]
            [rocks.mygiftlist.model.user-test :as user-test]
            [rocks.mygiftlist.test-helper :as test-helper :refer [system]]
            [clojure.test :refer [use-fixtures deftest is]]
            [honeysql.core :as sql])
  (:import [java.util UUID]))

(use-fixtures :once (test-helper/use-system ::parser/parser ::db/pool))

(use-fixtures :each test-helper/truncate-after)

(defn example-gift-list
  ([]
   (example-gift-list {}))
  ([m]
   (merge #::gift-list{:id #uuid "5ac47a01-ce18-488e-b86e-9a70f3e3ca47"
                       :name "Test gift list"}
     m)))

(defn insert-entities! [pool {:keys [user gift-list]}]
  (let [u (user-test/example-user user)]
    (db/execute-one! pool
      {:insert-into :user
       :values [(user-test/example-user user)]
       :upsert {:on-conflict [:id]
                :do-nothing true}
       :returning [:id]})
    (if gift-list
      (let [gl (assoc-in (example-gift-list gift-list)
                 [::gift-list/created-by ::user/id] (::user/id u))]
        (db/execute-one! pool
          {:insert-into :gift_list
           :values [(assoc (example-gift-list gift-list)
                      ::gift-list/created-by-id (::user/id u))]})
        {:user u
         :gift-list gl})
      {:user u})))

(deftest test-create-gift-list
  (let [{::parser/keys [parser] ::db/keys [pool]} system
        {:keys [user]} (insert-entities! pool {:user {}})
        gift-list (example-gift-list)]
    (parser (test-helper/authenticate-with {} user)
      `[(m.gift-list/create-gift-list ~gift-list)])
    (is (= {:count 1}
          (db/execute-one! pool {:select [(sql/call :count :*)]
                                 :from [:gift_list]}))
      "There is one gift list in the database after insert")
    (is (= (assoc (select-keys gift-list [::gift-list/id ::gift-list/name])
             ::gift-list/created-by-id (::user/id user))
          (db/execute-one! pool {:select [:id :name :created_by_id]
                                 :from [:gift_list]}))
      "The gift list in the database's id, name, and created by matches what was inserted")))

(deftest test-unauthorized-create-gift-list
  (let [{::parser/keys [parser] ::db/keys [pool]} system]
    (parser {} `[(m.gift-list/create-gift-list ~(example-gift-list))])
    (is (= {:count 0}
          (db/execute-one! pool {:select [(sql/call :count :*)]
                                 :from [:gift_list]}))
      "No gift list is inserted when no credentials are provided.")))

(deftest test-gift-list-by-id
  (let [{::parser/keys [parser] ::db/keys [pool]} system
        {:keys [user gift-list]} (insert-entities! pool
                                   {:user {} :gift-list {}})
        id (::gift-list/id gift-list)
        ret (parser (test-helper/authenticate-with {} user)
              [{[::gift-list/id id]
                [::gift-list/id ::gift-list/name ::gift-list/created-at
                 {::gift-list/created-by [::user/id]}]}])]
    (is (= (assoc-in gift-list [::gift-list/created-by ::user/id] (::user/id user))
          (select-keys (get ret [::gift-list/id id])
            [::gift-list/id ::gift-list/name ::gift-list/created-by]))
      "The query result attributes match what was inserted")
    (is (instance? java.time.Instant
          (get-in ret [[::gift-list/id id] ::gift-list/created-at]))
      "The query result's created-at value is an instant")))

(deftest test-unauthorized-gift-list-by-id
  (let [{::parser/keys [parser] ::db/keys [pool]} system
        {:keys [gift-list]} (insert-entities! pool
                              {:user {} :gift-list {}})
        unauthorized-user-id (UUID/randomUUID)
        {unauthorized-user :user} (insert-entities! pool
                                    {:user {::user/id unauthorized-user-id
                                            ::user/email "you@example.com"
                                            ::user/auth0-id "auth0|def456"}})
        id (::gift-list/id gift-list)
        query [{[::gift-list/id id]
                [::gift-list/id ::gift-list/name ::gift-list/created-at
                 {::gift-list/created-by [::user/id]}]}]
        empty-creds-ret (parser {} query)
        incorrect-creds-ret
        (parser (test-helper/authenticate-with {} unauthorized-user) query)]
    (is (every? #(not (contains? (get empty-creds-ret [::gift-list/id id]) %))
          [::gift-list/name ::gift-list/created-by ::gift-list/created-at])
      "Unauthenticated requests cannot access gift list data")
    (is (every? #(not (contains? (get incorrect-creds-ret [::gift-list/id id]) %))
          [::gift-list/name ::gift-list/created-by ::gift-list/created-at])
      "Unauthorized requests cannot access gift list data")))

(deftest test-created-gift-lists
  (let [{::parser/keys [parser] ::db/keys [pool]} system
        {user :user gift-list1 :gift-list} (insert-entities! pool
                                             {:user {} :gift-list {}})
        {gift-list2 :gift-list} (insert-entities! pool
                                  {:user {}
                                   :gift-list {::gift-list/id (UUID/randomUUID)}})
        ret (parser (test-helper/authenticate-with {} user)
              [{:created-gift-lists [::gift-list/id]}])]
    (is (= [{::gift-list/id (::gift-list/id gift-list1)}
            {::gift-list/id (::gift-list/id gift-list2)}]
          (:created-gift-lists ret))
      "The query result has both created gift lists in order of creation")))

(deftest test-unauthorized-created-gift-lists
  (let [{::parser/keys [parser] ::db/keys [pool]} system
        _ (insert-entities! pool {:user {} :gift-list {}})
        unauthorized-user-id (UUID/randomUUID)
        {unauthorized-user :user} (insert-entities! pool
                                    {:user {::user/id unauthorized-user-id
                                            ::user/email "you@example.com"
                                            ::user/auth0-id "auth0|def456"}})
        query [{:created-gift-lists [::gift-list/id]}]
        empty-creds-ret (parser {} query)
        incorrect-creds-ret
        (parser (test-helper/authenticate-with {} unauthorized-user) query)]
    (is (empty? (:created-gift-lists empty-creds-ret))
      "An unauthenticated user gets no created gift lists")
    (is (empty? (:created-gift-lists incorrect-creds-ret))
      "One user does not get another user's created gift lists")))
