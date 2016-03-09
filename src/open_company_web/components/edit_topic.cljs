(ns open-company-web.components.edit-topic
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [cljs.core.async :refer (chan <!)]
            [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [dommy.core :refer-macros (sel1)]
            [open-company-web.lib.utils :as utils]
            [open-company-web.api :as api]
            [cljs-dynamic-resources.core :as cdr]))

(defn add-navbar-channels []
  (let [save-ch (chan)
        cancel-ch (chan)]
    (utils/add-channel "save-bt-navbar" save-ch)
    (utils/add-channel "cancel-bt-navbar" cancel-ch)))

(defn remove-navbar-channels []
  (utils/remove-channel "save-bt-navbar")
  (utils/remove-channel "cancel-bt-navbar"))

(defn tinymce-options [opts]
  (clj->js {
    :selector "div.body-editor"
    :theme "advanced"
    :toolbar "bold,italic,strikethrough,style-h1,bullist,link,unlink,image"
    :plugins "link,image,stylebuttons"
    :resize false
    :menubar false
    :statusbar false
    :theme_advanced_buttons1 ""
    :theme_advanced_buttons2 ""
    :theme_advanced_buttons3 ""
    :setup (fn [editor]
             (.on editor "change" #((:save-bt-active-cb opts) true)))}))

(defn setup-editor [owner options]
  (when (and (om/get-state owner :did-mount)
             (om/get-state owner :did-load-resources))
    (let [tinymce-editor (.init js/tinymce (tinymce-options options))]
      (om/set-state! owner :tinymce-editor tinymce-editor))))

(defn body-content []
  (.getContent (.-activeEditor js/tinymce)))

(defcomponent edit-topic [{:keys [section section-data] :as data} owner options]

  (init-state [_]
    (cdr/add-script! "/lib/tinymce/tinymce.min.js"
                      (fn []
                        (om/set-state! owner :did-load-resources true)
                        (setup-editor owner options)))
    {:title (:title section-data)
     :headline (:headline section-data)
     :tinymce-editor nil
     :did-mount false
     :did-load-resources false})

  (will-mount [_]
    ((:navbar-editing-cb options) true "Edit")
    (add-navbar-channels))

  (will-unmount [_]
    (remove-navbar-channels))

  (did-mount [_]
    (om/set-state! owner :did-mount true)
    (setup-editor owner options)
    (let [save-ch (utils/get-channel "save-bt-navbar")]
      (go (loop []
        (let [change (<! save-ch)]
          (let [section-data {:title (om/get-state owner :title)
                              :headline (om/get-state owner :headline)
                              :body (body-content)}]
            (api/partial-update-section section section-data)
            ((:dismiss-topic-editing-cb options) true))))))
    (let [cancel-ch (utils/get-channel "cancel-bt-navbar")]
      (go (loop []
        (let [change (<! cancel-ch)]
          ((:dismiss-topic-editing-cb options) false))))))

  (render-state [_ {:keys [title headline]}]
    (dom/div {:class "edit-topic"}
      (dom/div {:class "edit-topic-internal group"}
        (dom/div {:class "edit-topic-title"}
          (dom/input #js {:ref "topic-title"
                          :className "edit-topic-title-input"
                          :maxLength 100
                          :value title
                          :onChange (fn [e]
                                      (om/set-state! owner :title (.. e -target -value))
                                      ((:save-bt-active-cb options) true))}))
        (dom/div {:class "edit-topic-headline"}
          (dom/input #js {:ref "topic-headline"
                          :className "edit-topic-headline-input"
                          :placeholder "Headline"
                          :maxLength 120
                          :value headline
                          :onChange (fn [e]
                                      (om/set-state! owner :headline (.. e -target -value))
                                      ((:save-bt-active-cb options) true))}))
        (dom/div {:class "edit-topic-body"}
          (dom/div #js {:ref "topic-body"
                        :className "body-editor"
                        :dangerouslySetInnerHTML (clj->js {"__html" (:body section-data)})})
          (dom/div #js {:className "toolbar-container"
                        :ref "toolbar-container"}))))))