(ns oc.web.actions.notifications
  (:require [oc.web.dispatcher :as dis]
            [oc.lib.cljs.useragent :as ua]))

;; Default time to disappeara notification
(def default-expiration-time 3)

(defn- potentially-show-desktop-notification!
  [{:keys [title click]}]
  (when (and ua/desktop-app?
             (not (js/window.OCCarrotDesktop.windowHasFocus)))
    (let [notif (js/Notification. title)]
      (set! (.-onclick notif) #(do (js/window.OCCarrotDesktop.showDesktopWindow)
                                   (click))))))

(defn show-notification [notification-data]
  (let [expiration-time (or (:expire notification-data) default-expiration-time)
        fixed-notification-data (-> notification-data
                                 (assoc :created-at (.getTime (js/Date.)))
                                 (assoc :expire expiration-time))]
    (potentially-show-desktop-notification! fixed-notification-data)
    (dis/dispatch! [:notification-add fixed-notification-data])))

(defn remove-notification [notification-data]
  (dis/dispatch! [:notification-remove notification-data]))

(defn remove-notification-by-id [notification-id]
  (dis/dispatch! [:notification-remove-by-id notification-id]))

(defn show-mobile-user-notifications []
  (dis/dispatch! [:input [:mobile-user-notifications] true]))

(defn hide-mobile-user-notifications []
  (dis/dispatch! [:input [:mobile-user-notifications] false]))

(defn toggle-mobile-user-notifications []
  (dis/dispatch! [:update [:mobile-user-notifications] not]))