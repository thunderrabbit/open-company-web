(ns oc.web.components.entry-card
  (:require [rum.core :as rum]
            [cuerdas.core :as s]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.oc-colors :refer (get-color-by-kw)]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]
            [oc.web.components.ui.interactions-summary :refer (interactions-summary)]
            [goog.object :as gobj]))

(rum/defc entry-card-empty
  [read-only?]
  [:div.entry-card.empty-state.group
    [:div.empty-state-content
      [:img {:src (utils/cdn "/img/ML/entry_empty_state.svg")}]
      [:div.entry-card-title
        "This topic’s a little sparse. "
        (when-not read-only?
          [:button.mlb-reset
            {:on-click #(dis/dispatch! [:entry-edit {}])}
            "Add an update?"])]]])

(defn delete-clicked [e entry-data]
  (utils/event-stop e)
  (let [alert-data {:icon "/img/ML/trash.svg"
                    :message "Delete this entry?"
                    :first-button-title "No"
                    :first-button-cb #(dis/dispatch! [:alert-modal-hide])
                    :second-button-title "Yes"
                    :second-button-cb #(do
                                        (dis/dispatch! [:entry-delete entry-data])
                                        (dis/dispatch! [:alert-modal-hide]))
                    }]
    (dis/dispatch! [:alert-modal-show alert-data])))

(defn truncate-body [body-sel]
  (.dotdotdot (js/$ body-sel)
   #js {:height 72
        :wrap "word"
        :watch true
        :ellipsis "... "
        :after "a.read-more"}))

(defn get-first-body-image [body]
  (.get (.find (js/$ (str "<div>" body "</div>")) "img:not(.emojione)") 0))

(rum/defcs entry-card < rum/static
                        (rum/local false ::hovering-card)
                        (rum/local false ::showing-dropdown)
                        (rum/local false ::truncated)
                        (rum/local nil ::first-body-image)
                        {:after-render (fn [s]
                                         (let [entry-data (first (:rum/args s))
                                               body-sel (str "div.entry-card-" (:uuid entry-data) " div.entry-card-body")
                                               body-a-sel (str body-sel " a")
                                               read-more-sel (str body-a-sel ".read-more")]
                                           ; Prevent body links in FoC
                                           (.click (js/$ body-a-sel) #(.preventDefault %))
                                           ; Prevent read more link to change directly the url
                                           (.click (js/$ read-more-sel) #(.preventDefault %))
                                           ; Truncate body text with dotdotdot
                                           (when-not @(::truncated s)
                                             (truncate-body body-sel)
                                             (utils/after 10 #(truncate-body body-sel))
                                             (reset! (::truncated s) true)))
                                         s)
                         :will-mount (fn [s]
                                       (let [entry-data (first (:rum/args s))]
                                         (reset! (::first-body-image s) (get-first-body-image (:body entry-data))))
                                       s)
                         :did-remount (fn [o s]
                                        (let [old-entry-data (first (:rum/args o))
                                              new-entry-data (first (:rum/args s))]
                                          (when (not= (:body old-entry-data) (:body new-entry-data))
                                            (reset! (::truncated s) false)
                                            (reset! (::first-body-image s) (get-first-body-image (:body new-entry-data)))))
                                        s)
                         :did-mount (fn [s]
                                      (let [entry-data (first (:rum/args s))]
                                        (.on (js/$ (str "div.entry-card-" (:uuid entry-data)))
                                         "show.bs.dropdown"
                                         (fn [e]
                                           (reset! (::showing-dropdown s) true)))
                                        (.on (js/$ (str "div.entry-card-" (:uuid entry-data)))
                                         "hidden.bs.dropdown"
                                         (fn [e]
                                           (reset! (::showing-dropdown s) false))))
                                      s)}
  [s entry-data has-headline has-body]
  [:div.entry-card
    {:class (str "entry-card-" (:uuid entry-data))
     :on-click #(dis/dispatch! [:entry-modal-fade-in (:uuid entry-data)])
     :on-mouse-over #(reset! (::hovering-card s) true)
     :on-mouse-leave #(reset! (::hovering-card s) false)}
    ; Card header
    [:div.entry-card-head.group
      ; Card author
      [:div.entry-card-head-author
        (user-avatar-image (first (:author entry-data)))
        [:div.name (:name (first (:author entry-data)))]
        [:div.time-since
          [:time
            {:date-time (:updated-at entry-data)
             :data-toggle "tooltip"
             :data-placement "top"
             :data-container "body"
             :title (let [js-date (utils/js-date (:updated-at entry-data))] (str (.toDateString js-date) " at " (.toLocaleTimeString js-date)))}
            (utils/time-since (:updated-at entry-data))]]]
      ; Card labels
      [:div.entry-card-head-right
        (when (:topic-slug entry-data)
          (let [topic-name (or (:topic-name entry-data) (s/upper (:topic-slug entry-data)))]
            [:div.topic-tag
              {:on-click #(do
                            (utils/event-stop %)
                            (router/nav! (oc-urls/board-filter-by-topic (:topic-slug entry-data))))}
              topic-name]))]]
    [:div.entry-card-content.group
      [:div.entry-card-headline
        {:dangerouslySetInnerHTML (utils/emojify (:headline entry-data))
         :class (when has-headline "has-headline")}]
      (let [body-without-images (utils/strip-img-tags (:body entry-data))
            emojied-body (utils/emojify (str body-without-images "<a class=\"read-more\" href=\"" (oc-urls/entry (:uuid entry-data)) "\">Read more</a>"))]
        [:div.entry-card-body
          {:dangerouslySetInnerHTML emojied-body
           :class (utils/class-set {:has-body has-body
                                    :has-media-preview @(::first-body-image s)})}])
      (when @(::first-body-image s)
        [:div.entry-card-media-preview
          [:span.helper]
          [:img
            {:src (.-src @(::first-body-image s))}]])]
    [:div.entry-card-footer.group
      (interactions-summary entry-data)
      [:div.more-button.dropdown
        [:button.mlb-reset.more-ellipsis.dropdown-toggle
          {:type "button"
           :class (utils/class-set {:hidden (and (not @(::hovering-card s)) (not @(::showing-dropdown s)))})
           :id (str "entry-card-more-" (router/current-board-slug) "-" (:uuid entry-data))
           :on-click #(utils/event-stop %)
           :title "More"
           :data-toggle "dropdown"
           :aria-haspopup true
           :aria-expanded false}]
        [:div.dropdown-menu
          {:aria-labelledby (str "entry-card-more-" (router/current-board-slug) "-" (:uuid entry-data))}
          [:div.triangle]
          [:ul.entry-card-more-menu
            [:li
              {:on-click (fn [e]
                           (utils/event-stop e)
                           (dis/dispatch! [:entry-edit entry-data]))}
              "Edit"]
            [:li
              {:on-click #(delete-clicked % entry-data)}
              "Delete"]]]]]])