(ns oc.web.stores.org
  (:require [taoensso.timbre :as timbre]
            [oc.web.lib.utils :as utils]
            [oc.web.local-settings :as ls]
            [oc.web.lib.jwt :as jwt]
            [oc.web.utils.org :as org-utils]
            [oc.web.utils.activity :as activity-utils]
            [oc.web.utils.user :as user-utils]
            [oc.web.dispatcher :as dispatcher]
            [oc.web.actions.cmail :as cmail-actions]
            [oc.web.stores.user :as user-store]))

(def private-board-tooltip "Posting to private topics is part of the Premium plan.")
(def public-board-tooltip "Posting to public topics is part of the Premium plan.")

(defmethod dispatcher/action :org-loaded
  [db [_ org-data saved? email-domain]]
  ;; We need to remove the boards that are no longer in the org
  (let [fixed-org-data (activity-utils/parse-org db org-data)
        org-data-key (dispatcher/org-data-key (:slug fixed-org-data))
        boards-key (dispatcher/boards-key (:slug fixed-org-data))
        old-boards (get-in db boards-key)
        ;; No need to add a spacial case for drafts board here since
        ;; we are only excluding keys that already exists in the app-state
        board-slugs (set (mapv #(keyword (str (:slug %))) (:boards org-data)))
        filter-board (fn [[k _]]
                       (board-slugs k))
        next-boards (into {} (filter filter-board old-boards))
        with-saved? (if (nil? saved?)
                      ;; If saved? is nil it means no save happened, so we keep the old saved? value
                      fixed-org-data
                      ;; If save actually happened let's update the saved value
                      (assoc fixed-org-data :saved saved?))
        next-org-editing (-> with-saved?
                          (assoc :email-domain email-domain)
                          (update :new-entry-cta #(or % org-utils/default-entry-cta))
                          (update :new-entry-placeholder #(or % org-utils/default-entry-placeholder))
                          (dissoc :has-changes))
        editable-boards* (filterv #(and (not (:draft %)) (utils/link-for (:links %) "create" "POST"))
                          (:boards org-data))
        editable-boards (zipmap (map :slug editable-boards*) editable-boards*)
        premium? (jwt/premium? (:team-id org-data))
        user-id (jwt/user-id)
        private-boards* (when-not premium?
                          ;; All private boards even if they have no create link but the user is an author, exclude drafts board
                          (filterv #(and (= (:access %) "private")
                                         (not (utils/link-for (:links %) "create" "POST"))
                                         (not= (:slug %) utils/default-drafts-board-slug)
                                         ((set (:authors %)) user-id))
                                   (:boards org-data)))
        private-boards (zipmap (map :slug private-boards*) (map #(assoc % :premium-lock private-board-tooltip) private-boards*))
        public-boards* (when-not premium?
                         ;; All public boards even if they have no create link but we know the user could post to
                         (filterv #(and (= (:access %) "public")
                                        (not (utils/link-for (:links %) "create" "POST"))
                                        ((set (:authors org-data)) user-id))
                                  (:boards org-data)))
        public-boards (zipmap (map :slug public-boards*) (map #(assoc % :premium-lock public-board-tooltip) public-boards*))
        current-board-slug (dispatcher/current-board-slug)
        editing-board (when editable-boards*
                        (cmail-actions/get-board-for-edit (when-not (dispatcher/is-container? current-board-slug) current-board-slug) editable-boards*))
        ;; Active users
        active-users (dispatcher/active-users (:slug fixed-org-data) db)
        ;; Follow/Unfollow boards/users
        follow-boards-list-key (dispatcher/follow-boards-list-key (:slug fixed-org-data))
        unfollow-board-uuids (get-in db (dispatcher/unfollow-board-uuids-key (:slug fixed-org-data)))
        follow-publishers-list (dispatcher/follow-publishers-list (:slug fixed-org-data) db)
        ;; Team data
        team-data-key (dispatcher/team-data-key (:team-id fixed-org-data))
        update-team-users? (contains? (get-in db team-data-key) :users)
        ;; Roster data
        team-roster-key (dispatcher/team-roster-key (:team-id fixed-org-data))
        update-roster-users? (contains? (get-in db team-roster-key) :users)
        ;; Cmail data
        setup-cmail? (and (not (contains? db (first dispatcher/cmail-state-key)))
                          editing-board)]
    (as-> db ndb
     (assoc-in ndb org-data-key fixed-org-data)
     (assoc-in ndb (dispatcher/editable-boards-key (:slug org-data)) editable-boards)
     (assoc-in ndb (dispatcher/private-boards-key (:slug org-data)) private-boards)
     (assoc-in ndb (dispatcher/public-boards-key (:slug org-data)) public-boards)
     (assoc ndb :org-editing next-org-editing)
     (assoc ndb :org-avatar-editing (select-keys fixed-org-data [:logo-url]))
     (update ndb :current-user-data #(user-store/parse-user-data % fixed-org-data active-users))
     (if setup-cmail?
       (assoc-in ndb dispatcher/cmail-state-key {:key (utils/activity-uuid)
                                                 :fullscreen false
                                                 :collapsed true})
       ndb)
     (if setup-cmail?
        (update-in ndb dispatcher/cmail-data-key merge editing-board)
        ndb)
     (if update-team-users?
       (update-in ndb (conj team-data-key :users) #(user-store/parse-users % fixed-org-data follow-publishers-list))
       ndb)
     (if update-roster-users?
       (update-in ndb (conj team-roster-key :users) #(user-store/parse-users % fixed-org-data follow-publishers-list))
       ndb)
     (assoc-in ndb boards-key next-boards)
     (update-in ndb follow-boards-list-key #(user-store/enrich-boards-list unfollow-board-uuids (:boards fixed-org-data))))))

(defmethod dispatcher/action :org-edit-save
  [db [_]]
  (update db :org-editing #(dissoc % :saved :error)))

(defmethod dispatcher/action :org-avatar-update/failed
  [db [_]]
  (let [org-data (dispatcher/org-data db)]
    (assoc db :org-avatar-editing (select-keys org-data [:logo-url]))))

(defmethod dispatcher/action :org-create
  [db [_]]
  (dissoc db
   :latest-entry-point
   :latest-auth-settings
   ;; Remove the entry point, orgs and auth settings
   ;; to avoid using the old loaded orgs
   (first dispatcher/api-entry-point-key)
   (first dispatcher/auth-settings-key)
   dispatcher/orgs-key))

(defmethod dispatcher/action :org-edit-setup
  [db [_ org-data]]
  (assoc db :org-editing (-> org-data
                             (dissoc :has-changes)
                             (update :brand-color #(or % ls/default-brand-color)))))

(defmethod dispatcher/action :bookmarks-nav/show
  [db [_ org-slug]]
  (let [bookmarks-count-key (vec (conj (dispatcher/org-data-key org-slug) :bookmarks-count))]
    (update-in db bookmarks-count-key #(or % 0))))

(defmethod dispatcher/action :drafts-nav/show
  [db [_ org-slug]]
  (let [drafts-count-key (vec (conj (dispatcher/org-data-key org-slug) :drafts-count))]
    (update-in db drafts-count-key #(or % 0))))

(defmethod dispatcher/action :maybe-badge-following
  [db [_ org-slug current-board-slug]]
  (let [badges-key (dispatcher/badges-key org-slug)
        badges-data (get-in db badges-key)]
    (if (and (or (not badges-data)
                 (not (contains? badges-data :following)))
             (not= (keyword current-board-slug) :following))
      (assoc-in db (conj badges-key :following) true)
      db)))

(defmethod dispatcher/action :maybe-badge-replies
  [db [_ org-slug current-board-slug]]
  (let [badges-key (dispatcher/badges-key org-slug)
        badges-data (get-in db badges-key)]
    (if (and (or (not badges-data)
                 (not (contains? badges-data :replies)))
             (not= (keyword current-board-slug) :replies))
      (assoc-in db (conj badges-key :replies) true)
      db)))