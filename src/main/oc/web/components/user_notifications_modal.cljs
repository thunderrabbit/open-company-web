(ns oc.web.components.user-notifications-modal
  (:require [rum.core :as rum]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.lib.jwt :as jwt]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.local-settings :as ls]
            [oc.web.lib.responsive :as responsive]
            [oc.web.mixins.ui :as ui-mixins]
            [oc.web.actions.user :as user-actions]
            [oc.web.utils.user :as user-utils]
            [oc.web.actions.nav-sidebar :as nav-actions]
            [oc.web.components.ui.alert-modal :as alert-modal]
            [oc.web.components.ui.carrot-checkbox :refer (carrot-checkbox)]))

(defn change! [k v]
  (let [ks (if (keyword? k) [k] k)]
    (dis/dispatch! [:input (concat [:edit-user-profile] ks) v])
    (dis/dispatch! [:input [:edit-user-profile :has-changes] true])))

(defn save-clicked [s]
  (when (compare-and-set! (::loading s) false true)
    (let [edit-user-profile @(drv/get-ref s :edit-user-profile)
          current-user-data @(drv/get-ref s :current-user-data)]
      (user-actions/user-profile-save current-user-data edit-user-profile))))

(defn close-clicked [editing-user-data dismiss-action]
  (if (:has-changes editing-user-data)
    (let [alert-data {:icon "/img/ML/trash.svg"
                      :action "user-profile-unsaved-edits"
                      :message "Leave without saving your changes?"
                      :link-button-title "Stay"
                      :link-button-cb #(alert-modal/hide-alert)
                      :solid-button-style :red
                      :solid-button-title "Lose changes"
                      :solid-button-cb (fn []
                                        (alert-modal/hide-alert)
                                        (dismiss-action))}]
      (alert-modal/show-alert alert-data))
    (dismiss-action)))

(defn- digest-time-label [t]
  (let [time-string (name t)
        minutes (subs time-string (- (count time-string) 2) (count time-string))
        hours* (.parseInt js/window (subs time-string 0 (- (count time-string) 2)) 10)
        hours (if (> hours* 12) (- hours* 12) hours*)]
    (str  hours ":" minutes (if (> hours* 11) " PM" " AM"))))

(def premium-times-tooltip "Premium accounts can change when digests are sent each day.")

(rum/defcs user-notifications-modal <
  rum/reactive
  (drv/drv :org-data)
  (drv/drv :team-roster)
  (drv/drv :edit-user-profile)
  (drv/drv :current-user-data)
  ;; Locals
  (rum/local false ::loading)
  (rum/local false ::show-success)
  ;; Mixins
  ui-mixins/refresh-tooltips-mixin
  {:will-mount (fn [s]
   (user-actions/get-user nil)
   s)
  :did-remount (fn [_ new-state]
   (let [user-data (:user-data @(drv/get-ref new-state :edit-user-profile))]
     (when (and @(::loading new-state)
                (not (:has-changes user-data)))
       (reset! (::show-success new-state) true)
       (reset! (::loading new-state) false)
       (utils/after 2500 (fn [] (reset! (::show-success new-state) false)))))
   new-state)}
  [s]
  (let [org-data (drv/react s :org-data)
        _current-user-data (drv/react s :current-user-data)
        user-profile-drv (drv/react s :edit-user-profile)
        editing-user-data (:user-data user-profile-drv)
        bots-data (jwt/team-has-bot? (:team-id org-data))
        team-roster (drv/react s :team-roster)
        slack-enabled? (user-utils/user-has-slack-with-bot? editing-user-data bots-data team-roster)
        digest-delivery (:digest-delivery editing-user-data)
        team-digest-delivery (some #(when (= (:team-id %) (:team-id org-data))
                                      %)
                                   digest-delivery)
        digest-times-set (set (:digest-times team-digest-delivery))
        is-mobile? (responsive/is-mobile-size?)]
    [:div.user-notifications-modal-container
      [:button.mlb-reset.modal-close-bt
        {:on-click #(close-clicked editing-user-data nav-actions/close-all-panels)}]
      [:div.user-notifications-modal
        [:div.user-notifications-header
          [:div.user-notifications-header-title
            "Notification settings"]
          [:button.mlb-reset.save-bt
            {:on-click #(if (:has-changes editing-user-data)
                          (save-clicked s)
                          (nav-actions/show-user-settings nil))
             :class (when (or (not (:has-changes editing-user-data))
                              @(::show-success s)
                              @(::loading s))
                      "disabled")}
           (if (:loading editing-user-data)
             "Saving..."
             (if @(::show-success s)
               "Saved!"
               "Save"))]
          [:button.mlb-reset.cancel-bt
            {:on-click (fn [_] (close-clicked editing-user-data #(nav-actions/show-user-settings nil)))}
            "Back"]]
        [:div.user-notifications-body
          ; [:div.user-profile-modal-fields
          ;   [:div.field-label "Daily digest"]
          ;   [:select.field-value.oc-input
          ;     {:value (:digest-medium editing-user-data)
          ;      :disabled (and (not slack-enabled?)
          ;                     (not= (:digest-medium editing-user-data) "slack"))
          ;      :on-change #(change! :digest-medium (.. % -target -value))}
          ;     [:option
          ;       {:value "email"}
          ;       "Via email"]
          ;     (when (or slack-enabled?
          ;               (= (:digest-medium editing-user-data) "slack"))
          ;       [:option
          ;         {:value "slack"
          ;          :disabled (not slack-enabled?)}
          ;         "Via Slack"])]
          ;   [:div.field-description
          ;     (str ls/product-name " will curate all the content you should see and deliver it to you directly each morning.")]]
          [:div.user-profile-modal-fields
            [:div.field-label
              "The latest updates will be sent to you in a digest at your preferred times."]
            [:div.field-value-group.digest-times
              (for [t ls/premium-digest-times
                    :let [selected? (digest-times-set t)
                          premium-locked? (and (not (:premium? org-data))
                                               (not ((set ls/digest-times) t)))
                          change-cb (fn [_]
                                      (let [times (if selected?
                                                    (disj digest-times-set t)
                                                    (conj digest-times-set t))
                                            next-digest-delivery (map #(if (= (:team-id %) (:team-id org-data))
                                                                         {:digest-times times
                                                                          ;; add a flag so the server can distinguish what
                                                                          ;; is changing to filter out not-allowed times
                                                                          ;; if the team has no premium plan
                                                                          :changed true
                                                                          :team-id (:team-id org-data)}
                                                                         %)
                                                                       digest-delivery)]
                                        (change! [:digest-delivery] next-digest-delivery)))]]
                [:div.field-value.group
                  {:key (name t)
                   :class (when premium-locked? "premium-lock")
                   :title (when premium-locked?
                            (str premium-times-tooltip " Click for details."))
                   :data-toggle (when-not is-mobile? "tooltip")
                   :data-placement "top"
                   :data-container "body"
                   :on-click (if premium-locked?
                               #(nav-actions/toggle-premium-picker! premium-times-tooltip)
                               change-cb)}
                  (carrot-checkbox {:selected selected?
                                    :disabled premium-locked?})
                  [:span.digest-time
                    {:class (when premium-locked? "disabled")}
                    (digest-time-label t)]])]
            [:div.field-description
              "Your timezone is "
              [:a
                {:href "?user-settings=profile"
                 :on-click #(do
                              (utils/event-stop %)
                              (nav-actions/show-user-settings :profile))
                 :data-toggle (when-not is-mobile? "tooltip")
                 :data-placement "top"
                 :data-container "body"
                 :title "Change your timezone"}
                (user-utils/readable-tz (:timezone editing-user-data))]
              "."]]
          [:div.user-profile-modal-fields
            [:div.field-label "Comments and mentions:"]
            [:select.field-value.oc-input
              {:value (:notification-medium editing-user-data)
               :on-change #(change! :notification-medium (.. % -target -value))}
              [:option
                {:value "email"}
                "Via email"]
              (when (or slack-enabled?
                        (= (:notification-medium editing-user-data) "slack"))
                [:option
                  {:value "slack"
                   :disabled (not slack-enabled?)}
                  "Via Slack"])
              [:option
                {:value "in-app"}
                "In-app only"]]
            [:div.field-description
              "Notifications are sent in real-time if someone comments on a post you are watching or mentions you."]]
          (when ls/reminders-enabled?
            [:div.user-profile-modal-fields
              [:div.field-label "Recurring update reminders"]
              [:select.field-value.oc-input
                {:value (:reminder-medium editing-user-data)
                 :on-change #(change! :reminder-medium (.. % -target -value))}
                [:option
                  {:value "email"}
                  "Via email"]
                (when (or slack-enabled?
                          (= (:reminder-medium editing-user-data) "slack"))
                  [:option
                    {:value "slack"
                     :disabled (not slack-enabled?)}
                    "Via Slack"])
                [:option
                  {:value "in-app"}
                  "In-app only"]]])]]]))