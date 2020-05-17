(ns rocks.mygiftlist.model.user-test
  (:require [rocks.mygiftlist.db :as db]
            [rocks.mygiftlist.parser :as parser]
            [rocks.mygiftlist.type.user :as user]
            [rocks.mygiftlist.model.user :as m.user]
            [rocks.mygiftlist.test-helper :as test-helper :refer [system]]
            [com.fulcrologic.fulcro.algorithms.tempid :as tempid]
            [clojure.test :refer [use-fixtures deftest is]]
            [honeysql.core :as sql]))

(use-fixtures :once (test-helper/use-system ::parser/parser ::db/pool))

(use-fixtures :each test-helper/truncate-after)

(defn example-user
  ([]
   (example-user {}))
  ([m]
   (merge #::user{:id #uuid "e58efe42-8a06-45b0-a11e-3f609580932d"
                  :email "me@example.com"
                  :auth0-id "auth0|abc123"}
     m)))

(deftest test-create-user
  (let [tempid (tempid/tempid)
        {::parser/keys [parser] ::db/keys [pool]} system
        u (example-user {::user/id tempid})
        ret (parser (test-helper/authenticate-with {} u)
              `[(m.user/create-user ~u)])]
    (is (= {:count 1}
          (db/execute-one! pool {:select [(sql/call :count :*)]
                                 :from [:user]}))
      "There is one user in the database after insert")
    (is (= (select-keys u [::user/email ::user/auth0-id])
          (db/execute-one! pool {:select [:email :auth0_id]
                                 :from [:user]}))
      "The user in the database's email and auth0 id matches what was inserted")
    (let [user-id (::user/id (db/execute-one! pool
                               {:select [:id]
                                :from [:user]}))]
      (is (= {::user/id user-id :tempids {tempid user-id}}
            (get ret `m.user/create-user))
        "The parser return value has the user id and tempids mapping"))))

(deftest test-unauthorized-create-user
  (let [{::parser/keys [parser] ::db/keys [pool]} system]
    (parser {} `[(m.user/create-user ~(example-user))])
    (is (= {:count 0}
          (db/execute-one! pool {:select [(sql/call :count :*)]
                                 :from [:user]}))
      "No user is inserted when no credentials are provided.")
    (parser (test-helper/authenticate-with {}
              (example-user {::user/auth0-id "auth0|def456"}))
      `[(m.user/create-user ~(example-user))])
    (is (= {:count 0}
          (db/execute-one! pool {:select [(sql/call :count :*)]
                                 :from [:user]}))
      "No user is inserted when unauthorized credentials are provided.")))

(deftest test-user-by-id
  (let [{::user/keys [id] :as u} (example-user)
        {::parser/keys [parser] ::db/keys [pool]} system
        _ (db/execute-one! pool {:insert-into :user
                                 :values [u]})
        ret (parser (test-helper/authenticate-with {} u)
              [{[::user/id id]
                [::user/id ::user/email
                 ::user/auth0-id ::user/created-at]}])]
    (is (= u
          (select-keys (get ret [::user/id id])
            [::user/id ::user/auth0-id ::user/email]))
      "The query result attributes match what was inserted")
    (is (instance? java.time.Instant
          (get-in ret [[::user/id id] ::user/created-at]))
      "The query result's created-at value is an instant")))

(deftest test-unauthorized-user-by-id
  (let [{::user/keys [id] :as u} (example-user)
        {::parser/keys [parser] ::db/keys [pool]} system
        _ (db/execute-one! pool {:insert-into :user
                                 :values [u]})
        query [{[::user/id id]
                [::user/id ::user/email
                 ::user/auth0-id ::user/created-at]}]
        empty-creds-ret (parser {} query)
        incorrect-creds-ret
        (parser (test-helper/authenticate-with {}
                  (example-user {::user/auth0-id "auth0|def456"}))
          query)]
    (is (every? #(not (contains? (get empty-creds-ret [::user/id id]) %))
          [::user/email ::user/auth0-id ::user/created-at])
      "Unauthenticated requests cannot access user data")
    (is (every? #(not (contains? (get incorrect-creds-ret [::user/id id]) %))
          [::user/email ::user/auth0-id ::user/created-at])
      "Unauthorized requests cannot access user data")))

(comment
  (db/execute-one! db/pool {:insert-into :user
                            :values [(example-user)]})
  (db/execute-one! db/pool {:select [:*]
                            :from [:user]})
  (parser/parser {}
    [{[::user/id (::user/id (example-user))]
      [::user/id ::user/email ::user/auth0-id ::user/created-at]}])
  )
