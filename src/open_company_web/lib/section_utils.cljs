(ns open-company-web.lib.section-utils
  (:require [open-company-web.lib.utils :as utils]
            [open-company-web.router :as router]
            [open-company-web.dispatcher :as dispatcher]
            [open-company-web.components.finances.utils :as finances-utils]))

(defn insert-section
  [category-into section-after category-to-insert section-to-insert sections]
  (let [category-kw (keyword category-to-insert)
        category (category-kw sections)]
    (cond
      ; category doesn't exist, create it with the new section
      (not (contains? sections category-kw))
      (merge sections {category-kw [section-to-insert]})
      ; categories are different, adding as last section
      (not= category-into category-to-insert)
      (merge sections {category-kw (conj (category-kw sections) section-to-insert)})
      ; category exists, section is placeholder for first place
      (= section-after finances-utils/first-section-placeholder)
      (let [new-category (concat [section-to-insert] (category-kw sections))]
        (merge sections {category-kw (vec new-category)}))
      ; category exists, adding section
      :else
      (let [idx (inc (.indexOf (to-array category) section-after))
            [before after] (split-at idx category)
            new-category (vec (concat before [section-to-insert] after))]
        (merge sections {category-kw new-category})))))

(defn add-section [last-category last-section new-category new-section section-defaults]
  (let [slug (keyword (:slug @router/path))
        company-data (slug @dispatcher/app-state)
        sections (:sections company-data)
        new-sections (insert-section last-category last-section new-category new-section sections)
        section-defaults (utils/fix-section (merge section-defaults {:oc-editing true
                                                                     :updated-at (utils/as-of-now)})
                                            (name new-section))
        body-placeholder (if (contains? section-defaults :note)
                           (:note section-defaults)
                           (:body section-defaults))
        with-body-placeholder (-> section-defaults
                                  (dissoc :body)
                                  (dissoc :note)
                                  (assoc :body-placeholder body-placeholder))
        with-title-placeholder (-> with-body-placeholder
                                   (dissoc :title)
                                   (assoc :title-placeholder (:title with-body-placeholder)))
        new-section-kw (keyword new-section)
        new-categories (if (utils/in? (:categories company-data) new-category)
                         (:categories company-data)
                         (conj (:categories company-data) new-category))]
    (swap! dispatcher/app-state assoc-in [slug] (merge company-data {new-section-kw with-title-placeholder}))
    (swap! dispatcher/app-state assoc-in [slug :sections] new-sections)
    (swap! dispatcher/app-state assoc-in [slug :categories] new-categories)))

(defn remove-section [sections section-name]
  (apply merge (map (fn [[k v]]
                      (hash-map k (utils/vec-dissoc v section-name)))
                    sections)))

(defn remove-unsaved-section [section-name]
  (let [section (keyword section-name)
        slug (keyword (:slug @router/path))
        company-data (slug @dispatcher/app-state)
        new-sections (remove-section (:sections company-data) section-name)]
    ; remove the section body
    (swap! dispatcher/app-state update-in [slug] dissoc section)
    ; remove the section from sections
    (swap! dispatcher/app-state assoc-in [slug :sections] new-sections)))