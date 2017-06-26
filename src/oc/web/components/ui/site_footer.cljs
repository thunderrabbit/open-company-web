(ns oc.web.components.ui.site-footer
  "Component for the site footer. This is copied into oc.core/footer and every change here should be reflected there and vice versa."
  (:require [rum.core :as rum]
            [oc.web.urls :as oc-urls]
            [oc.web.router :as router]
            [oc.web.local-settings :as ls]
            [oc.web.lib.utils :as utils]
            [oc.web.lib.responsive :as responsive]))

(rum/defc bottom-footer
  [container-class]
  [:div
    {:class container-class}
    [:div.small-logos
      [:a.twitter
        {:href oc-urls/oc-twitter}
        [:img {:src "/img/ML/home_page_twitter.svg"}]]
      [:a.medium
        {:href oc-urls/oc-medium}
        [:img {:src "/img/ML/home_page_medium.svg"}]]]
    [:div.copyright "© Copyright 2017. All rights reserved"]])

(rum/defcs site-footer  < (rum/local nil ::expanded)
  [s]
  ;; <!-- footer -->
  [:nav.navbar.navbar-default.navbar-bottom
    [:div.container-fluid.group
      [:div.left-column
        [:img.logo
          {:src "/img/ML/carrot_wordmark_white.svg"}]
        [:div.small-links
          [:a "Request Free Early Access"]]
        (when-not (responsive/is-mobile-size?)
          (bottom-footer "big-web-footer"))]

      [:div.right-column

        [:div.column
          {:class (when (= @(::expanded s) :support) "expanded")}
          [:div.column-title
            {:on-click #(when (responsive/is-mobile-size?)
                          (if (= @(::expanded s) :support)
                            (reset! (::expanded s) nil)
                            (reset! (::expanded s) :support)))}
            "SUPPORT"]
          [:div.column-item [:a {:href oc-urls/contact-mail-to} "Help"]]
          [:div.column-item [:a {:href oc-urls/contact-mail-to} "Contact"]]]

        [:div.column
          {:class (when (= @(::expanded s) :integration) "expanded")}
          [:div.column-title
            {:on-click #(when (responsive/is-mobile-size?)
                          (if (= @(::expanded s) :integration)
                            (reset! (::expanded s) nil)
                            (reset! (::expanded s) :integration)))}
            "INTEGRATIONS"]
          [:div.column-item [:a {:href "https://github.com/open-company"} "Developers"]]]

        [:div.column
          {:class (when (= @(::expanded s) :company) "expanded")}
          [:div.column-title
            {:on-click #(when (responsive/is-mobile-size?)
                          (if (= @(::expanded s) :company)
                            (reset! (::expanded s) nil)
                            (reset! (::expanded s) :company)))}
            "COMPANY"]
          [:div.column-item [:a {:href oc-urls/about} "About"]]
          [:div.column-item [:a {:href oc-urls/blog} "Blog"]]]

        [:div.column
          {:class (when (= @(::expanded s) :tour) "expanded")}
          [:div.column-title
            {:on-click #(when (responsive/is-mobile-size?)
                          (if (= @(::expanded s) :tour)
                            (reset! (::expanded s) nil)
                            (reset! (::expanded s) :tour)))}
            "TOUR"]
          [:div.column-item [:a {:href oc-urls/home} "Home"]]
          [:div.column-item [:a {:href oc-urls/features} "Features"]]]]

        (when (responsive/is-mobile-size?)
          (bottom-footer "mobile-footer"))]])