(ns rocks.mygiftlist.model.gift-list
  (:require
   [rocks.mygiftlist.type.gift-list :as gift-list]
   [com.fulcrologic.fulcro.algorithms.normalized-state :refer [swap!->]]
   [com.fulcrologic.fulcro.mutations :as m :refer [defmutation]]))

(defmutation create-gift-list [{::gift-list/keys [id] :as gift-list}]
  (action [{:keys [state]}]
    (swap!-> state
      (assoc-in [::gift-list/id id] gift-list)
      (update-in [:component/id :left-nav :created-gift-lists]
        conj gift-list)))
  (remote [_] true))
