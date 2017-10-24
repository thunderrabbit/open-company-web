(ns oc.web.components.activity-modal
  (:require [rum.core :as rum]
            [dommy.core :as dommy :refer-macros (sel1)]
            [org.martinklepsch.derivatives :as drv]
            [cuerdas.core :as string]
            [goog.events :as events]
            [goog.events.EventType :as EventType]
            [oc.web.lib.jwt :as jwt]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.cookies :as cook]
            [oc.web.components.ui.mixins :as mixins]
            [oc.web.components.ui.activity-move :refer (activity-move)]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]
            [oc.web.components.ui.carrot-close-bt :refer (carrot-close-bt)]
            [oc.web.components.ui.activity-attachments :refer (activity-attachments)]
            [oc.web.components.reactions :refer (reactions)]
            [oc.web.components.comments :refer (comments)]))

(defn dismiss-modal [s board-filters]
  (let [org (router/current-org-slug)
        board (router/current-board-slug)
        current-board-filters @(drv/get-ref s :board-filters)]
    (router/nav!
      (if (string? board-filters)
        (oc-urls/board-filter-by-topic org board board-filters)
        (if (:from-all-posts @router/path)
          (oc-urls/all-posts org)
          (if (string? current-board-filters)
            (oc-urls/board-filter-by-topic org board current-board-filters)
            (if (= current-board-filters :by-topic)
              (oc-urls/board-sort-by-topic org board)
              (oc-urls/board org board))))))))

(defn close-clicked [s & [board-filters]]
  (when-not (:from-all-posts @router/path)
    ;; Make sure the seen-at is not reset when navigating back to the board so NEW is still visible
    (dis/dispatch! [:input [:no-reset-seen-at] true]))
  (reset! (::dismiss s) true)
  (utils/after 180 #(dismiss-modal s board-filters)))

(defn delete-clicked [e activity-data]
  (let [alert-data {:icon "/img/ML/trash.svg"
                    :action "delete-entry"
                    :message (str "Delete this update?")
                    :link-button-title "No"
                    :link-button-cb #(dis/dispatch! [:alert-modal-hide])
                    :solid-button-title "Yes"
                    :solid-button-cb #(do
                                       (let [org-slug (router/current-org-slug)
                                             board-slug (router/current-board-slug)
                                             last-filter (keyword (cook/get-cookie (router/last-board-filter-cookie org-slug board-slug)))]
                                         (if (= last-filter :by-topic)
                                           (router/nav! (oc-urls/board-sort-by-topic))
                                           (router/nav! (oc-urls/board))))
                                       (dis/dispatch! [:activity-delete activity-data])
                                       (dis/dispatch! [:alert-modal-hide]))
                    }]
    (dis/dispatch! [:alert-modal-show alert-data])))

(rum/defcs activity-modal < rum/reactive
                            ;; Derivatives
                            (drv/drv :activity-modal-fade-in)
                            (drv/drv :org-data)
                            (drv/drv :board-filters)
                            ;; Locals
                            (rum/local false ::dismiss)
                            (rum/local false ::animate)
                            (rum/local false ::showing-dropdown)
                            (rum/local nil ::window-resize-listener)
                            (rum/local nil ::esc-key-listener)
                            (rum/local false ::move-activity)
                            (rum/local 330 ::activity-modal-height)
                            (rum/local false ::share-dropdown)
                            (rum/local nil ::window-click)
                            ;; Mixins
                            mixins/no-scroll-mixin
                            mixins/first-render-mixin

                            {:before-render (fn [s]
                                              (when (and (not @(::animate s))
                                                       (= @(drv/get-ref s :activity-modal-fade-in) (:uuid (first (:rum/args s)))))
                                                (reset! (::animate s) true))
                                              (when-let [activity-modal (sel1 [:div.activity-modal])]
                                                (when (not= @(::activity-modal-height s) (.-clientHeight activity-modal))
                                                  (reset! (::activity-modal-height s) (.-clientHeight activity-modal))))
                                              s)
                             :will-mount (fn [s]
                                           (reset! (::esc-key-listener s)
                                            (events/listen js/window EventType/KEYDOWN #(when (= (.-key %) "Escape") (close-clicked s))))
                                           s)
                             :did-mount (fn [s]
                                          (reset! (::window-click s)
                                           (events/listen js/window EventType/CLICK (fn [e]
                                                                                      (when (and (not (utils/event-inside? e (sel1 [:div.activity-modal :div.more-dropdown])))
                                                                                                 (not (utils/event-inside? e (sel1 [:div.activity-modal :div.activity-move]))))
                                                                                        (reset! (::showing-dropdown s) false))
                                                                                      (when (not (utils/event-inside? e (sel1 [:div.activity-modal :div.activity-modal-share])))
                                                                                        (reset! (::share-dropdown s) false)))))
                                          s)
                            :will-unmount (fn [s]
                                            ;; Remove window resize listener
                                            (when @(::window-resize-listener s)
                                              (events/unlistenByKey @(::window-resize-listener s))
                                              (reset! (::window-resize-listener s) nil))
                                            (when @(::window-click s)
                                              (events/unlistenByKey @(::window-click s))
                                              (reset! (::window-click s) nil))
                                            (when @(::esc-key-listener s)
                                              (events/unlistenByKey @(::esc-key-listener s))
                                              (reset! (::esc-key-listener s) nil))
                                            s)}
  [s activity-data]
  (let [show-comments? (utils/link-for (:links activity-data) "comments")
        fixed-activity-modal-height (max @(::activity-modal-height s) 330)
        wh (.-innerHeight js/window)]
    [:div.activity-modal-container
      {:class (utils/class-set {:will-appear (or @(::dismiss s) (and @(::animate s) (not @(:first-render-done s))))
                                :appear (and (not @(::dismiss s)) @(:first-render-done s))
                                :no-comments (not show-comments?)})
       :on-click #(when-not (utils/event-inside? % (sel1 [:div.activity-modal]))
                    (close-clicked s))}
      [:div.modal-wrapper
        {:style {:margin-top (str (max 0 (/ (- wh fixed-activity-modal-height) 2)) "px")}}
        [:button.carrot-modal-close.mlb-reset
          {:on-click #(close-clicked s)}]
        [:div.activity-modal.group
          {:class (str "activity-modal-" (:uuid activity-data))}
          [:div.activity-modal-header.group
            [:div.activity-modal-header-left
              (user-avatar-image (first (:author activity-data)))
              [:div.name (:name (first (:author activity-data)))]
              [:div.time-since
                [:time
                  {:date-time (:created-at activity-data)
                   :data-toggle "tooltip"
                   :data-placement "top"
                   :title (utils/activity-date-tooltip activity-data)}
                  (utils/time-since (:created-at activity-data))]]]
            [:div.activity-modal-header-right
              (when (or (utils/link-for (:links activity-data) "partial-update")
                        (utils/link-for (:links activity-data) "delete"))
                (let [all-boards (filter #(not= (:slug %) "drafts") (:boards (drv/react s :org-data)))
                      same-type-boards (filter #(= (:type %) (:type activity-data)) all-boards)]
                  [:div.more-dropdown
                    [:button.mlb-reset.activity-modal-more.dropdown-toggle
                      {:type "button"
                       :on-click (fn [e]
                                   (utils/remove-tooltips)
                                   (reset! (::showing-dropdown s) (not @(::showing-dropdown s)))
                                   (reset! (::move-activity s) false))
                       :data-toggle "tooltip"
                       :data-placement "right"
                       :data-container "body"
                       :title "More"}]
                    (when @(::showing-dropdown s)
                      [:div.activity-modal-dropdown-menu
                        [:div.triangle]
                        [:ul.activity-modal-more-menu
                          (when (utils/link-for (:links activity-data) "partial-update")
                            [:li
                              {:on-click #(do
                                            (reset! (::showing-dropdown s) false)
                                            (dis/dispatch! [:entry-edit activity-data]))}
                              "Edit"])
                          (when (utils/link-for (:links activity-data) "partial-update")
                            [:li
                              {:on-click #(do
                                           (reset! (::showing-dropdown s) false)
                                           (reset! (::move-activity s) true))}
                              "Move"])
                          (when (utils/link-for (:links activity-data) "delete")
                            [:li
                              {:on-click #(do
                                            (reset! (::showing-dropdown s) false)
                                            (delete-clicked % activity-data))}
                              "Delete"])]])
                    (when @(::move-activity s)
                      (activity-move {:activity-data activity-data :boards-list same-type-boards :dismiss-cb #(reset! (::move-activity s) false) :on-change #(close-clicked s nil)}))]))
              (activity-attachments activity-data)
              (when (:topic-slug activity-data)
                (let [topic-name (or (:topic-name activity-data) (string/upper (:topic-slug activity-data)))]
                  [:div.activity-tag
                    {:on-click #(close-clicked s (:topic-slug activity-data))}
                    topic-name]))]]
          [:div.activity-modal-columns
            ;; Left column
            [:div.activity-left-column
              [:div.activity-left-column-content
                [:div.activity-modal-content
                  {:on-click #(when (utils/link-for (:links activity-data) "partial-update")
                                (dis/dispatch! [:entry-edit activity-data]))}
                  [:div.activity-modal-content-headline
                    {:dangerouslySetInnerHTML (utils/emojify (:headline activity-data))}]
                  [:div.activity-modal-content-body
                    {:dangerouslySetInnerHTML (utils/emojify (:body activity-data))
                     :class (when (empty? (:headline activity-data)) "no-headline")}]]
                [:div.activity-modal-footer.group
                  {:class (when (and @(:first-render-done s)
                                     (= wh (.-clientHeight (sel1 [:div.activity-modal])))) "scrolling-content")}
                  (reactions activity-data)
                  [:div.activity-modal-footer-right
                    [:div.activity-modal-share
                      (when @(::share-dropdown s)
                        [:div.share-dropdown
                          [:div.triangle]
                          [:ul.share-dropdown-menu
                            (when (utils/link-for (:links activity-data) "share")
                              [:li.share-dropdown-item
                                {:on-click (fn [e]
                                             (reset! (::share-dropdown s) false)
                                             ; open the activity-share-modal component
                                             (dis/dispatch! [:activity-share-show :link activity-data]))}
                                "Share Link"])
                            (when (utils/link-for (:links activity-data) "share")
                              [:li.share-dropdown-item
                                {:on-click (fn [e]
                                             (reset! (::share-dropdown s) false)
                                             ; open the activity-share-modal component
                                             (dis/dispatch! [:activity-share-show :email activity-data]))}
                                "Share Email"])
                            (when (and (utils/link-for (:links activity-data) "share")
                                       (jwt/team-has-bot? (:team-id (dis/org-data))))
                              [:li.share-dropdown-item
                                {:on-click (fn [e]
                                             (reset! (::share-dropdown s) false)
                                             ; open the activity-share-modal component
                                             (dis/dispatch! [:activity-share-show :slack activity-data]))}
                                "Share Slack"])]])
                      [:button.mlb-reset.share-button
                        {:on-click #(do
                                     (reset! (::share-dropdown s) (not @(::share-dropdown s))))}
                        "Share"]]]]]]
            ;; Right column
            (when show-comments?
              [:div.activity-right-column
                [:div.activity-right-column-content
                  (comments activity-data)]])]]]]))