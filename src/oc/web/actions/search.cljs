(ns oc.web.actions.search
  (:require [taoensso.timbre :as timbre]
            [oc.web.api :as api]
            [oc.web.lib.jwt :as jwt]
            [oc.web.router :as router]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.cookies :as cook]
            [oc.web.dispatcher :as dispatcher]
            [oc.web.actions.cmail :as cmail-actions]        
            [oc.web.components.ui.alert-modal :as alert-modal]))


(defn query-finished
  [result]
  (dispatcher/dispatch! [:search-query/finish result]))

(defn reset []
  (dispatcher/dispatch! [:search-reset]))

(defn inactive []
  (dispatcher/dispatch! [:search-inactive]))

(defn active []
  (dispatcher/dispatch! [:search-active]))

(def search-history-cookie (str "search-history-" (jwt/user-id)))

(def search-history-length 5)

(defn search-history []
  (let [res (cook/get-cookie search-history-cookie)]
    (if (seq res)
      (set (reverse (take search-history-length (reverse (js->clj (.parseJSON js/$ res))))))
      #{})))

(defn query-change [search-query]
  (dispatcher/dispatch! [:search-query/change search-query]))

(defn query
  "Use the search service to query for results."
  [search-query]
  (let [trimmed-query (utils/trim search-query)]
    (if (seq trimmed-query)
      (do
        (cook/set-cookie! search-history-cookie (-> (search-history) (disj trimmed-query) (conj trimmed-query) clj->js js/JSON.stringify)
          cook/default-cookie-expire)
        (active)
        (dispatcher/dispatch! [:search-query/start trimmed-query])
        (api/query (:uuid (dispatcher/org-data)) trimmed-query query-finished))
      (reset))))

(defn result-clicked [entry-result url]
  (let [post-loaded? (dispatcher/activity-data (:uuid entry-result))
        open-post-cb (fn [success status]
                       (if success
                        (do
                          (dispatcher/dispatch! [:search-result-clicked])
                          (utils/after 10 (router/nav! url)))
                        (let [is-404? (= status 404)
                              alert-data {:icon "/img/ML/trash.svg"
                                          :action "search-result-load-failed"
                                          :title (if is-404? "Post moved or deleted" "An error occurred")
                                          :message (if is-404?
                                                     "The selected post was moved to another section or deleted."
                                                     "An error occurred while loading the selected post. Please try again.")
                                          :solid-button-style :red
                                          :solid-button-title "Ok"
                                          :solid-button-cb alert-modal/hide-alert}]
                          (alert-modal/show-alert alert-data))))]
    (if post-loaded?
      (open-post-cb true nil)
      (cmail-actions/get-entry-with-uuid (:board-slug entry-result) (:uuid entry-result) open-post-cb))))