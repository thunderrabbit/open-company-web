(ns oc.web.components.navigation-sidebar
  (:require [rum.core :as rum]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.lib.jwt :as jwt]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.lib.chat :as chat]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.cookies :as cook]
            [oc.web.mixins.ui :as ui-mixins]
            [oc.web.actions.nux :as nux-actions]
            [oc.web.components.ui.menu :as menu]
            [oc.web.lib.responsive :as responsive]
            [oc.web.actions.nav-sidebar :as nav-actions]
            [oc.web.components.ui.orgs-dropdown :refer (orgs-dropdown)]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]
            [oc.web.components.ui.qsg-breadcrumb :refer (qsg-breadcrumb)]
            [goog.events :as events]
            [taoensso.timbre :as timbre]
            [goog.events.EventType :as EventType]))

(defn sort-boards [boards]
  (vec (sort-by :name boards)))

(def sidebar-top-margin 90)

(defn save-content-height [s]
  (when-let [navigation-sidebar-content (rum/ref-node s "left-navigation-sidebar-content")]
    (let [height (+ (.height (js/$ navigation-sidebar-content)) 32)]
      (when (not= height @(::content-height s))
        (reset! (::content-height s) height))))
  (when-let [navigation-sidebar-footer (rum/ref-node s "left-navigation-sidebar-footer")]
    (let [footer-height (+ (.height (js/$ navigation-sidebar-footer)) 8)]
      (when (not= footer-height @(::footer-height s))
        (reset! (::footer-height s) footer-height)))))

(defn filter-board [board-data]
  (let [self-link (utils/link-for (:links board-data) "self")]
    (and (not= (:slug board-data) utils/default-drafts-board-slug)
         (or (not (contains? self-link :count))
             (and (contains? self-link :count)
                  (pos? (:count self-link))))
         (or (not (contains? board-data :draft))
             (not (:draft board-data))))))

(defn filter-boards [all-boards]
  (filterv filter-board all-boards))

(defn save-window-size
  "Save the window height in the local state."
  [s]
  (reset! (::window-height s) (.-innerHeight js/window))
  (reset! (::window-width s) (.-innerWidth js/window)))

(rum/defcs navigation-sidebar < rum/reactive
                                ;; Derivatives
                                (drv/drv :qsg)
                                (drv/drv :org-data)
                                (drv/drv :board-data)
                                (drv/drv :show-section-add)
                                (drv/drv :change-cache-data)
                                (drv/drv :current-user-data)
                                (drv/drv :mobile-navigation-sidebar)
                                ;; Locals
                                (rum/local false ::content-height)
                                (rum/local false ::footer-height)
                                (rum/local nil ::window-height)
                                (rum/local nil ::window-width)
                                ;; Mixins
                                ui-mixins/first-render-mixin
                                (ui-mixins/render-on-resize save-window-size)

                                {:will-mount (fn [s]
                                  (save-window-size s)
                                  (save-content-height s)
                                  s)
                                 :before-render (fn [s]
                                  (nux-actions/check-nux)
                                  s)
                                 :did-mount (fn [s]
                                  (save-content-height s)
                                  (when-not (utils/is-test-env?)
                                    (.tooltip (js/$ "[data-toggle=\"tooltip\"]")))
                                  s)
                                 :will-update (fn [s]
                                  (save-content-height s)
                                  s)
                                 :did-update (fn [s]
                                  (when-not (utils/is-test-env?)
                                    (.tooltip (js/$ "[data-toggle=\"tooltip\"]")))
                                  s)}
  [s]
  (let [org-data (drv/react s :org-data)
        board-data (drv/react s :board-data)
        change-data (drv/react s :change-cache-data)
        current-user-data (drv/react s :current-user-data)
        mobile-navigation-sidebar (drv/react s :mobile-navigation-sidebar)
        left-navigation-sidebar-width (- responsive/left-navigation-sidebar-width 20)
        all-boards (:boards org-data)
        boards (filter-boards all-boards)
        sorted-boards (sort-boards boards)
        is-all-posts (= (router/current-board-slug) "all-posts")
        is-must-see (= (router/current-board-slug) "must-see")
        is-drafts-board (= (:slug board-data) utils/default-drafts-board-slug)
        create-link (utils/link-for (:links org-data) "create")
        show-boards (or create-link (pos? (count boards)))
        show-all-posts (and (jwt/user-is-part-of-the-team (:team-id org-data))
                            (utils/link-for (:links org-data) "activity"))
        drafts-board (first (filter #(= (:slug %) utils/default-drafts-board-slug) all-boards))
        drafts-link (utils/link-for (:links drafts-board) "self")
        org-slug (router/current-org-slug)
        is-admin-or-author? (utils/is-admin-or-author? org-data)
        show-invite-people (and org-slug
                                is-admin-or-author?)
        is-tall-enough? (or (not @(::content-height s))
                            (not @(::footer-height s))
                            (not (neg?
                             (- @(::window-height s) sidebar-top-margin @(::content-height s) @(::footer-height s)))))
        is-mobile? (responsive/is-tablet-or-mobile?)
        show-reminders? (utils/link-for (:links org-data) "reminders")
        qsg-data (drv/react s :qsg)
        showing-qsg (:visible qsg-data)]
    [:div.left-navigation-sidebar.group
      {:class (utils/class-set {:show-mobile-boards-menu mobile-navigation-sidebar
                                :navigation-sidebar-overflow (not is-tall-enough?)})
       :style {:left (when-not is-mobile?
                      (str (/ (- @(::window-width s) 952 (when showing-qsg 220)) 2) "px"))
               :overflow (when (= (:step qsg-data) :add-section-1)
                           "visible")}}
      [:div.mobile-header-container
        [:button.mlb-reset.mobile-header-close
          {:on-click #(dis/dispatch! [:input [:mobile-navigation-sidebar] false])}]
        (orgs-dropdown)
        [:button.btn-reset.mobile-menu.group
          {:on-click #(do
                       (when is-mobile?
                         (dis/dispatch! [:input [:user-settings] nil])
                         (dis/dispatch! [:input [:org-settings] nil]))
                       (dis/dispatch! [:input [:mobile-navigation-sidebar] false])
                       (menu/mobile-menu-toggle))}
          (user-avatar-image current-user-data)]]
      [:div.left-navigation-sidebar-content
        {:ref "left-navigation-sidebar-content"}
        ;; All posts
        (when show-all-posts
          [:a.all-posts.hover-item.group
            {:class (utils/class-set {:item-selected is-all-posts})
             :href (oc-urls/all-posts)
             :on-click #(nav-actions/nav-to-url! % (oc-urls/all-posts))}
            [:div.all-posts-icon
              {:class (when is-all-posts "selected")}]
            [:div.all-posts-label
              {:class (utils/class-set {:new (seq (apply concat (map :unseen (vals change-data))))})}
              "All posts"]])
        (when show-all-posts
           [:a.must-see.hover-item.group
            {:class (utils/class-set {:item-selected is-must-see
                                      :showing-drafts drafts-link})
              :href (oc-urls/must-see)
              :on-click #(nav-actions/nav-to-url! % (oc-urls/must-see))}
             [:div.must-see-icon
               {:class (when is-must-see "selected")}]
             [:div.must-see-label
               "Must see"]])
        (when drafts-link
          (let [board-url (oc-urls/board (:slug drafts-board))
                draft-posts (dis/draft-posts-data)
                draft-count (or (count draft-posts) (:count drafts-link))]
            [:a.drafts.hover-item.group
              {:class (when (and (not is-all-posts)
                                 (= (router/current-board-slug) (:slug drafts-board)))
                        "item-selected")
               :data-board (name (:slug drafts-board))
               :key (str "board-list-" (name (:slug drafts-board)))
               :href board-url
               :on-click #(nav-actions/nav-to-url! % board-url)}
              [:div.drafts-label.group
                "Drafts "
                (when (pos? draft-count)
                  [:span.count "(" draft-count ")"])]]))
        ;; Boards list
        (when show-boards
          [:div.left-navigation-sidebar-top.group
            {:class (when (drv/react s :show-section-add) "show-section-add")}
            ;; Boards header
            [:h3.left-navigation-sidebar-top-title.group
              [:span
                "SECTIONS"]
              (when create-link
                [:button.left-navigation-sidebar-top-title-button.btn-reset.qsg-add-section-1
                  {:on-click #(nav-actions/show-section-add)
                   :class (when (= (:step qsg-data) :add-section-1) "active")
                   :title "Create a new section"
                   :data-placement "top"
                   :data-toggle (when-not is-mobile? "tooltip")
                   :data-container "body"
                   :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"}
                  (when (= (:step qsg-data) :add-section-1)
                    (qsg-breadcrumb qsg-data))])]])
        (when show-boards
          [:div.left-navigation-sidebar-items.group
            (for [board sorted-boards
                  :let [board-url (oc-urls/board org-slug (:slug board))
                        is-current-board (= (router/current-board-slug) (:slug board))
                        board-change-data (get change-data (:uuid board))]]
              [:a.left-navigation-sidebar-item.hover-item
                {:class (utils/class-set {:item-selected (and (not is-all-posts)
                                                              is-current-board)})
                 :data-board (name (:slug board))
                 :key (str "board-list-" (name (:slug board)))
                 :href board-url
                 :on-click #(do
                              (nav-actions/nav-to-url! % board-url))}
                (when (= (:access board) "public")
                  [:div.public
                    {:class (when is-current-board "selected")}])
                (when (= (:access board) "private")
                  [:div.private
                    {:class (when is-current-board "selected")}])
                [:div.board-name.group
                  {:class (utils/class-set {:public-board (= (:access board) "public")
                                            :private-board (= (:access board) "private")
                                            :team-board (= (:access board) "team")})}
                  [:div.internal
                    {:class (utils/class-set {:new (seq (:unseen board-change-data))
                                              :has-icon (#{"public" "private"} (:access board))})
                     :key (str "board-list-" (name (:slug board)) "-internal")
                     :dangerouslySetInnerHTML (utils/emojify (or (:name board) (:slug board)))}]]])])]
      [:div.left-navigation-sidebar-footer
        {:ref "left-navigation-sidebar-footer"
         :class (utils/class-set {:push-to-bottom is-tall-enough?})}
        (when show-reminders?
          [:button.mlb-reset.bottom-nav-bt
            {:on-click #(do
                          (nav-actions/show-reminders)
                          (utils/after 500 utils/remove-tooltips))
             :title "Set reminders to update your team on time"
             :data-toggle (when-not is-mobile? "tooltip")
             :data-placement "top"
             :data-container "body"
             :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"}
            [:div.bottom-nav-icon.reminders-icon]
            [:span "Reminders"]])
        (when show-invite-people
          [:button.mlb-reset.bottom-nav-bt
            {:on-click #(do
                          (nav-actions/show-invite)
                          (utils/after 500 utils/remove-tooltips))
             :title "Invite teammates with email or Slack"
             :data-toggle (when-not is-mobile? "tooltip")
             :data-placement "top"
             :data-container "body"
             :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"}
            [:div.bottom-nav-icon.invite-people-icon]
            [:span "Invite team"]])
        [:button.mlb-reset.bottom-nav-bt
          {:on-click #(do
                        (chat/chat-click 42861)
                        (utils/after 500 utils/remove-tooltips))
           :title "Have a question for Carrot? Chat with us."
           :data-toggle (when-not is-mobile? "tooltip")
           :data-placement "top"
           :data-container "body"
           :data-delay "{\"show\":\"500\", \"hide\":\"0\"}"}
          [:div.bottom-nav-icon.support-icon]
          [:span "Get support"]]]]))