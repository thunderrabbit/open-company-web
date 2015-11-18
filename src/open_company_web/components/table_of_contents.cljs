(ns open-company-web.components.table-of-contents
  (:require-macros [cljs.core.async.macros :refer (go)])
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros (defcomponent)]
            [om-tools.dom :as dom :include-macros true]
            [cljs.core.async :refer (chan <!)]
            [open-company-web.router :as router]
            [open-company-web.lib.utils :as utils]
            [open-company-web.api :as api]
            [open-company-web.dispatcher :as dispatcher]
            [open-company-web.components.new-section-popover :refer (new-section-popover)]))

(def first-sec-placeholder "firstsectionplaceholder")

(defn get-category-section-info [e]
  (let [target (.-target e)
        $el (.$ js/window target)
        category (.data $el "category")
        section (.data $el "section")]
    {:category category :section section}))

(defn show-popover [e category section]
  (when (.-$ js/window) ; avoid tests crash
    (let [$info (.$ js/window "#last-add-section-info")]
      (.data $info "category" category)
      (.data $info "section" section))
    (let [popover (.$ js/window "#new-section-popover-container")]
      (.click popover (fn [e]
                        (.fadeOut popover 400 #(.css popover #js {"display" "none"}))))
      (.setTimeout js/window #(.fadeIn popover 400) 0))))

(defn add-popover-container [data]
  (when (.-$ js/window) ; avoid tests crash
    (let [popover (.$ js/window "<div id='new-section-popover-container'></div>")
          body (.$ js/window (.-body js/document))]
      ; if the new-section-popover div is not present add it
      (when-not (pos? (.-length (.$ js/window "body div#new-section-popover-container")))
        (.append body popover))
      ; if the new-section-popover component has not been mount, render it
      (when-not (pos? (.-length (.$ js/window "body div#new-section-popover-container div.new-section-popover")))
        (.setTimeout js/window
                     #(om/root new-section-popover
                               dispatcher/app-state
                               {:target (.getElementById js/document "new-section-popover-container")})
                     1000)))))

(defn insert-section
  [category-into section-after category-to-insert section-to-insert sections]
  (let [category-kw (keyword category-into)
        category (category-kw sections)]
    (if (= section-to-insert first-sec-placeholder)
      (let [new-category (conj section-to-insert (category-kw sections))]
        (merge sections {category-kw new-category}))
      (if-not (= category-into category-to-insert)
        (let [new-category (conj section-to-insert (category-kw sections))]
          (merge sections {category-kw new-category}))
        (let [idx (inc (.indexOf (to-array category) section-after))
              [before after] (split-at idx category)
              new-category (vec (concat before [section-to-insert] after))]
          (merge sections {category-kw new-category}))))))

(defn handle-add-section-change [change]
  (let [$info (.$ js/window "#last-add-section-info")
        last-section (.data $info "section")
        last-category (.data $info "category")
        slug (keyword (:slug @router/path))
        company-data (slug @dispatcher/app-state)
        sections (:sections company-data)
        new-categories (insert-section last-category last-section (:category change) (:section change) sections)
        section-defaults (utils/fix-section (merge (:section-defaults change) {:oc-editing true
                                                                               :updated-at (utils/as-of-now)})
                                            (name (:section change)))
        new-section-kw (keyword (:section change))]
    (swap! dispatcher/app-state assoc-in [slug] (merge (slug @dispatcher/app-state) {new-section-kw section-defaults}))
    (swap! dispatcher/app-state assoc-in [slug :sections] new-categories)
    (.setTimeout js/window #(utils/scroll-to-section new-section-kw) 1000)))

(defcomponent add-section [data owner]

  (render [_]
    (let [section (name (:section data))
          category (name (:category data))]
      (dom/div {:id (str "new-section-*-" (name section))
                :class "new-section"
                :on-click #(show-popover % category section)}
        (dom/div {:class "new-section-internal"})
        (dom/div {:class "add-section"
                  :on-click #(show-popover % category section)}
          (dom/i {:class "fa fa-plus"}))))))

(defcomponent table-of-contents [data owner]

  (did-mount [_]
    (let [add-section-chan (chan)]
      (utils/add-channel "add-section" add-section-chan)
      (go (loop []
        (let [change (<! add-section-chan)]
          (handle-add-section-change change)
          (recur)))))
    (add-popover-container data))

  (render [_]
    (let [sections (:sections data)
          categories (:categories data)]
      (dom/div #js {:className "table-of-contents" :ref "table-of-contents"}
        (dom/div {:id "last-add-section-info"
                  :data-category ""
                  :data-section ""})
        (dom/div {:class "table-of-contents-inner"}
          (for [category categories]
            (dom/div {:class "category-container"}
              (dom/div {:class (utils/class-set {:category true
                                                 :empty (zero? (count ((keyword category) sections)))})}
                       (dom/h3 (utils/camel-case-str (name category))))
              (om/build add-section {:category category
                                     :section first-sec-placeholder})
              (for [section (into [] (get sections (keyword category)))]
                (let [section-data ((keyword section) data)]
                  (dom/div {}
                    (dom/div {:class "category-section"}
                      (dom/div {:class "category-section-close"
                                :on-click #(api/remove-section (name section))})
                      (dom/a {:href "#"
                              :on-click (fn [e]
                                          (.preventDefault e)
                                          (utils/scroll-to-section (name section)))}
                        (dom/p {:class "section-title"} (:title section-data))
                        (dom/p {:class "section-date"} (utils/time-since (:updated-at section-data)))))
                    (om/build add-section {:category category
                                           :section section})))))))))))