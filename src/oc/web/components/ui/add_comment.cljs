(ns oc.web.components.ui.add-comment
  (:require [rum.core :as rum]
            [goog.events :as events]
            [dommy.core :refer-macros (sel1)]
            [goog.events.EventType :as EventType]
            [org.martinklepsch.derivatives :as drv]
            [oc.web.lib.jwt :as jwt]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [oc.web.utils.comment :as cu]
            [oc.web.lib.responsive :as responsive]
            [oc.web.utils.mention :as mention-utils]
            [oc.web.mixins.mention :as mention-mixins]
            [oc.web.actions.comment :as comment-actions]
            [oc.web.mixins.ui :as ui-mixins]
            [oc.web.utils.medium-editor-media :as me-media-utils]
            [oc.web.components.ui.emoji-picker :refer (emoji-picker)]
            [oc.web.components.ui.giphy-picker :refer (giphy-picker)]
            [oc.web.components.ui.user-avatar :refer (user-avatar-image)]
            [oc.web.components.ui.media-video-modal :refer (media-video-modal)]))

;; Add commnet handling

(defn enable-add-comment? [s]
  (when-let [add-comment-div (rum/ref-node s "editor-node")]
    (let [activity-data (first (:rum/args s))
          parent-comment-uuid (second (:rum/args s))
          comment-text (.-innerHTML add-comment-div)
          next-add-bt-disabled (or (nil? comment-text) (not (seq comment-text)))]
      (comment-actions/add-comment-change activity-data parent-comment-uuid comment-text)
      (when (not= next-add-bt-disabled @(::add-button-disabled s))
        (reset! (::add-button-disabled s) next-add-bt-disabled)))))

(defn focus-add-comment [s]
  (enable-add-comment? s)
  (let [activity-data (first (:rum/args s))
        parent-comment-uuid (second (:rum/args s))]
    (if parent-comment-uuid
      (comment-actions/add-comment-focus parent-comment-uuid)
      (comment-actions/add-comment-focus (:uuid activity-data)))))

(defn disable-add-comment-if-needed [s]
  (when-let [add-comment-node (rum/ref-node s "editor-node")]
    (enable-add-comment? s)
    (when (not (seq (.-innerHTML add-comment-node)))
      (comment-actions/add-comment-blur))))

(defn- send-clicked [s parent-comment-uuid]
  (let [add-comment-div (rum/ref-node s "editor-node")
        comment-body (cu/add-comment-content add-comment-div true)
        activity-data (first (:rum/args s))]
    (set! (.-innerHTML add-comment-div) "")
    (comment-actions/add-comment activity-data comment-body parent-comment-uuid)))

(def me-options
  {:media-config ["gif" "photo" "video"]
   :placeholder "Leave a new comment…"
   :use-inline-media-picker true
   :media-picker-initially-visible false})

(defn add-comment-did-change [s]
  (reset! (::did-change s) true)
  (reset! (::show-post-button s) true)
  (enable-add-comment? s))

(defn- should-focus-field? [s]
  (let [activity-data (first (:rum/args s))
        parent-comment-uuid (second (:rum/args s))
        add-comment-focus @(drv/get-ref s :add-comment-focus)]
    (or (and (= (:uuid activity-data) add-comment-focus)
             (not parent-comment-uuid))
        (and (seq parent-comment-uuid)
             (= parent-comment-uuid add-comment-focus)))))

(rum/defcs add-comment < rum/reactive
                         ;; Locals
                         (rum/local nil :me/editor)
                         (rum/local nil :me/media-picker-ext)
                         (rum/local false :me/media-photo)
                         (rum/local false :me/media-video)
                         (rum/local false :me/media-attachment)
                         (rum/local false :me/media-photo-did-success)
                         (rum/local false :me/media-attachment-did-success)
                         (rum/local false :me/showing-media-video-modal)
                         (rum/local false :me/showing-gif-selector)
                         ;; Image upload lock
                         (rum/local false :me/upload-lock)
                         (rum/local "" ::add-comment-id)

                         ;; Derivatives
                         (drv/drv :media-input)
                         (drv/drv :add-comment-focus)
                         (drv/drv :add-comment-data)
                         (drv/drv :team-roster)
                         (drv/drv :current-user-data)
                         ;; Locals
                         (rum/local true ::add-button-disabled)
                         (rum/local "" ::initial-add-comment)
                         (rum/local false ::did-change)
                         (rum/local false ::show-post-button)
                         ;; Mixins
                         ;; Mixins
                         ui-mixins/first-render-mixin
                         (mention-mixins/oc-mentions-hover)

                         (ui-mixins/on-window-click-mixin (fn [s e]
                          (when (and @(:me/showing-media-video-modal s)
                                     (not (.contains (.-classList (.-target e)) "media-video"))
                                     (not (utils/event-inside? e (rum/ref-node s :video-container))))
                            (me-media-utils/media-video-add s @(:me/media-picker-ext s) nil)
                            (reset! (:me/showing-media-video-modal s) false))
                          (when (and @(:me/showing-gif-selector s)
                                     (not (.contains (.-classList (.-target e)) "media-gif"))
                                     (not (utils/event-inside? e (sel1 [:div.giphy-picker]))))
                            (me-media-utils/media-gif-add s @(:me/media-picker-ext s) nil)
                            (reset! (:me/showing-gif-selector s) false))))
                         {:will-mount (fn [s]
                          (reset! (::add-comment-id s) (utils/activity-uuid))
                          (let [activity-data (first (:rum/args s))
                                add-comment-data @(drv/get-ref s :add-comment-data)
                                activity-add-comment-data (get add-comment-data (:uuid activity-data))]
                            (reset! (::initial-add-comment s) (or activity-add-comment-data ""))
                            (reset! (::show-post-button s) should-focus-field?))
                          s)
                          :did-mount (fn [s]
                           (me-media-utils/setup-editor s add-comment-did-change me-options)
                           (let [add-comment-node (rum/ref-node s "editor-node")]
                             (when (should-focus-field? s)
                               (.focus add-comment-node)
                               (utils/after 0
                                #(utils/to-end-of-content-editable add-comment-node))))
                           (utils/after 2500 #(js/emojiAutocomplete))
                           s)
                          :did-remount (fn [_ s]
                           (me-media-utils/setup-editor s add-comment-did-change me-options)
                           s)
                          :will-update (fn [s]
                           (let [data @(drv/get-ref s :media-input)
                                 video-data (:media-video data)]
                              (when (and @(:me/media-video s)
                                         (or (= video-data :dismiss)
                                             (map? video-data)))
                                (when (or (= video-data :dismiss)
                                          (map? video-data))
                                  (reset! (:me/media-video s) false)
                                  (dis/dispatch! [:update [:media-input] #(dissoc % :media-video)]))
                                (if (map? video-data)
                                  (me-media-utils/media-video-add s @(:me/media-picker-ext s) video-data)
                                  (me-media-utils/media-video-add s @(:me/media-picker-ext s) nil))))
                           s)
                          :will-unmount (fn [s]
                           (when @(:me/editor s)
                             (.destroy @(:me/editor s))
                             (reset! (:me/editor s) nil))
                           s)}
  [s activity-data parent-comment-uuid dismiss-reply-cb]
  (let [_add-comment-data (drv/react s :add-comment-data)
        _media-input (drv/react s :media-input)
        _team-roster (drv/react s :team-roster)
        add-comment-focus (drv/react s :add-comment-focus)
        current-user-data (drv/react s :current-user-data)
        add-comment-class (str "add-comment-" @(::add-comment-id s))
        container-class (str "add-comment-box-container-" @(::add-comment-id s))
        is-focused? (should-focus-field? s)
        should-hide-post-button (and ;; Hide post button onlu for the last add comment field, not
                                     ;; for the reply to comments
                                     (not parent-comment-uuid)
                                     (or (not @(::show-post-button s))
                                         (not is-focused?)
                                         ;; Hide post button of the last add comment field
                                         ;; when a thread comment box is focused
                                         (and (not is-focused?)
                                              (not parent-comment-uuid)
                                              (seq add-comment-focus))))]
    [:div.add-comment-box-container
      {:class container-class}
      [:div.add-comment-box
        (user-avatar-image current-user-data)
        [:div.add-comment-internal
          {:class (when-not should-hide-post-button "active")}
          [:div.add-comment.emoji-autocomplete.emojiable.oc-mentions.oc-mentions-hover
           {:ref "editor-node"
            :class (utils/class-set {add-comment-class true
                                     :medium-editor-placeholder-hidden @(::did-change s)
                                     utils/hide-class true})
            :on-focus #(focus-add-comment s)
            :on-blur #(disable-add-comment-if-needed s)
            :on-key-down (fn [e]
                          (let [add-comment-node (rum/ref-node s "editor-node")]
                            (when (and (= (.-key e) "Escape")
                                       (= (.-activeElement js/document) add-comment-node))
                              (.blur add-comment-node))
                            (when (and (= (.-activeElement js/document) add-comment-node)
                                       (.-metaKey e)
                                       (= (.-key e) "Enter"))
                              (send-clicked s (second (:rum/args s))))))
            :content-editable true
            :dangerouslySetInnerHTML #js {"__html" @(::initial-add-comment s)}}]]
        (when @(:me/showing-media-video-modal s)
          [:div.video-container
            {:ref :video-container}
            (media-video-modal {:fullscreen false
                                :dismiss-cb #(do
                                              (me-media-utils/media-video-add s @(:me/media-picker-ext s) nil)
                                              (reset! (:me/showing-media-video-modal s) false))
                                :offset-element-selector [(keyword (str "div." container-class))]
                                :outer-container-selector [(keyword (str "div." container-class))]})])
        (when @(:me/showing-gif-selector s)
          (giphy-picker {:fullscreen false
                         :pick-emoji-cb (fn [gif-obj]
                                         (reset! (:me/showing-gif-selector s) false)
                                         (me-media-utils/media-gif-add s @(:me/media-picker-ext s) gif-obj))
                         :offset-element-selector [(keyword (str "div." container-class))]
                         :outer-container-selector [(keyword (str "div." container-class))]}))
        [:div.add-comment-footer
          {:class (when should-hide-post-button "hidden")}
          [:button.mlb-reset.send-btn
            {:on-click #(send-clicked s parent-comment-uuid)
             :disabled @(::add-button-disabled s)}
            "Post"]
          (when (and parent-comment-uuid
                     (fn? dismiss-reply-cb))
            [:button.mlb-reset.close-reply-bt
              {:on-click dismiss-reply-cb
               :data-toggle (if (responsive/is-tablet-or-mobile?) "" "tooltip")
               :data-placement "right"
               :title "Close"}])
          (emoji-picker {:add-emoji-cb #(add-comment-did-change s)
                         :width 24
                         :height 24
                         :position "top"
                         :default-field-selector (str "div." add-comment-class)
                         :container-selector (str "div." add-comment-class)})]]]))