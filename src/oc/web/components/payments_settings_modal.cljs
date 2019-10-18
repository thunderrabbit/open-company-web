(ns oc.web.components.payments-settings-modal
  (:require [rum.core :as rum]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.lib.utils :as utils]
            [oc.web.mixins.ui :as ui-mixins]
            [oc.web.actions.nav-sidebar :as nav-actions]
            [oc.web.components.ui.dropdown-list :refer (dropdown-list)]))

(defn- user-count [team-data]
  (let [user-count (count (:users team-data))]
    (if (> user-count 1)
      [:span [:strong (str user-count " users")] " are"]
      [:span [:strong  "1 user"] " is"])))

(defn plan-summary [s team-data]
  [:div.plan-summary
    [:div.plan-summary-details
      "Payment method:"
      [:br]
      "Visa ending in 8059, exp: 02/2022"
      [:button.mlb-reset.change-pay-method-bt
        "Change"]]
    [:div.plan-summary-details.bottom-margin
      "Billing period:"
      [:br]
      "Plan billed annually ($1,200.00)"
      [:br]
      "Next payment due on Sept 16, 2020"
      [:button.mlb-reset.change-pay-method-bt
        {:on-click #(reset! (::payments-tab s) :change)}
        "Change"]]
    [:div.plan-summary-separator]
    [:div.plan-summary-details
      [:button.mlb-reset.history-bt
        "Lookup billing history"]]
    (comment
      [:div.plan-summary-separator]
      [:div.plan-summary-details
        "Have a team of 250+"
        [:a.chat-with-us
          {:class "intercom-chat-link"
           :href "mailto:zcwtlybw@carrot-test-28eb3360a1a3.intercom-mail.com"}
          "Chat with us"]])])

(defn- plan-description [plan]
  (case plan
    "team-monthly"
    "Monthly plan"
    "team-annual"
    "Annual plan (save 20%)"
    "Trial"))

(defn plan-change [s team-data]
  (let [current-plan (::payments-plan s)]
    [:div.plan-change
      [:button.mlb-reset.plans-dropdown-bt
        {:on-click #(reset! (::show-plans-dropdown s) true)}
        (plan-description @current-plan)]
      (when @(::show-plans-dropdown s)
        (dropdown-list {:items [{:value "team-monthly"
                                 :label (plan-description "team-monthly")}
                                {:value "team-annual"
                                 :label (plan-description "team-annual")}]
                        :value @current-plan
                        :on-blur #(reset! (::show-plans-dropdown s) false)
                        :on-change (fn [selected-item]
                                     (reset! (::show-plans-dropdown s) false)
                                     (reset! current-plan (:value selected-item)))}))
      [:div.plan-change-description
        (str
         "For your team of 25 people, your plan will cost $1,200 annually "
         "(25 people x $4 x 12 months). An annual plan saves you $300 per year.")]
      [:div.plan-change-title
        "Due today: $1.200,00"]
      [:button.mlb-reset.payment-info-bt
        "Add payment information"]
      ; (comment
       [:div.plan-change-separator]
       [:div.plan-change-details
         "Have a team of 250+"
         [:a.chat-with-us
           {:class "intercom-chat-link"
            :href "mailto:zcwtlybw@carrot-test-28eb3360a1a3.intercom-mail.com"}
           "Chat with us"]];)
  ]))

(rum/defcs payments-settings-modal <
  ;; Mixins
  rum/reactive
  (drv/drv :team-data)
  ui-mixins/refresh-tooltips-mixin
  ;; Locals
  (rum/local :summary ::payments-tab)
  (rum/local "free" ::payments-plan)
  (rum/local false ::show-plans-dropdown)
  [s {:keys [org-data]}]
  (let [team-data (drv/react s :team-data)
        payments-tab (::payments-tab s)
        is-change-tab? (= @payments-tab :change)]
    [:div.payments-settings-modal
      [:button.mlb-reset.modal-close-bt
        {:on-click #(nav-actions/close-all-panels)}]
      [:div.payments-settings-modal-container
        [:div.payments-settings-header
          [:div.payments-settings-header-title
            (if is-change-tab?
             "Change plan"
             "Billing")]
          (when-not is-change-tab?
            [:button.mlb-reset.save-bt
              {:on-click #(reset! payments-tab :change)}
              "Change plan"])
          [:button.mlb-reset.cancel-bt
            {:on-click #(if is-change-tab?
                          (reset! payments-tab :summary)
                          (nav-actions/show-org-settings nil))}
            "Back"]]
        [:div.payments-settings-body
          (if is-change-tab?
            (plan-change s team-data)
            (plan-summary s team-data))]]]))