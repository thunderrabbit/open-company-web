(ns ^:figwheel-always open-company-web.core
  (:require [om.core :as om :include-macros true]
            [secretary.core :as secretary :refer-macros (defroute)]
            [dommy.core :refer-macros (sel1)]
            [open-company-web.router :as router]
            [open-company-web.components.page :refer (company)]
            [open-company-web.components.company-editor :refer (company-editor)]
            [open-company-web.components.company-dashboard :refer (company-dashboard)]
            [open-company-web.components.list-companies :refer (list-companies)]
            [open-company-web.components.page-not-found :refer (page-not-found)]
            [open-company-web.components.user-profile :refer (user-profile)]
            [open-company-web.components.login :refer (login)]
            [open-company-web.components.ui.loading :refer (loading)]
            [open-company-web.lib.raven :refer (raven-setup)]
            [open-company-web.lib.utils :as utils]
            [open-company-web.actions]
            [open-company-web.dispatcher :refer (app-state)]
            [open-company-web.api :as api]
            [goog.events :as events]
            [open-company-web.lib.cookies :as cook]
            [open-company-web.local-settings :as ls]
            [open-company-web.lib.jwt :as jwt]
            [goog.history.EventType :as HistoryEventType]
            [goog.events.EventType :as EventType]))

(enable-console-print!)

(defonce prevent-route-dispatch (atom false))

;; setup Sentry error reporting
(defonce raven (raven-setup))

(defn check-get-params [query-params]
  (when (contains? query-params :browser-type)
    ; if :browser-type is "mobile" the mobile site is forced
    ; any other value will be set as big web
    ; remove the cookie to let it calculate the type of site
    ; Rules set via css won't be affected by this
    (cook/set-cookie! :force-browser-type (:browser-type query-params) (* 60 60 24 6))))

(defn inject-loading []
  (let [target (sel1 [:div#oc-loading])]
    (om/root loading app-state {:target target})))

(defn pre-routing [query-params]
 (check-get-params query-params)
 (inject-loading))

;; Company list
(defn home-handler [target params]
  (pre-routing (:query-params params))
  ;; clean the caches
  (utils/clean-company-caches)
  ;; save route
  (router/set-route! ["companies"] {})
  ;; load data from api
  (swap! app-state assoc :loading true)
  (api/get-entry-point)
  (api/get-companies)
  ;; render component
  (om/root list-companies app-state {:target target}))

;; Handle successful and unsuccessful logins
(defn login-handler [target params]
  (pre-routing (:query-params params))
  (utils/clean-company-caches)
  (if (contains? (:query-params params) :jwt)
    (do ; contains :jwt so auth went well
      (cook/set-cookie! :jwt (:jwt (:query-params params)) (* 60 60 24 60) "/" ls/jwt-cookie-domain ls/jwt-cookie-secure)
      ;; redirect to dashboard
      (if-let [login-redirect (cook/get-cookie :login-redirect)]
        (do
          ;; remove the login redirect cookie
          (cook/remove-cookie! :login-redirect)
          ;; redirect to the initial path
          (utils/redirect! login-redirect))
        ;; redirect to / if no cookie is set
        (utils/redirect! "/")))
    (do
      (when (contains? (:query-params params) :login-redirect)
        (cook/set-cookie! :login-redirect (:login-redirect (:query-params params)) (* 60 60) "/" ls/jwt-cookie-domain ls/jwt-cookie-secure))
      ;; save route
      (router/set-route! ["login"] {})
      (swap! app-state assoc :loading true)
      (when (contains? (:query-params params) :access)
        ;login went bad, add the error message to the app-state
        (swap! app-state assoc :access (:access (:query-params params))))
      ;; render component
      (om/root login app-state {:target target}))))

;; Component specific to a company
(defn company-handler [route target component params]
  (let [slug (:slug (:params params))
        query-params (:query-params params)]
    (pre-routing query-params)
    (utils/clean-company-caches)
    ;; save the route
    (router/set-route! ["companies" slug route] {:slug slug :query-params query-params})
    ;; do we have the company data already?
    (when-not (contains? @app-state (keyword slug))
      ;; load the company data from the API
      (api/get-company slug)
      (swap! app-state assoc :loading true))
    ;; render component
    (om/root component app-state {:target target})))

;; Routes - Do not define routes when js/document#app
;; is undefined because it breaks tests
(if-let [target (sel1 :div#app)]
  (do

    (defroute login-route "/login" {:as params}
      (login-handler target params))

    (defroute home-page-route "/" {:as params}
      (home-handler target params))

    (defroute company-create-route "/create-company" {:as params}
      (pre-routing (:query-params params))
      (om/root company-editor app-state {:target target}))

    (defroute list-page-route "/companies" {:as params}
      (home-handler target params))

    (defroute list-page-route-slash "/companies/" {:as params}
      (home-handler target params))

    (defroute user-profile-route "/profile" {:as params}
      (utils/clean-company-caches)
      (pre-routing (:query-params params))
      (om/root user-profile app-state {:target target}))

    (defroute company-route "/:slug" {:as params}
      (company-handler nil target company params))

    (defroute company-profile-route "/:slug/profile" {:as params}
      (company-handler "profile" target company params))

    (defroute company-dashboard-route "/:slug/dashboard" {:as params}
      (company-handler "dashboard" target company-dashboard params))


    (defroute not-found-route "*" []
      ;; render component
      (om/root page-not-found app-state {:target target}))

    (def route-dispatch!
      (secretary/uri-dispatcher [login-route
                                 home-page-route
                                 list-page-route-slash
                                 list-page-route
                                 company-create-route
                                 user-profile-route
                                 company-route
                                 company-profile-route
                                 company-dashboard-route
                                 not-found-route]))

    (defn login-wall []
      ;; load the login settings from auth server
      ;; if the user is not logged in yet
      (when-not (or (jwt/jwt)
                    (contains? @app-state :auth-settings))
        (api/get-auth-settings)))

    (defn handle-url-change [e]
      (when-not @prevent-route-dispatch
        ;; we are checking if this event is due to user action,
        ;; such as click a link, a back button, etc.
        ;; as opposed to programmatically setting the URL with the API
        (when-not (.-isNavigation e)
          ;; in this case, we're setting it so
          ;; let's scroll to the top to simulate a navigation
          (js/window.scrollTo 0 0))
        ; check if the user is logged in
        (login-wall)
        ;; dispatch on the token
        (route-dispatch! (router/get-token))))))

(defonce history
  (doto (router/make-history)
    (events/listen HistoryEventType/NAVIGATE
      ;; wrap in a fn to allow live reloading
      handle-url-change)
    (.setEnabled true)))

(defonce resize-listener
  ; define this inside a defonce to avoid multiple listener during development
  (events/listen
    js/window
    EventType/RESIZE
    (fn [e]
      (utils/set-browser-type!)
      (route-dispatch! (router/get-token)))))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;; (swap! app-state update-in [:__figwheel_counter] inc)
  (.clear js/console)
  (route-dispatch! (router/get-token)))