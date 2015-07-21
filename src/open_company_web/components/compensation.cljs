(ns open-company-web.components.compensation
    (:require [om.core :as om :include-macros true]
              [om-tools.core :as om-core :refer-macros [defcomponent]]
              [om-tools.dom :as dom :include-macros true]
              [open-company-web.components.report-line :refer [report-line report-editable-line]]
              [open-company-web.utils :refer [thousands-separator handle-change get-symbols-for-currency-code]]
              [open-company-web.components.comment :refer [comment-component]]
              [open-company-web.components.pie-chart :refer [pie-chart]]
              [goog.string :as gstring]))

(defn get-chart-data [data head-data symbol]
  (let [show-founders (> (get head-data "founders") 0)
        show-executives (> (get head-data "executives") 0)
        show-employees (> (+ (get head-data "ft-employees") (get head-data "pt-employees")) 0)
        show-contrators (> (+ (get head-data "ft-contractors") (get head-data "pt-contractors")) 0)]
    { :symbol symbol
      :columns [["string" "Compensation"] ["number" "Amount"]]
      :values [["Founders" (if show-founders (get data "founders") 0)]
              ["Executives" (if show-executives (get data "executives") 0)]
              ["Employees" (if show-employees (get data "employees") 0)]
              ["Contractors" (if show-contrators (get data "contractors") 0)]]}))

(defn switch-values
  "Update all the values in the cursor with the values passed"
  [cursor last-values]
  (handle-change cursor (get last-values "founders") "founders")
  (handle-change cursor (get last-values "executives") "executives")
  (handle-change cursor (get last-values "employees") "employees")
  (handle-change cursor (get last-values "contractors") "contractors"))

(defn calc-percentage
  [dollar total]
  (let [perc (gstring/format "%.2f" (* (/ dollar total) 100))]
    (js/parseFloat perc)))

(defn dollars->percentage
  "Calculate the percentage given the compensation component cursor"
  [comp-data head-data]
  (let [show-founders (> (get head-data "founders") 0)
        show-executives (> (get head-data "executives") 0)
        show-employees (> (+ (get head-data "ft-employees") (get head-data "pt-employees")) 0)
        show-contrators (> (+ (get head-data "ft-contractors") (get head-data "pt-contractors")) 0)
        founders (if show-founders (get comp-data "founders") 0)
        executives (if show-executives (get comp-data "executives") 0)
        employees (if show-employees (get comp-data "employees") 0)
        contractors (if show-contrators (get comp-data "contractors") 0)
        total (+ founders executives employees contractors)]
    (handle-change comp-data (calc-percentage founders total) "founders")
    (handle-change comp-data (calc-percentage executives total) "executives")
    (handle-change comp-data (calc-percentage employees total) "employees")
    (handle-change comp-data (calc-percentage contractors total) "contractors")))

(defn copy-compensation-state
  "Copy the compensation state into the component state"
  [owner cursor]
  (when (not (:percentage cursor))
    (om/set-state! owner :initial-values cursor)))

(defcomponent compensation [data owner]
  (will-mount [_]
    (copy-compensation-state owner (get data "compensation")))
  (render [_]
    (let [head-data (get data "headcount")
          show-founders (> (get head-data "founders") 0)
          show-executives (> (get head-data "executives") 0)
          show-employees (> (+ (get head-data "ft-employees") (get head-data "pt-employees")) 0)
          show-contrators (> (+ (get head-data "ft-contractors") (get head-data "pt-contractors")) 0)
          comp-data (get data "compensation")
          percentage (get comp-data "percentage")
          founders (if show-founders (get comp-data "founders") 0)
          executives (if show-executives (get comp-data "executives") 0)
          employees (if show-employees (get comp-data "employees") 0)
          contractors (if show-contrators (get comp-data "contractors") 0)
          currency (get data "currency")
          currency-symbol (get-symbols-for-currency-code currency)
          prefix (str (if percentage "%" currency-symbol) " ")
          comment (get comp-data "comment")
          founders-label (str "founder" (if (= (get head-data "founders") 1) "" "s") " compensation this month")
          executives-label (str "executive" (if (= (get head-data "executives") 1) "" "s") " compensation this month")
          employees-label (str "employee" (if (= (get head-data "employees") 1) "" "s") " compensation this month")
          contractors-label (str "contractor" (if (= (get head-data "contractors") 1) "" "s") " compensation this month")
          total-compensation 0
          total-compensation (+ (if show-founders founders 0) total-compensation)
          total-compensation (+ (if show-executives executives 0) total-compensation)
          total-compensation (+ (if show-employees employees 0) total-compensation)
          total-compensation (+ (if show-contrators contractors 0) total-compensation)
          total-compensation (gstring/format "%.2f" total-compensation)]
      (dom/div {:class "report-list compensation clearfix"}
        (dom/div {:class "report-list-left"}
          (dom/h3 "Compensation: ")
          (dom/div
            (dom/span {:class "label"} "Report in: "
              (dom/input {
                :type "radio"
                :name "report-type"
                :value currency
                :id "report-type-$"
                :checked (not percentage)
                :on-click (fn[e] (switch-values comp-data (:initial-values (om/get-state owner)))
                            (handle-change comp-data false "percentage"))})
              (dom/label {:class "switch-vis" :for "report-type-$"} (str " " currency-symbol "  "))
              (dom/input {
                :type "radio"
                :name "report-type"
                :value "%"
                :id "report-type-%"
                :checked percentage
                :on-click (fn[e] (handle-change comp-data true "percentage")
                                 (copy-compensation-state owner comp-data)
                                 (dollars->percentage comp-data head-data))})
              (dom/label {:class "switch-vis" :for "report-type-%"} "  Percent ")))
          (when show-founders
            (om/build report-editable-line {
              :cursor comp-data
              :key "founders"
              :prefix prefix
              :label founders-label
              :pluralize false
              :on-change #(when (not percentage) (om/set-state! owner :initial-values comp-data))}))
          (when show-executives
            (om/build report-editable-line {
              :cursor comp-data
              :key "executives"
              :prefix prefix
              :label executives-label
              :pluralize false
              :on-change #(when (not percentage) (om/set-state! owner :initial-values comp-data))}))
          (when show-employees
            (om/build report-editable-line {
              :cursor comp-data
              :key "employees"
              :prefix prefix
              :label employees-label
              :pluralize false
              :on-change #(when (not percentage) (om/set-state! owner :initial-values comp-data))}))
          (when show-contrators
            (om/build report-editable-line {
              :cursor comp-data
              :key "contractors"
              :prefix prefix
              :label contractors-label
              :pluralize false
              :on-change #(when (not percentage) (om/set-state! owner :initial-values comp-data))}))
          (dom/div
            (om/build report-line {
              :prefix prefix
              :number (thousands-separator total-compensation)
              :label "total compensation this month"}))
          (om/build comment-component {:value comment}))
        (om/build pie-chart (get-chart-data comp-data head-data prefix))))))
