(def cljs-deps
  '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
    [adzerk/boot-reload "0.4.5" :scope "test"]
    [org.clojure/clojurescript "1.8.40"] ; ClojureScript compiler https://github.com/clojure/clojurescript]
    ;; --- DO NOT UPDATE OM, the 1.x.x code is Om Next and requires changes on our part https://github.com/omcljs/om/wiki/Quick-Start-(om.next)
    [org.omcljs/om "0.9.0" :excludes [cljsjs/react]] ; Cljs interface to React https://github.com/omcljs/om
    [cljs-http "0.1.39"] ; HTTP for cljs https://github.com/r0man/cljs-http
    [prismatic/schema "1.1.0"] ; Dependency of om-tools https://github.com/Prismatic/schema
    [prismatic/plumbing "0.5.2"] ; Dependency of om-tools https://github.com/Prismatic/plumbing
    [prismatic/om-tools "0.4.0"] ; Tools for Om https://github.com/Prismatic/om-tools
    [secretary "2.0.0.1-260a59"] ; Client-side router https://github.com/gf3/secretary
    [prismatic/dommy "1.1.0"] ; DOM manipulation and event library https://github.com/Prismatic/dommy
    [cljs-flux "0.1.2"] ; Flux implementation for Om https://github.com/kgann/cljs-flux
    [com.cognitect/transit-cljs "0.8.237"] ; ClojureScript wrapper for JavaScript JSON https://github.com/cognitect/transit-cljs
    [racehub/om-bootstrap "0.6.1"] ; Bootstrap for Om https://github.com/racehub/om-bootstrap
    [noencore "0.2.1"] ; Clojure & ClojureScript functions not in core https://github.com/r0man/noencore
    [org.clojure.bago/cljs-dynamic-resources "0.0.3"] ; Dynamically load JavaScript and CSS https://github.com/bago2k4/cljs-dynamic-resources
    [com.andrewmcveigh/cljs-time "0.4.0"] ; A clj-time inspired date library for clojurescript. https://github.com/andrewmcveigh/cljs-time
    [funcool/cuerdas "0.7.1"] ; String manipulation library for Clojure(Script) https://github.com/funcool/cuerdas
    [cljsjs/react "0.14.7-0"] ; A Javascript library for building user interfaces https://github.com/cljsjs/packages
    [medley "0.7.3"] ; lightweight library of useful, mostly pure functions that are "missing" from clojure.core
    [cljsjs/d3 "3.5.7-1"]]) ; d3 externs https://clojars.org/cljsjs/d3

(def static-site-deps
  '[[hiccup "1.0.5" :scope "test"]
    [perun "0.3.0" :scope "test"]
    [compojure "1.5.0" :scope "test"]
    [pandeiro/boot-http "0.7.3" :scope "test"]
    [deraen/boot-sass "0.2.1" :scope "test"]])

(set-env!
  :source-paths   #{"src" "scss" "site"}
  :resource-paths #{"resources"}
  :dependencies   (into cljs-deps static-site-deps))

(require '[pandeiro.boot-http  :refer [serve]]
         '[adzerk.boot-cljs :refer [cljs]]
         '[adzerk.boot-reload :refer [reload]]
         '[deraen.boot-sass :refer [sass]]
         '[io.perun :as p])

;; We use a bunch of edn files in `resources/pages` to declare a "page"
;; these edn files can hold additional information about the page such
;; as it's permalink identifier (`:page` key) or the page's title etc.

(defn page? [f]
  (and (.startsWith (:path f) "pages/")
       (.endsWith (:path f) ".edn")))

(defn page->permalink [f]
  (-> (read-string (slurp (:full-path f)))
      :page name (str ".html")))

(deftask build-site []
  (comp (p/base)
        (p/permalink :permalink-fn page->permalink
                     :filterer page?)
        (p/render :renderer 'oc.core/static-page
                  :filterer page?)
        ;; We're not actually rendering a collection here but using the collection task
        ;; is often a handy hack to render pages which are "unique"
        (p/collection :renderer 'oc.core/app-shell
                      :page "app-shell.html"
                      :filterer identity)))

(deftask dev []
  (comp (serve :handler 'oc.server/handler
               :port 3559)
        (watch)
        (sass)
        (build-site)
        (reload :asset-path "/public"
                :on-jsload 'open-company-web.core/on-js-reload)
        (cljs)))

(deftask prod-build []
  (comp (sass :output-style :compressed)
        (build-site)
        (cljs :optimizations :advanced)))