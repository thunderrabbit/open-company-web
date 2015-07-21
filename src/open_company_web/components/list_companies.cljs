(ns open-company-web.components.list-companies
    (:require [om.core :as om :include-macros true]
              [om-tools.core :as om-core :refer-macros [defcomponent]]
              [om-tools.dom :as dom :include-macros true]
              [open-company-web.components.link :refer [link]]))

(defcomponent list-page-item [data owner]
  (render [_]
    (dom/li
      (om/build link {:href (str "/companies/" (get data "symbol")) :name (get data "name")}))))

(defcomponent list-companies [data owner]
  (render [_]
    (let [company-list (:companies data)]
      (dom/div
        (dom/h1 "Companies:")
        (if (> (count company-list) 0)
          (dom/ul
            (om/build-all list-page-item company-list))
          (dom/h2 "No companies found."))))))
