(ns oc.web.components.ui.custom-tag
  (:require [rum.core :as rum]
            [cuerdas.core :as string]
            [oops.core :refer (oget oget+ oset! ocall)]
            [oc.web.lib.utils :as utils]
            [oc.web.utils.dom :as dom-utils]
            [oc.web.lib.react-utils :as react-utils]
            ["react" :as react :refer (createElement PureComponent)]
            ["react-dom" :as react-dom :refer (findDOMNode)]
            [goog.events :as events]
            [goog.events.EventType :as EventType])
  (:use-macros [oc.web.macros :only [goog-extend]]))

(defn- user-display-name [user]
  (if (seq (oget user "?name"))
    (oget user "?name")
    (str (oget user "?first-name") " " (oget user "?last-name"))))

(defn- user-selected-display-value [user]
  (cond
    (and (= (oget user "?selectedKey") "slack-username")
         (oget user "?slack-username"))
    (oget user "?slack-username")
    (oget user "?name")
    (oget user "?name")
    (and (oget user "?first-name")
         (oget user "?last-name"))
    (str (oget user "?first-name") " " (oget user "?last-name"))
    (oget user "?first-name")
    (oget user "?first-name")
    (oget user "?last-name")
    (oget user "?last-name")
    :else
    (oget user "?email")))

(defn- user-slack-username [user]
  (or (oget user "?slack-username")
      (oget user "?slack-usernames.?0")))

(defn- subline-text [user]
  (oget user "?email"))

(defn list-item [props]
  (let [user (oget props "?user")
        avatar-style (clj->js {:backgroundImage (str "url(" (oget user "?avatar-url") ")")})
        display-name (user-display-name user)
        slack-username (user-slack-username user)
        alt-subline (subline-text user)]
    (createElement "div"
                   (clj->js {:key (str "user-" (oget user "?user-id"))
                             :className (string/join " " ["oc-mention-option"
                                                          (when (= (oget props "?selectedIndex") (oget props "?index"))
                                                            "active ")
                                                          (when (seq (oget user "?avatar-url"))
                                                            "has-avatar ")])
                             :data-name display-name
                             :data-user-id (oget user "?user-id")
                             :data-slack-username slack-username
                             :onMouseEnter (fn [e]
                                             (when (fn? (oget props "?hoverItem"))
                                               (ocall props "hoverItem" e (oget props "?index"))))
                             :onClick (fn [_]
                                        (when (fn? (oget props "?clickCb"))
                                          (ocall props "clickCb" user)))})
                   (createElement "div"
                                  (clj->js {:className "oc-mention-option-avatar"
                                            :style avatar-style}))
                   (createElement "div"
                                  (clj->js {:className "oc-mention-option-title"})
                                  display-name)
                   (createElement "div"
                                  (clj->js {:className (string/join " " ["oc-mention-option-subline"
                                                                         (when (seq slack-username)
                                                                           "slack-icon")])})
                                  (or slack-username alt-subline)))))

(defn- value-lookup [value search-value]
  (when (seq value)
    (some #(string/index-of % (string/lower search-value))
          (-> value
              (string/lower)
              (string/split #"\s")))))

(defn- check-slack-usernames [user current-text]
  (some (fn [slack-username]
          (when (value-lookup slack-username current-text)
            (js/Object.assign user #js {:selectedKey "slack-username"
                                        :slack-username slack-username})))
        (oget user "?slack-usernames")))

(defn- arrow-keys [that e options index-fn index-default]
  (if-let [selected-index (oget that "?state.?selectedIndex")]
    (that.setState (clj->js {"selectedIndex" (mod (index-fn selected-index) (oget options "?length"))}))
    (that.setState (clj->js {"selectedIndex" index-default})))
  (utils/event-stop e))

(defn- select-current [that e]
  (when-let [selected-index (oget that "state.?selectedIndex")]
    (let [user (-> (that.filterUsers (oget that "?props"))
                   vec
                   (nth selected-index))]
      (ocall that "selectItem" user)))
  (utils/event-stop e))

(js* "/** @nocollapse */")
(goog-extend ^js CustomizedTagComponent
             PureComponent
             ([props]
              (this-as this
                       (goog/base this props)
                       (oset! this "!state" (clj->js {"selectedIndex" 0}))
                       ;; Replace the inherited functions with one bounded to the current this instead of the parent
                       (oset! this "setState" (.bind (oget this "setState") this))
                       (oset! this "componentDidMount" (.bind (oget this "componentDidMount") this))
                       (oset! this "componentWillUnmount" (.bind (oget this "componentWillUnmount") this))
                       (oset! this "addBindedEvent" (.bind (oget this "addBindedEvent") this))
                       (oset! this "removeBindedEvent" (.bind (oget this "removeBindedEvent") this))
                       (oset! this "keyPress" (.bind (oget this "keyPress") this))
                       (oset! this "hoverItem" (.bind (oget this "hoverItem") this))
                       (oset! this "selectItem" (.bind (oget this "selectItem") this))
                       (oset! this "!hidePanel" (.bind (oget this "hidePanel") this))
                       this))
             (hidePanel []
                        (this-as this
                                 (let [hide-panel-fn (oget this "props.?hidePanel")]
                                   (when (fn? hide-panel-fn)
                                     (hide-panel-fn)))))
             (render []
                     (this-as this
                              ;; (goog/base (js* "this") "render")
                              (let [filtered-users (this.filterUsers (oget this "props"))]
                                (createElement "div"
                                               #js {:className "oc-mention-options"
                                                    :contentEditable false}
                                               (createElement "div"
                                                              #js {:className "oc-mention-options-list"}
                                                              (clj->js (mapv (fn [idx]
                                                                               (let [user (get filtered-users idx)]
                                                                                 (list-item #js {:user user
                                                                                                 :index idx
                                                                                                 :selectedIndex (oget this "?state.?selectedIndex")
                                                                                                 :clickCb (.bind (oget this "?selectItem") this)
                                                                                                 :hoverItem (.bind (oget this "?hoverItem") this)})))
                                                                             (range (count filtered-users)))))))))
             (filterUsers [properties]
                          (this-as this
                                  ;;  (goog/base (js* "this") "filterUsers" properties)
                                   (let [current-mention-text (oget properties "currentMentionText")
                                         current-text (string/lower (subs current-mention-text 1 (count current-mention-text)))
                                         mapped-users (map (fn [user-index]
                                                             (let [user (oget+ properties (str "?users.?" user-index))]
                                                               (cond
                                                                 (value-lookup (oget user "?name") current-text)
                                                                 (js/Object.assign user #js {:selectedKey "name"})
                                                                ;;  (oset! user "selectedKey" "name")
                                                                 (value-lookup (oget user "?first-name") current-text)
                                                                 (js/Object.assign user #js {:selectedKey "first-name"})
                                                                ;;  (oset! user "selectedKey" "first-name")
                                                                 (value-lookup (oget user "?last-name") current-text)
                                                                 (js/Object.assign user #js {:selectedKey "last-name"})
                                                                ;;  (oset! user "selectedKey" "last-name")
                                                                 (and (seq (oget user "?slack-usernames"))
                                                                      (check-slack-usernames user current-text))
                                                                 (check-slack-usernames user current-text)
                                                                 (value-lookup (oget user "?email") current-text)
                                                                 (js/Object.assign user #js {:selectedKey "email"})
                                                                ;;  (oset! user "selectedKey" "email")
                                                                 :else
                                                                 user)))
                                                           (range (oget properties "?users.?length")))]
                                     (filterv (fn [user] (seq (oget user "?selectedKey"))) mapped-users))))
             (addBindedEvent [el e-name cb]
                             (this-as this
                                      (events/listen el e-name cb)))
             (removeBindedEvent [el e-name cb]
                                (this-as this
                                         (events/unlisten el e-name cb)))
             (keyPress [e]
               (this-as this
                        ;; (goog/base (js* "this") "keyPress" e)
                        (let [event (or e (oget js/window "event"))
                              node (findDOMNode this)
                              options (when node (ocall node "querySelectorAll" ".oc-mention-option"))]
                          (when (and (not (dom-utils/is-hidden node))
                                     (seq options))
                            (case (oget event "?keyCode")
                              (39 40) (arrow-keys this event options inc 0) ;; Right and down arrow
                              (37 38) (arrow-keys this event options dec (dec (oget options "?length"))) ;; Left and up arrow
                              13      (select-current this event) ;; Enter
                              9       (select-current this event) ;; Tab
                              27      (this.hidePanel) ;; Esc
                              this ;; :else
                              )))))

             (componentDidMount []
               (this-as this
                        ;; (goog/base (js* "this") "componentDidMount")
                        (this.addBindedEvent js/window EventType/KEYDOWN (oget this "keyPress"))))

             (componentWillUnmount []
               (this-as this
                        ;; (goog/base (js* "this") "componentWillUnmount")
                        (this.removeBindedEvent js/window EventType/KEYDOWN (oget this "keyPress"))))

             (hoverItem [e idx]
               (this-as this
                        ;; (goog/base (js* "this") "hoverItem" e idx)
                        (this.setState (clj->js {"selectedIndex" idx}))
                        (utils/event-stop e)))

             (selectItem [user]
               (this-as this
                        ;; (goog/base (js* "this") "selectItem" user)
                        (let [select-mention-cb (oget this "props.selectMentionCallback")
                              selected-value (user-selected-display-value user)]
                          (when (fn? select-mention-cb)
                            (select-mention-cb (str "@" selected-value) user))))))