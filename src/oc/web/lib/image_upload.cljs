(ns oc.web.lib.image-upload
  (:require [oc.web.local-settings :as ls]
            [cljsjs.filestack]
            [goog.object :as gobj]
            [oc.web.lib.raven :as sentry]))

(def _fs (atom nil))

(defn init-filestack []
  (if @_fs
    @_fs
    (let [new-fs (.init js/filestack ls/filestack-key)]
      (reset! _fs new-fs)
      new-fs)))

(def store-to
  {:container ls/attachments-bucket
   :region "us-east-1"
   :location "s3"})

(defn upload!
  [type success-cb progress-cb error-cb & [finished-cb selected-cb started-cb]]
  (let [from-sources (if (= type "image/*")
                        ["local_file_system" "imagesearch" "googledrive" "dropbox" "onedrive" "box"]
                        ["local_file_system" "googledrive" "dropbox" "onedrive" "box"])
        base-config   {:maxFiles 1
                       :maxSize (* 20 1024 1024) ; Limit the uploaded file to be at most 20MB
                       :storeTo store-to
                       :transformOptions {
                         :transformations {
                           :crop true
                           :rotate true
                           :circle true
                         }
                       }
                       :fromSources from-sources
                       ;; Selected cb
                       :onFileSelected (fn [res]
                         (when (fn? selected-cb)
                           (selected-cb res)))
                       ;; Started cb
                       :onFileUploadStarted (fn [res]
                         (when (fn? started-cb)
                           (started-cb res)))
                       ;; Progress cb
                       :onFileUploadProgress (fn [res progress]
                         (when (fn? progress-cb)
                            (progress-cb res progress)))
                       ;; Finished cb
                       :onFileUploadFinished (fn [res]
                         (when (fn? finished-cb)
                           (finished-cb res)))
                       ;; Error cb
                       :onFileUploadFailed error-cb}
        config        (if type
                        (merge base-config {:accept type})
                        base-config)
        fs (init-filestack)]
    (.then
      (.pick fs
        (clj->js config))
      (fn [res]
        (let [files-uploaded (gobj/get res "filesUploaded")]
          (when (= (count files-uploaded)1)
            (success-cb (get files-uploaded 0))))))))

(defn thumbnail [fs-url & [success-cb error-cb]]
  (let [fs-client (init-filestack)
        opts (clj->js {:resize {
                        :fit "crop"
                        :width 72
                        :height 72
                        :align "faces"}})
       transformed-url (.transform fs-client fs-url opts)
       storing-task (.storeURL fs-client transformed-url (clj->js store-to))]
    (try
      (.then storing-task
        (fn [res]
          (let [url (gobj/get res "url")]
            (when (fn? success-cb)
              (success-cb url)))))
      (catch :default e
        (sentry/capture-error e)
        (when (fn? error-cb)
          (error-cb e))))))