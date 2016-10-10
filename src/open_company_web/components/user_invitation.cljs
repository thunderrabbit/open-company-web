(ns open-company-web.components.user-invitation
  (:require [rum.core :as rum]
            [open-company-web.dispatcher :as dis]
            [open-company-web.lib.utils :as utils]
            [open-company-web.components.ui.small-loading :refer (small-loading)]))

(defn user-invitation-action [invitation action & [payload]]
  (.tooltip (js/$ "[data-toggle=\"tooltip\"]") "hide")
  (dis/dispatch! [:user-invitation-action invitation action payload]))

(rum/defc invite-row
  [invitation show-email?]
  (let [user-links (:links invitation)]
    [:tr
      [:td [:div.value (:email invitation)]]
      (when show-email?
        [:td (when-not (clojure.string/blank? (:real-name invitation)) [:div.value (:real-name invitation)])])
      [:td [:div (clojure.string/upper-case (:status invitation))]]
      [:td
        (cond
          (:loading invitation)
          [:div
            (small-loading)]
          (= (clojure.string/lower-case (:status invitation)) "pending")
          [:div
            [:button.btn-reset.invite-row-action
              {:data-placement "top"
               :data-toggle "tooltip"
               :data-container "body"
               :title "RESEND INVITE"
               :on-click #(let [company-data (dis/company-data)]
                            (user-invitation-action
                              invitation
                              "invite"
                              {:email (:email invitation)
                               :company-name (:name company-data)
                               :logo (or (:logo company-data) "")}))}
              [:i.fa.fa-share]]
            [:button.btn-reset.invite-row-action
              {:data-placement "top"
               :data-toggle "tooltip"
               :data-container "body"
               :title "CANCEL INVITE"
               :on-click #(user-invitation-action invitation "delete")}
              [:i.fa.fa-times]]]
          (= (clojure.string/lower-case (:status invitation)) "active")
          [:div
            [:button.btn-reset.invite-row-action
              {:data-placement "top"
               :data-toggle "tooltip"
               :data-container "body"
               :title "REMOVE USER"
               :on-click #(user-invitation-action invitation "delete")}
              [:i.fa.fa-trash-o]]]
          :else
          [:div])]]))

(rum/defc user-invitation < {:did-mount (fn [s]
                                        (when-not (utils/is-test-env?)
                                          (.tooltip (js/$ "[data-toggle=\"tooltip\"]")))
                                        s)}
  [invitations]
  (let [show-name? (some #(not (clojure.string/blank? (:real-name %))) invitations)]
    [:div.my3.um-invitations-box.container.col-12.group
      [:table.table
        [:thead
          [:tr
            [:th "EMAIL"]
            (when show-name?
              [:th "NAME"])
            [:th "STATUS"]
            [:th "ACTIONS"]]]
        [:tbody
          (for [invitation (filter #(contains? % :status) invitations)]
            (rum/with-key (invite-row invitation show-name?) (str "invitation-tr-" (:href (utils/link-for (:links invitation) "self")))))]]]))