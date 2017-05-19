(ns oc.web.components.reactions
  (:require-macros [dommy.core :refer (sel1)]
                   [if-let.core :refer (when-let*)])
  (:require [rum.core :as rum]
            [oc.web.dispatcher :as dis]
            [oc.web.lib.utils :as utils]
            [cljsjs.web-animations]))

(defn animate-reaction [e s]
  (when-let* [target (.-currentTarget e)
              span-reaction (sel1 target :span.reaction)]
    (doseq [i (range 8)]
      (let [cloned-el (.cloneNode span-reaction true)
            translate-y {:transform ["translateY(0px)" "translateY(-80px)"]
                         :opacity [1 0]}
            v (+ 7 (* 3 (int (rand 4))))]
        (set! (.-opacity (.-style cloned-el)) 0)
        (set! (.-position (.-style cloned-el)) "absolute")
        (set! (.-left (.-style cloned-el)) (str v "px"))
        (set! (.-top (.-style cloned-el)) "2px")
        (.appendChild (.-parentElement span-reaction) cloned-el)
        (.animate cloned-el (clj->js translate-y) (clj->js {:duration 800 :delay (* 150 i) :fill "forwards" :easing "ease-out"}))))))

(rum/defcs reactions
  [s topic-slug entry-uuid reactions-data]
  [:div.reactions
    (for [idx (range (count reactions-data))
          :let [r (get reactions-data idx)]]
      [:button.reaction-btn.btn-reset
        {:key (str "topic-" topic-slug "-entry-" entry-uuid "-" idx)
         :class (if (:reacted r) "reacted" "")
         :on-click (fn [e] (animate-reaction e s) ; (dis/dispatch! [:reaction-add topic-slug entry-uuid r])
                    )}
        [:span.reaction (:reaction r)]
        [:span.count (:count r)]])])
