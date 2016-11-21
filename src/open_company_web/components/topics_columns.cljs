(ns open-company-web.components.topics-columns
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros (defcomponent)]
            [om-tools.dom :as dom :include-macros true]
            [dommy.core :refer-macros (sel1 sel)]
            [open-company-web.router :as router]
            [open-company-web.dispatcher :as dis]
            [open-company-web.lib.responsive :as responsive]
            [open-company-web.lib.utils :as utils]
            [open-company-web.components.topic :refer (topic)]
            [open-company-web.components.add-topic :as at]))

;; Calc best topics layout based on heights

(defn add-topic? [owner]
  (let [data (om/get-props owner)
        company-data (:company-data data)
        sharing-mode (om/get-props owner :sharing-mode)
        foce-active  (dis/foce-section-key)]
    (and (not (:hide-add-topic data))
         (responsive/can-edit?)
         (not sharing-mode)
         (not (:read-only company-data))
         (not foce-active))))

(def inter-topic-gap 22)
(def add-a-topic-height 95)

(def topic-default-height 70)
(def data-topic-default-zero-height 74)
(def data-topic-default-one-height 251)
(def data-topic-default-more-height 357)
(def topic-body-height 29)
(def add-topic-height 94)
(def read-more-height 15)

(def topic-margins 20)

(defn headline-body-height [headline body card-width]
  (let [$headline (js/$ (str "<div class=\"topic\">"
                                "<div class=\"topic-anim\">"
                                  "<div>"
                                    "<div class=\"topic-internal\">"
                                      (when-not (clojure.string/blank? headline)
                                        (str "<div class=\"topic-headline-inner\" style=\"width: " (+ card-width topic-margins) "px;\">"
                                               (utils/emojify headline true)
                                             "</div>"))
                                      "<div class=\"topic-body\" style=\"width: " (+ card-width topic-margins) "px;\">"
                                        (utils/emojify body true)
                                      "</div>"
                                    "</div>"
                                  "</div>"
                                "</div>"
                              "</div>"))]
    (.appendTo $headline (.-body js/document))
    (let [height (.height $headline)]
      (.detach $headline)
      height)))

(defn data-topic-height [owner topic topic-data]
  (if (= topic :finances)
    (cond
      (= (count (:data topic-data)) 0)
      data-topic-default-zero-height
      (= (count (:data topic-data)) 1)
      data-topic-default-one-height
      (> (count (:data topic-data)) 1)
      data-topic-default-more-height)
    (let [data (:data topic-data)
          selected-metric (or (om/get-props owner :selected-metric) (:slug (first (:metrics topic-data))))
          metric-data (filter #(= (:slug %) selected-metric) data)]
      (cond
        (= (count metric-data) 0)
        data-topic-default-zero-height
        (= (count metric-data) 1)
        data-topic-default-one-height
        (> (count metric-data) 1)
        data-topic-default-more-height))))

(defn calc-column-height [owner data topics clmn]
  (let [card-width (om/get-props owner :card-width)
        topics-data (:topics-data data)]
    (for [topic topics
          :let [topic-kw (keyword topic)
                topic-data (get topics-data topic-kw)
                is-data-topic (#{:finances :growth} topic-kw)
                topic-body (:body topic-data)]]
      (cond
        (= topic "add-topic")
        add-topic-height
        (#{:finances :growth} topic-kw)
        (let [headline-height (headline-body-height (:headline topic-data) topic-body card-width)
              start-height (data-topic-height owner topic topic-data)
              read-more    (if (clojure.string/blank? (utils/strip-HTML-tags (:body topic-data))) 0 read-more-height)]
          (+ start-height headline-height read-more))
        :else
        (let [topic-image-height      (if (:image-url topic-data)
                                        (utils/aspect-ration-image-height (:image-width topic-data) (:image-height topic-data) card-width)
                                        0)
              headline-body-height (headline-body-height (:headline topic-data) topic-body card-width)
              read-more               (if (clojure.string/blank? (utils/strip-HTML-tags (:body topic-data))) 0 read-more-height)]
          (+ topic-default-height headline-body-height topic-image-height read-more))))))

(defn get-shortest-column [owner data current-layout]
  (let [columns-num (:columns-num data)
        frst-clmn (apply + (calc-column-height owner data (:1 current-layout) 1))
        scnd-clmn (apply + (calc-column-height owner data (:2 current-layout) 2))
        thrd-clmn (apply + (calc-column-height owner data (:3 current-layout) 3))
        min-height (if (= columns-num 3)
                    (min frst-clmn scnd-clmn thrd-clmn)
                    (min frst-clmn scnd-clmn))]
    (cond
      (= min-height frst-clmn)
      :1
      (= min-height scnd-clmn)
      :2
      (= min-height thrd-clmn)
      :3)))

(defn get-pinned-layout
  "Return the layout of the pinned topics only on 3 or 2 columns depending on the
  current screen width."
  [pinned-topics columns-num]
  (if (= columns-num 3)
    (loop [idx 3
           cl1 (vec (remove nil? [(first pinned-topics)]))
           cl2 (vec (remove nil? [(second pinned-topics)]))
           cl3 (vec (remove nil? [(get pinned-topics 2)]))]
      (if (<= idx (count pinned-topics))
        (recur (+ idx 3)
               (vec (remove nil? (conj cl1 (get pinned-topics idx))))
               (vec (remove nil? (conj cl2 (get pinned-topics (inc idx)))))
               (vec (remove nil? (conj cl3 (get pinned-topics (+ idx 2))))))
        {:1 cl1
         :2 cl2
         :3 cl3}))
    (loop [idx 2
           cl1 (vec (remove nil? [(first pinned-topics)]))
           cl2 (vec (remove nil? [(second pinned-topics)]))]
      (if (<= idx (count pinned-topics))
        (recur (+ idx 2)
               (vec (remove nil? (conj cl1 (get pinned-topics idx))))
               (vec (remove nil? (conj cl2 (get pinned-topics (inc idx))))))
        {:1 cl1
         :2 cl2}))))

(defn calc-layout
  "Calculate the best layout given the list of topics and the number of columns to layout to"
  [owner data]
  (if (utils/is-test-env?)
    (om/get-props owner :topics)
    (let [columns-num (:columns-num data)
          company-data (:company-data data)
          show-add-topic (add-topic? owner)
          {:keys [pinned other]} (utils/get-pinned-other-keys (utils/get-section-keys company-data) company-data)
          final-layout (loop [idx 0
                              layout (get-pinned-layout pinned columns-num)]
                          (let [shortest-column (get-shortest-column owner data layout)
                                new-column (conj (get layout shortest-column) (get other idx))
                                new-layout (assoc layout shortest-column new-column)]
                            (if (<= (inc idx) (count other))
                              (recur (inc idx)
                                     new-layout)
                              new-layout)))
          clean-layout (apply merge (for [[k v] final-layout] {k (vec (remove nil? v))}))]
      clean-layout)))

(defn render-topic [owner options section-name & [column]]
  (when section-name
    (let [props                 (om/get-props owner)
          sharing-mode          (:sharing-mode props)
          share-selected-topics (:share-selected-topics props)
          company-data          (:company-data props)
          topics-data           (:topics-data props)
          topics                (:topics props)
          topic-click           (or (:topic-click options) identity)
          update-active-topics  (or (:update-active-topics options) identity)
          share-selected?       (utils/in? share-selected-topics section-name)
          slug                  (keyword (router/current-company-slug))
          window-scroll         (.-scrollTop (.-body js/document))
          {:keys [pinned other]} (utils/get-pinned-other-keys topics company-data)
          first-topic           (if (pos? (count pinned))
                                  (first pinned)
                                  (first other))]
      (if (= section-name "add-topic")
        (at/add-topic {:column column
                       :archived-topics (mapv (comp keyword :section) (:archived company-data))
                       :active-topics (vec topics)
                       :initially-expanded (and (not (om/get-props owner :loading))
                                                (om/get-props owner :new-sections)
                                                (zero? (count topics)))
                       :update-active-topics update-active-topics})
        (let [sd (->> section-name keyword (get topics-data))
              topic-row-style (if (or (utils/in? (:route @router/path) "su-snapshot-preview")
                                      (utils/in? (:route @router/path) "su-list"))
                                #js {}
                                #js {:width (if (responsive/is-mobile?) "auto" (str (:card-width props) "px"))})]
          (when-not (and (:read-only company-data) (:placeholder sd))
            (dom/div #js {:className "topic-row"
                          :data-topic (name section-name)
                          :style topic-row-style
                          :ref section-name
                          :key (str "topic-row-" (name section-name))}
              (om/build topic {:loading (:loading company-data)
                               :section section-name
                               :is-stakeholder-update (:is-stakeholder-update props)
                               :section-data sd
                               :card-width (:card-width props)
                               :foce-data-editing? (:foce-data-editing? props)
                               :show-share-remove (:show-share-remove props)
                               :read-only-company (:read-only company-data)
                               :currency (:currency company-data)
                               :sharing-mode sharing-mode
                               :foce-key (:foce-key props)
                               :foce-data (:foce-data props)
                               :share-selected share-selected?
                               :show-first-edit-tip (:show-first-edit-tip props)}
                               {:opts {:section-name section-name
                                       :share-remove-click (:share-remove-click options)
                                       :topic-click (partial topic-click section-name)}}))))))))

(defcomponent topics-columns [{:keys [columns-num
                                      sharing-mode
                                      content-loaded
                                      total-width
                                      card-width
                                      topics
                                      company-data
                                      topics-data
                                      is-stakeholder-update] :as data} owner options]

  (did-mount [_]
    (when (> columns-num 1)
      (om/set-state! owner :best-layout (calc-layout owner data))))

  (will-receive-props [_ next-props]
    (when (and (> (:columns-num next-props) 1)
               (or (not= (:topics next-props) (:topics data))
                   (not= (:columns-num next-props) (:columns-num data))))
      (om/set-state! owner :best-layout (calc-layout owner next-props))))

  (render-state [_ {:keys [best-layout]}]
    (let [show-add-topic         (add-topic? owner)
          partial-render-topic   (partial render-topic owner options)
          {:keys [pinned other]} (utils/get-pinned-other-keys topics company-data)
          columns-container-key   (str (apply str pinned) (apply str other))
          topics-column-conatiner-style (if (responsive/is-mobile?)
                                          #js {:margin "0px 9px"
                                               :width "auto"}
                                          #js {:width total-width})]
      ;; Topic list
      (dom/div {:class (utils/class-set {:topics-columns true
                                         :overflow-visible true
                                         :sharing-mode sharing-mode
                                         :group true
                                         :content-loaded content-loaded})}
        (cond
          ;; render 2 or 3 column layout
          (> columns-num 1)
          (dom/div {:class "topics-column-container group"
                    :style topics-column-conatiner-style
                    :key columns-container-key}
            ; for each column key contained in best layout
            (for [kw (if (= columns-num 3) [:1 :2 :3] [:1 :2])]
              ; get the pinned and the other topics of the current column
              (let [column (get best-layout kw)
                    {:keys [pinned other]} (utils/get-pinned-other-keys column company-data)]
                (dom/div {:class (str "topics-column col-" (name kw))
                          :style #js {:width (str (+ card-width topic-margins) "px")}}
                  ; render the pinned topics
                  (dom/div #js {:className "topics-column-pinned"}
                    (when (pos? (count pinned))
                      (for [idx (range (count pinned))
                            :let [section-kw (get pinned idx)
                                  section-name (name section-kw)]]
                        (partial-render-topic section-name
                                              (when (= section-name "add-topic") (int (name kw)))))))
                  ; render the other topics
                  (dom/div #js {:className "topics-column-other"}
                    (when (pos? (count other))
                      (for [idx (range (count other))
                            :let [section-kw (get other idx)
                                  section-name (name section-kw)]]
                        (partial-render-topic section-name
                                              (when (= section-name "add-topic") (int (name kw))))))
                    ; render the add topic in the correct column
                    (when (and show-add-topic
                               (= kw :1)
                               (= (count topics) 0))
                      (partial-render-topic "add-topic" 1))
                    (when (and show-add-topic
                               (= kw :2)
                               (or (and (= (count topics) 1)
                                        (= columns-num 3))
                                   (and (>= (count topics) 1)
                                        (= columns-num 2))))
                      (partial-render-topic "add-topic" 2))
                    (when (and show-add-topic
                               (= kw :3)
                               (>= (count topics) 2))
                      (partial-render-topic "add-topic" 3)))))))
          ;; 1 column or default
          :else
          (dom/div {:class "topics-column-container columns-1 group"
                    :style topics-column-conatiner-style
                    :key columns-container-key}
            (dom/div {:class "topics-column"}
              (dom/div #js {:className "topics-column-pinned"}
                (for [section pinned]
                  (partial-render-topic (name section))))
              (dom/div #js {:className "topics-column-other"}
                (for [section other]
                  (partial-render-topic (name section)))
                (when show-add-topic
                  (partial-render-topic "add-topic" 1))))))))))