(ns test.open-company-web.components.finances.revenue
    (:require [cljs.test :refer-macros [deftest async testing is are use-fixtures]]
              [cljs-react-test.simulate :as sim]
              [cljs-react-test.utils :as tu]
              [om.core :as om :include-macros true]
              [dommy.core :as dommy :refer-macros [sel1 sel]]
              [open-company-web.components.finances.revenue :refer [revenue]]
              [om.dom :as dom :include-macros true]
              [open-company-web.data.company :refer (company)]))

(enable-console-print!)

; dynamic mount point for components
(def ^:dynamic c)

(deftest test-revenue-component
  (testing "Revenue component"
    (let [c (tu/new-container!)
          app-state (atom company)
          _ (om/root revenue app-state {:target c})
          chart-node (sel1 c [:div.section.revenue])]
      (is (not (nil? chart-node)))
      (tu/unmount! c))))