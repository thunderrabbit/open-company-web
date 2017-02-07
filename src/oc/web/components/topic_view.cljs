(ns oc.web.components.topic-view
  (:require-macros [if-let.core :refer (when-let*)])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros (defcomponent)]
            [om-tools.dom :as dom :include-macros true]
            [oc.web.api :as api]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.tooltip :as t]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.responsive :as responsive]
            [oc.web.components.topic :refer (topic)]
            ; [open-company-web.components.topic-edit :refer (topic-edit)]
            [oc.web.components.ui.popover :refer (add-popover hide-popover)]
            [oc.web.components.ui.loading :refer (loading)]
            [cuerdas.core :as s]))

(defn- remove-topic-click [owner e]
  (add-popover {:container-id "archive-topic-confirm"
                :message utils/before-archive-message
                :height "170px"
                :cancel-title "KEEP IT"
                :cancel-cb #(hide-popover nil "archive-topic-confirm")
                :success-title "ARCHIVE"
                :success-cb #(let [topic (om/get-props owner :selected-topic-view)]
                               (dis/dispatch! [:topic-archive topic])
                               (hide-popover nil "archive-topic-confirm")
                               (router/nav! (oc-urls/board)))}))

(defn load-revisions [owner]
  (when-let* [topic-name (om/get-props owner :selected-topic-view)
                board-data (om/get-props owner :board-data)
                topic-data (->> topic-name keyword (get board-data))
                revisions-link (utils/link-for (:links topic-data) "revisions" "GET")]
    ; Do not request old revisions if there is the :new key
    ; since it was newly added from the add topic component
    ; and also it's not an archived topic being reactivated
    (when-not (:placeholder topic-data)
      (om/set-state! owner :revisions-requested true)
      (api/load-revisions (router/current-board-slug) topic-name revisions-link))))

(defn load-revisions-if-needed [owner]
  (when (not (om/get-state owner :revisions-requested))
    (load-revisions owner)))

(defn start-foce-if-needed [{:keys [foce-key
                                    board-data
                                    selected-topic-view
                                    new-topics]
                             :as data} ]
  (when (nil? foce-key)
    (let [topic-kw (keyword selected-topic-view)
          topics-contains-topic (utils/in? (:topics board-data) selected-topic-view)]
      (when (not topics-contains-topic)
        (if (:read-only board-data)
          (router/redirect! (oc-urls/board (router/current-org-slug) (:slug board-data)))
          ; look for the urls in the new topics
          (when new-topics
            (let [new-topic (first (filter #(= (:topic-name %) selected-topic-view) new-topics))
                  new-topic-data (utils/new-topic-initial-data topic-kw (:title new-topic) {:links (:links new-topic)})]
              (if (and new-topic (contains? new-topic :links))
                (dis/dispatch! [:add-topic topic-kw (assoc new-topic-data :new true)])
                (router/redirect! (oc-urls/board (router/current-org-slug) (:slug board-data)))))))))))

(defcomponent topic-view [{:keys [card-width
                                  columns-num
                                  selected-topic-view
                                  board-data
                                  foce-key
                                  foce-data
                                  foce-data-editing?] :as data} owner options]
  (init-state [_]
    {:revisions-requested false})

  (did-mount [_]
    (dis/dispatch! [:show-add-topic false])
    (load-revisions-if-needed owner)
    (start-foce-if-needed owner)
    (om/set-state! owner :revisions-reload-interval (js/setInterval #(load-revisions owner) (* 60 1000))))

  (will-update [_ next-props _]
    (start-foce-if-needed next-props)
    (when (not= (:selected-topic-view next-props) selected-topic-view)
      (om/set-state! owner :revisions-requested false)))

  (did-update [_ _ _]
    (load-revisions-if-needed owner)
    (start-foce-if-needed owner))

  (will-unmount [_]
    (when (om/get-state owner :revisions-reload-interval)
      (js/clearInterval (om/get-state owner :revisions-reload-interval))))

  (render [_]
    (let [topic-kw (keyword selected-topic-view)
          topic-view-width (responsive/topic-view-width card-width columns-num)
          topic-card-width (responsive/calc-update-width columns-num)
          topic-data (get board-data topic-kw)
          is-custom-topic (s/starts-with? (:topic topic-data) "custom-")
          revisions (:revisions-data topic-data)
          is-new-foce (and (= foce-key topic-kw) (nil? (:created-at foce-data)))
          is-another-foce (and (not (nil? foce-key)) (not (nil? (:created-at foce-data))))
          loading-topic-data (and (contains? board-data topic-kw)
                                  (:loading topic-data))]
      (dom/div {:class "topic-view-container group"
                :style {:width (if (responsive/is-tablet-or-mobile?) "100%" (str topic-card-width "px"))
                        :margin-right (if (responsive/is-tablet-or-mobile?) "0px" (str (max 0 (- topic-view-width topic-card-width 50)) "px"))}
                :key (str "topic-view-inner-" selected-topic-view)}
        (dom/div {:class (utils/class-set {:topic-view true
                                           :group true
                                           :tablet-view (responsive/is-tablet-or-mobile?)
                                           :topic-404 (nil? topic-data)})}
          (when (responsive/is-tablet-or-mobile?)
            (dom/div {:class "topic-view-navbar"}
              (dom/div {:class "topic-view-navbar-close left"
                        :on-click #(router/nav! (oc-urls/board))} "<")
              (dom/div {:class "topic-view-navbar-title left"} (:title topic-data))))
          (if (or (:loading board-data)
                  loading-topic-data)
            (dom/div {:class "topic-view-internal loading group"}
              (om/build loading {:loading true}))
            (dom/div {:class "topic-view-internal"
                      :style {:width (if (responsive/is-tablet-or-mobile?) "auto" (str topic-card-width "px"))}}
              (when (and (not (:read-only board-data))
                         (not (responsive/is-tablet-or-mobile?)))
                (dom/div {:class (str "fake-textarea group " (when is-another-foce "disabled"))}
                  (cond
                    ; is-new-foce
                    ; (dom/div {:class "topic topic-edit"
                    ;           :style {:width (str (- topic-card-width 120) "px")}}
                    ;   (om/build topic-edit {:is-stakeholder-update false
                    ;                         :currency (:currency board-data)
                    ;                         :card-width (- topic-card-width 120)
                    ;                         :columns-num columns-num
                    ;                         :is-topic-view true
                    ;                         :foce-data-editing? foce-data-editing?
                    ;                         :foce-key foce-key
                    ;                         :show-delete-entry-button false
                    ;                         :foce-data foce-data}
                    ;                        {:opts options
                    ;                         :key (str "topic-foce-" selected-topic-view "-new")}))
                    (utils/in? (:topics board-data) selected-topic-view)
                    (let [initial-data (utils/new-topic-initial-data selected-topic-view (:title topic-data) topic-data)
                          with-data (if (#{:growth :finances} topic-kw) (assoc initial-data :data (:data topic-data)) initial-data)
                          with-metrics (if (= :growth topic-kw) (assoc with-data :metrics (:metrics topic-data)) with-data)]
                      (dom/div {:class "fake-textarea-internal"
                                :on-click #(dis/dispatch! [:start-foce topic-kw with-metrics])}
                        (:title topic-data)
                        (dom/br)
                        (dom/span {:class "new-entry"} "Start a new entry...")))
                    :else
                    (om/build loading {:loading true}))))
              ;; Render the topic from the board data only until the revisions are loaded.
              (when (and (not foce-key)
                         (not revisions)
                         (not (:placeholder topic-data))
                         (utils/in? (:topics board-data) selected-topic-view))
                (dom/div {:class "revision-container group"}
                  (om/build topic {:topic selected-topic-view
                                   :topic-data topic-data
                                   :card-width (- topic-card-width 60)
                                   :is-stakeholder-update false
                                   :read-only-board (:read-only board-data)
                                   :currency (:currency board-data)
                                   :is-topic-view true
                                   :foce-data-editing? foce-data-editing?
                                   :foce-key foce-key
                                   :foce-data foce-data
                                   :show-editing false}
                                   {:opts {:topic-name selected-topic-view}})))
              (for [idx (range (count revisions))
                    :let [rev (get revisions idx)]]
                (when rev
                  (dom/div {:class "revision-container group"}
                    (when-not (= idx 0)
                      (dom/hr {:class "separator-line"
                               :style {:width (if (responsive/is-tablet-or-mobile?) "auto" (str (- topic-card-width 60) "px"))}}))
                    (om/build topic {:topic selected-topic-view
                                     :topic-data rev
                                     :card-width (- topic-card-width 60)
                                     :is-stakeholder-update false
                                     :read-only-board (:read-only board-data)
                                     :currency (:currency board-data)
                                     :is-topic-view true
                                     :foce-data-editing? foce-data-editing?
                                     :foce-key foce-key
                                     :foce-data foce-data
                                     :show-delete-entry-button true
                                     :show-editing true}
                                     {:opts {:topic-name selected-topic-view}
                                      :key (str "topic-"
                                            (when foce-key
                                              "foce-")
                                            selected-topic-view "-" (:updated-at rev))})))))))
        (when (and (not loading-topic-data)
                   (not (responsive/is-tablet-or-mobile?))
                   (not (:read-only board-data)))
          (dom/button {:class "btn-reset btn-link archive-topic"
                       :on-click (partial remove-topic-click owner)} "Archive this topic"))))))