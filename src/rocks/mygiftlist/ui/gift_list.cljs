(ns rocks.mygiftlist.ui.gift-list
  (:require
   [com.fulcrologic.fulcro.algorithms.form-state :as fs]
   [com.fulcrologic.fulcro.algorithms.merge :as merge]
   [com.fulcrologic.fulcro.components :as comp :refer [defsc]]
   [com.fulcrologic.fulcro.dom :as dom]
   [com.fulcrologic.fulcro.mutations :as m]

   [com.fulcrologic.semantic-ui.elements.button.ui-button :refer [ui-button]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form :refer [ui-form]]
   [com.fulcrologic.semantic-ui.collections.form.ui-form-input :refer [ui-form-input]]

   [rocks.mygiftlist.type.gift-list :as gift-list]
   [rocks.mygiftlist.model.gift-list :as m.gift-list]
   [com.fulcrologic.fulcro.routing.dynamic-routing :as dr]))

(declare GiftListForm)

(defn- pristine-gift-list-form-state []
  (fs/add-form-config
    GiftListForm
    #::gift-list {:id (random-uuid)
                  :name ""}))

(defsc GiftListForm [this
                     {::gift-list/keys [name] :as gift-list}
                     {:keys [reset-form!]}]
  {:ident ::gift-list/id
   :query [::gift-list/id ::gift-list/name fs/form-config-join]
   :initial-state (fn [_] (pristine-gift-list-form-state))
   :form-fields #{::gift-list/name}}
  (let [validity (fs/get-spec-validity gift-list ::gift-list/name)]
    (ui-form
      {:onSubmit (fn [_]
                   (if (= :valid validity)
                     (do
                       (comp/transact! this
                         [(m.gift-list/create-gift-list
                            (select-keys gift-list
                              [::gift-list/id ::gift-list/name]))])
                       (reset-form!))
                     (comp/transact! this
                       [(fs/mark-complete! {})])))}
      (ui-form-input
        {:placeholder "Birthday 2020"
         :onChange (fn [evt]
                     (when (= :unchecked validity)
                       (comp/transact! this
                         [(fs/mark-complete!
                            {:field ::gift-list/name})]))
                     (m/set-string! this ::gift-list/name :event evt))
         :error (when (= :invalid validity)
                  "Gift list name cannot be blank")
         :autoFocus true
         :fluid true
         :value name})
      (ui-button
        {:type "submit"
         :primary true
         :disabled (= :invalid validity)}
        "Submit"))))

(def ui-gift-list-form (comp/factory GiftListForm))

(defsc GiftListFormPanel [this {:ui/keys [gift-list-form]}]
  {:ident (fn [] [:component/id :gift-list-form-panel])
   :query [{:ui/gift-list-form (comp/get-query GiftListForm)}]
   :initial-state {:ui/gift-list-form {}}}
  (dom/div {}
    (ui-gift-list-form
      (comp/computed gift-list-form
        {:reset-form! #(merge/merge-component! this GiftListFormPanel
                         {:ui/gift-list-form
                          (pristine-gift-list-form-state)})}))))

(def ui-gift-list-form-panel (comp/factory GiftListFormPanel))

(defsc GiftList [_this {::gift-list/keys [name]}]
  {:query [::gift-list/id ::gift-list/name]
   :ident ::gift-list/id
   :route-segment ["gift-list" ::gift-list/id]
   :will-enter (fn [_ {::gift-list/keys [id]}]
                 (dr/route-immediate [::gift-list/id (uuid id)]))}
  (dom/div
    (dom/h3 name)))

(def ui-gift-list (comp/factory GiftList))
