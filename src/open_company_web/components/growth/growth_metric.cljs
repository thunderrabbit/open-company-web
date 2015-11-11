(ns open-company-web.components.growth.growth-metric
  (:require [om.core :as om :include-macros true]
            [om-tools.core :as om-core :refer-macros [defcomponent]]
            [om-tools.dom :as dom :include-macros true]
            [open-company-web.lib.utils :as utils]
            [open-company-web.components.charts :refer [column-chart]]
            [clojure.string :as clj-str]))

(defn get-columns-num [interval]
  (case interval
    "monthly" 12
    8))

(defn get-graph-tooltip [period label prefix value suffix]
  (str label
       ": "
       (or prefix "")
       (if value (.toLocaleString value) "")
       (if suffix (str " " suffix) "")))

(defn chart-data-at-index [data column-name columns-num prefix suffix has-target interval idx]
  (println "aaa" interval)
  (let [data (to-array data)
        rev-idx (- (dec columns-num) idx)
        obj (get data rev-idx)
        value (:value obj)
        target (or (:target obj) 0)
        label (get-graph-tooltip (:period obj) column-name prefix value suffix)
        target-label (get-graph-tooltip (:period obj) "target" prefix (.toLocaleString target) suffix)
        period (utils/get-period-string (:period obj) interval [:short])
        gray-color "fill-color: #DDDDDD"
        blue-color "fill-color: #109DB7"
        values (if has-target
                 [period
                  target
                  gray-color
                  target-label
                  value
                  blue-color
                  label]
                 [period
                  value
                  blue-color
                  label])]
    values))

(defn- get-past-period [period diff columns-num]
  (let [[year month] (clojure.string/split period "-")
        int-year (int year)
        int-month (int month)
        diff-month (- int-month diff)
        change-year (<= diff-month 0)
        fix-month (if change-year (+ columns-num diff-month) diff-month)
        fix-year (if change-year (dec int-year) int-year)]
    (str fix-year "-" (utils/add-zero fix-month))))

(defn placeholder-data [data columns-num]
  (if (>= (count data) columns-num)
    data
    (let [first-period (or (:period (last data)) (utils/current-period))
          rest-data (- columns-num (count data))
          diff (- columns-num (count data))
          plc-vec (vec (reverse (range rest-data)))
          vect (map (fn [n]
                      {:period (get-past-period first-period (- diff n) columns-num)
                       :slug (:slug first-period)
                       :value 0})
                    plc-vec)]
      (concat data vect))))

(defn- get-chart-data [data prefix column-name tooltip-suffix columns-num interval]
  "Vector of max *columns elements of [:Label value]"
  (println "aaab" interval)
  (let [fixed-data (placeholder-data data columns-num)
        has-target (some #(:target %) data)
        chart-data (partial chart-data-at-index fixed-data column-name columns-num prefix tooltip-suffix has-target interval)
        columns (if has-target
                  [["string" column-name]
                   ["number" "target"]
                   #js {"type" "string" "role" "style"}
                   #js {"type" "string" "role" "tooltip"}
                   ["number" column-name]
                   #js {"type" "string" "role" "style"}
                   #js {"type" "string" "role" "tooltip"}]
                  [["string" column-name]
                   ["number" column-name]
                   #js {"type" "string" "role" "style"}
                   #js {"type" "string" "role" "tooltip"}])
        mapper (vec (range columns-num))
        values (vec (map chart-data mapper))]
    { :prefix (if prefix prefix "")
      :columns columns
      :values values
      :pattern "###,###.##"
      :column-thickness (if has-target "28" "14")}))

(defn get-actual [metrics]
  (some #(when (:value (metrics %)) %) (vec (range (count metrics)))))

(defcomponent growth-metric [data owner]
  (render [_]
    (let [metric-info (:metric-info data)
          metric-data (:metric-data data)
          sort-pred (utils/sort-by-key-pred :period true)
          sorted-metric (vec (sort #(sort-pred %1 %2) metric-data))
          actual-idx (get-actual sorted-metric)
          actual-set (sorted-metric actual-idx)
          actual (.toLocaleString (:value actual-set))
          interval (:interval metric-info)
          period (utils/get-period-string (:period actual-set) interval)
          metric-unit (:unit metric-info)
          cur-unit (utils/get-symbol-for-currency-code metric-unit)
          fixed-cur-unit (if (= cur-unit metric-unit)
                            nil
                            cur-unit)
          unit (if fixed-cur-unit nil (utils/camel-case-str metric-unit))
          name-has-unit (> (.indexOf (clj-str/lower-case (str (:name metric-info))) (clj-str/lower-case metric-unit)) -1)
          actual-with-label (if fixed-cur-unit
                              (str fixed-cur-unit actual)
                              (if (and name-has-unit (> (:total-metrics data) 1))
                                (str actual)
                                (str actual (if (= unit "%") "" " ") unit)))
          target (if (:target actual-set) (.toLocaleString (:target actual-set)) nil)
          target-with-label (if fixed-cur-unit
                              (str fixed-cur-unit target)
                              (if (and name-has-unit (> (:total-metrics data) 1))
                                (str target)
                                (str target (if (= unit "%") "" " ") unit)))]
      (dom/div {:class (utils/class-set {:section true
                                         (:slug metric-info) true
                                         :read-only (:read-only data)})}
        (when (> (count metric-data) 0)
          (dom/div {}
            (om/build column-chart (get-chart-data sorted-metric
                                                   fixed-cur-unit
                                                   (:name metric-info)
                                                   unit
                                                   (get-columns-num interval)
                                                   interval))
            (dom/div {:class "chart-footer-container"}
              (dom/div {:class (utils/class-set {:target-actual-container true :double target})}
                (when target
                  (dom/div {:class "target-container"}
                    (dom/h3 {:class "target"} target-with-label)
                    (dom/h3 {:class "target-label"} "TARGET")))
                (dom/div {:class "actual-container"}
                  (dom/h3 {:class "actual"} actual-with-label)
                  (dom/h3 {:class "actual-label"} "ACTUAL"))))
            (dom/div {:class "chart-footer-container"}
              (dom/div {:class "period-container"}
                (dom/p {} period)))))))))