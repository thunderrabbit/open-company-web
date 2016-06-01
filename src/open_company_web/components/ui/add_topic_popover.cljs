(ns open-company-web.components.ui.add-topic-popover
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros (defcomponent)]
            [om-tools.dom :as dom :include-macros true]
            [dommy.core :as dommy :refer-macros (sel1)]
            [goog.events :as events]
            [goog.events.EventType :as EventType]
            [goog.history.EventType :as HistoryEventType]
            [open-company-web.api :as api]
            [open-company-web.urls :as oc-urls]
            [open-company-web.caches :as caches]
            [open-company-web.router :as router]
            [open-company-web.dispatcher :as dis]
            [open-company-web.lib.utils :as utils]
            [cljsjs.react.dom]))

(defn is-child-of-popover [el]
  (loop [cur-el el]
    (if (= (.-id cur-el) "add-topic-popover")
      true
      (if (.-parentNode cur-el)
        (recur (.-parentNode cur-el))
        false))))

(defn dismiss-popover [options]
  (.replaceState js/history #js {} "Dashboard" (oc-urls/company))
  ((:dismiss-popover options)))

(defn on-click-out [owner options e]
  (when (not (is-child-of-popover (.-target e)))
    (dismiss-popover options)))

(defn on-click-in [owner options e]
  (.stopPropagation e))

(defn get-state [{:keys [active-topics-list all-topics archived-topics] :as data} old-state]
  (let [topics-list (map name (keys all-topics))
        sorted-archived-topics (sort #(compare (:title %1) (:title %2)) archived-topics)
        archived-topics-list (vec (map :section sorted-archived-topics))
        reduce-dissoc (partial reduce utils/vec-dissoc)
        unactive-topics (map name (-> topics-list
                                      (reduce-dissoc active-topics-list)
                                      (reduce-dissoc archived-topics-list)))
        unactive-equal? (= (set unactive-topics) (set (:unactive-topics old-state)))]
    {:active-topics active-topics-list
     :archived-topics-list archived-topics-list
     :unactive-topics (if unactive-equal? (:unactive-topics old-state) unactive-topics)
     :highlighted-topic nil
     :adding-custom-topic false
     :custom-topic-title ""}))

(defn add-topic-click [owner options topic]
  (let [all-topics (om/get-props owner :all-topics)
        topic-data (->> topic keyword (get all-topics))
        category-name (:category topic-data)]
    ((:did-change-active-topics options) category-name topic)
    (dismiss-popover options)))

(def down-arrow-key-code 40)
(def up-arrow-key-code 38)
(def enter-key-code 13)
(def esc-key-code 27)

(defn kb-key-down [owner options e]
  (when-not (om/get-state owner :adding-custom-topic)
    (let [key-code        (.-keyCode e)
          unactive-topics (om/get-state owner :unactive-topics)]
      (when (or (= key-code down-arrow-key-code)
                (= key-code up-arrow-key-code)
                (= key-code enter-key-code)
                (= key-code esc-key-code))
        (.stopPropagation e)
        (.preventDefault e)
        (cond
          (= key-code enter-key-code)
          ; enter key: select topic
          (when-let [topic (om/get-state owner :highlighted-topic)]
            (add-topic-click owner options topic))
          (= key-code esc-key-code)
          (dismiss-popover options)
          :else
          ; arrow key: change focus and scroll the parent div
          (let [cur-highlighted  (om/get-state owner :highlighted-topic)
                idx-fn           (cond
                                   (= key-code down-arrow-key-code) inc
                                   (= key-code up-arrow-key-code) dec)
                min-idx          0
                max-idx          (dec (count unactive-topics))
                next-highlighted (min (max (idx-fn cur-highlighted) min-idx) max-idx)
                topics-to-add    (om/get-ref owner "topics-to-add")
                new-topic-div    (om/get-ref owner (str "potential-topic-" next-highlighted))]
            (set! (.-scrollTop topics-to-add) (- (.-offsetTop new-topic-div) (.-offsetTop topics-to-add)))
            (cond
              (= key-code down-arrow-key-code)
              (om/set-state! owner :highlighted-topic next-highlighted)
              (= key-code up-arrow-key-code)
              (om/set-state! owner :highlighted-topic next-highlighted))))))))

(defn history-nav [options]
  ((:dismiss-popover options)))

(defn add-custom-topic [owner e]
  (.preventDefault e)
  (.stopPropagation e)
  (om/set-state! owner :adding-custom-topic true))

(defn custom-topic-key-down [owner e]
  (.stopPropagation e)
  (let [key-code (.-keyCode e)]
    (cond
      (and (= key-code enter-key-code)
           (pos? (count (om/get-state owner :custom-topic-title))))
      ; TODO: add title
      (let [topic-name (str "custom-" (utils/my-uuid))
            topic-title (om/get-state owner :custom-topic-title)
            company-data (dis/company-data)
            sections (:sections company-data)
            new-sections (update-in sections [:progress] #(conj % topic-name))]
        (api/patch-sections new-sections))
      (= key-code esc-key-code)
      ; ESC: exit adding topic
      (om/set-state! owner :adding-custom-topic false))))

(defcomponent add-topic-popover [{:keys [all-topics active-topics-list column show-above archived-topics] :as data} owner options]

  (init-state [_]
    (when (empty? @caches/new-sections)
      (api/get-new-sections))
    (get-state data nil))

  (will-mount [_]
    (.pushState js/history #js {} "Add topic" (str (oc-urls/company) "#add-topic")))

  (did-mount [_]
    (let [click-listener (events/listen (sel1 [:body]) EventType/CLICK (partial on-click-out owner options))
          kb-listener (events/listen (sel1 [:body]) EventType/KEYDOWN (partial kb-key-down owner options))
          nav-listener (events/listen @router/history HistoryEventType/NAVIGATE (partial history-nav options))]
      (om/set-state! owner :click-out-listener click-listener)
      (om/set-state! owner :kb-listener kb-listener)
      (om/set-state! owner :nav-listener nav-listener)))

  (will-receive-props [_ next-props]
    (when-not (= next-props data)
      (om/set-state! owner (get-state next-props (om/get-state owner)))))

  (will-unmount [_]
    (events/unlistenByKey (om/get-state owner :click-out-listener))
    (events/unlistenByKey (om/get-state owner :kb-listener))
    (events/unlistenByKey (om/get-state owner :nav-listener)))

  (did-update [_ _ prev-state]
    (when (and (om/get-state owner :adding-custom-topic)
               (not (:adding-custom-topic prev-state)))
      (.focus (.findDOMNode js/ReactDOM (om/get-ref owner "add-custom-topic-input")))))

  (render-state [_ {:keys [active-topics unactive-topics highlighted-topic archived-topics-list adding-custom-topic custom-topic-title]}]
    (dom/div {:class (utils/class-set {:add-topic-popover true
                                       (str "column-" column) true
                                       :show-above show-above})
              :id "add-topic-popover"
              :on-click (partial on-click-in owner options)}
      (dom/div {:class "triangle"})
      (dom/div {:class "add-topic-popover-header"} "CHOOSE A TOPIC")
      (dom/div {:class "add-topic-popover-scroll group"}
        (dom/div {:class "add-topic-popover-subheader"} "SUGGESTED TOPICS")
        (dom/div #js {:className "topics-to-add"
                      :ref "topics-to-add"}
          (for [idx (range (count unactive-topics))
                :let [topic (get (vec unactive-topics) idx)
                      topic-data (->> topic keyword (get all-topics))]]
            (dom/div #js {:className (str "potential-topic" (when (= highlighted-topic topic) " highlighted"))
                          :ref (str "potential-topic-unactive-" idx)
                          :onMouseOver #(om/set-state! owner :highlighted-topic topic)
                          :onClick #(add-topic-click owner options topic)}
              (:title topic-data))))
        (when (pos? (count archived-topics-list))
          (dom/div {:class "add-topic-popover-subheader"} "ARCHIVED TOPICS"))
        (when (pos? (count archived-topics-list))
          (dom/div #js {:className "topics-to-add"
                        :ref "topics-to-add"}
            (for [idx (range (count archived-topics-list))
                  :let [topic (get (vec archived-topics-list) idx)
                        topic-data (->> topic keyword (get all-topics))
                        topic-title (:title (first (filter #(= (:section %) topic) archived-topics)))]]
              (dom/div #js {:className (str "potential-topic" (when (= highlighted-topic topic) " highlighted"))
                            :ref (str "potential-topic-archived-" idx)
                            :onMouseOver #(om/set-state! owner :highlighted-topic topic)
                            :onClick #(add-topic-click owner options topic)}
                topic-title))))
        (if-not adding-custom-topic
          (dom/div {:class "add-custom-topic"
                    :on-click (partial add-custom-topic owner)} "+ Or Add New Topic")
          (dom/div {:class "add-custom-topic-container"}
            (dom/input #js {:type "text"
                            :className "add-custom-topic-input"
                            :ref "add-custom-topic-input"
                            :value custom-topic-title
                            :onChange #(om/set-state! owner :custom-topic-title (.-value (.-target %)))
                            :onKeyDown  (partial custom-topic-key-down owner)
                            :placeholder "Topic title"})))))))