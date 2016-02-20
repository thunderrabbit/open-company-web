(ns open-company-web.components.company-header
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [dommy.core :refer-macros (sel1)]
            [open-company-web.components.ui.company-avatar :refer (company-avatar)]
            [open-company-web.components.category-nav :refer (category-nav)]
            [open-company-web.router :as router]
            [goog.events :as events]
            [goog.style :as gstyle])
  (:import [goog.events EventType]))

(def company-header-default-height 50)

(defn scroll-watch [owner]
  (when-let [company-name-container (om/get-ref owner "company-name-container")]
    (let [category-nav (sel1 [:div.category-nav])
          company-header (om/get-ref owner "company-header")
          company-description-container (om/get-ref owner "company-description-container")
          topic-list (sel1 [:div.topic-list])
          company-name-offset-top (.-offsetTop company-name-container)
          company-header-height (.-clientHeight company-header)
          category-nav-height (.-clientHeight category-nav)
          company-name-container-height (.-clientHeight company-name-container)
          category-nav-pivot (- company-header-height category-nav-height company-name-container-height)]
      (events/listen
        js/window
        EventType.SCROLL
        (fn [e]
          (let [scroll-top (.-scrollTop (.-body js/document))]
            (if (> scroll-top company-name-offset-top)
              (do
                (gstyle/setStyle company-name-container #js {:position "fixed"})
                (gstyle/setStyle company-description-container #js {:marginTop "50px"}))
              (do
                (gstyle/setStyle company-name-container #js {:position "relative"})
                (gstyle/setStyle company-description-container #js {:marginTop "0px"})))
            (if (> scroll-top category-nav-pivot)
              (do
                (gstyle/setStyle category-nav #js {:position "fixed"
                                                   :top "50px"
                                                   :left "0"})
                (gstyle/setStyle topic-list #js {:margin-top "50px"}))
              (do
                (gstyle/setStyle category-nav #js {:position "relative"
                                                   :top "0"
                                                   :left "0"})
                (gstyle/setStyle topic-list #js {:margin-top "0px"})))
            ))))))

(defcomponent company-header [data owner]

  (did-mount [_]
    (scroll-watch owner))
 
  (render [_]
    (let [company-data (:company-data data)]
      (dom/div #js {:className "company-header"
                    :ref "company-header"}

        ;; Company logo
        (dom/div {:class "container"}
          (dom/img {:src (:logo company-data)
                    :class "company-logo"
                    :title (:name company-data)}))

        ;; Company name
        (dom/div #js {:className "company-name-container oc-header"
                      :ref "company-name-container"}
          (dom/div {:class "company-name"} (:name company-data)))

        ;; Company description
        (dom/div #js {:className "container oc-header"
                      :ref "company-description-container"}
          (dom/div {:class "company-description"} (:description company-data)))

        ;; Category navigation
        (om/build category-nav data)))))