(ns oc.shared
  (:require [environ.core :refer (env)]))

(defn circular-font-folder [font-file]
  (str (when (env :oc-web-cdn-url)
        (env :oc-web-cdn-url))
   font-file))

(defn circular-book-font []
  [:link {:rel "stylesheet"
          :type "text/css"
          :href (circular-font-folder "/CircularWebFont/LLCircular-BookWeb/css/stylesheet.css")}])

(defn circular-bold-font []
  [:link {:rel "stylesheet"
          :type "text/css"
          :href (circular-font-folder "/CircularWebFont/LLCircular-BoldWeb/css/stylesheet.css")}])

(defn cdn [img-src]
  (str (when (env :oc-web-cdn-url) (str (env :oc-web-cdn-url) "/" (env :oc-deploy-key))) img-src))

(def testimonials-logos-line
  [:div.homepage-testimonials-container.group
    ; [:div.homepage-testimonials-copy
    ;   "Growing and distributed teams around the world ❤️ Carrot"]
    [:div.homepage-testimonials-logos
      [:div.homepage-testimonials-logo.logo-ifttt]
      [:div.homepage-testimonials-logo.logo-hopper]
      [:div.homepage-testimonials-logo.logo-primary]
      [:div.homepage-testimonials-logo.logo-hinge]
      [:div.homepage-testimonials-logo.logo-resy]
      [:div.homepage-testimonials-logo.logo-flyt]
      [:div.homepage-testimonials-logo.logo-weblify]
      [:div.homepage-testimonials-logo.logo-novo]
      [:div.homepage-testimonials-logo.logo-gamercraft]]])

(defn dashed-string [num & [responsive-class]]
  [:div.dashed-string
    {:class (str "dashed-string-" num " " responsive-class)}])

(defn testimonial-block [slug & [responsive-class]]
  (let [testimonial-copy (cond
                         (= slug :ifttt)
                         (str
                          "“Carrot eliminates the fear of missing key Slack conversations, "
                          "and cuts out the \"Did you see my message?\" nagging.”")
                         (= slug :blend-labs)
                         (str
                          "“We use Carrot for key announcements and weekly updates no one can miss.”")
                         (= slug :bank-novo)
                         (str
                          "“Carrot keeps everyone across our global offices up to date. It "
                          "helps us share big wins and key information across our growing family.”"))
        footer-copy (cond
                      (= slug :ifttt)
                      "Kevin Ebaugh, Senior Platform Community Manager"
                      (= slug :blend-labs)
                      "Sara Vienna, Head of Design"
                      (= slug :bank-novo)
                      "Tyler McIntyre, CEO")
        testimonial-website (cond
                             (= slug :ifttt)
                             "https://ifttt.com/"
                             (= slug :blend-labs)
                             "https://bl3ndlabs.com/"
                             (= slug :bank-novo)
                             "https://banknovo.com/")
        testimonial-company (cond
                             (= slug :ifttt)
                             "IFTTT.com"
                             (= slug :blend-labs)
                             "Bl3NDlabs.com"
                             (= slug :bank-novo)
                             "Banknovo.com")]
    [:div.testimonials-block.group
      {:class (str (name slug) " " responsive-class)}
      [:div.testimonial-copy
        testimonial-copy]
      [:div.testimonial-copy-footer
        [:div.testimonial-author-pic]
        [:div.testimonial-copy-footer-right
          footer-copy
          [:a.testimonial-copy-link
            {:href testimonial-website
             :target "_blank"}
            testimonial-company]]]]))

(defn testimonials-screenshot-block [block & [responsive-class]]
  (let [header (cond
                (= block :thoughtful-communication)
                "Thoughtful communication"
                (= block :conversation)
                "What’s new"
                (= block :threads)
                "Clear, organized discussions"
                (= block :analytics)
                "Know who saw your update"
                (= block :stay-in-sync)
                "Daily newsletter to stay in sync"
                (= block :stay-in-sync-slack)
                "Daily digest to stay in sync"
                (= block :share-to-slack)
                "Auto-share posts to Slack")
        subline (cond
                 (= block :thoughtful-communication)
                 "Space to write longer updates that convey more information"
                 (= block :conversation)
                 (str
                  "Get caught up - fast - and let Carrot help you filter "
                  "out the discussions you don't want to follow.")
                 (= block :threads)
                 (str
                  "Threaded comments make it easy for your "
                  "team to stay engaged asynchronously. Ideal "
                  "for remote teams.")
                 (= block :analytics)
                 (str
                  "Carrot works in the background to make sure "
                  "everyone sees what matters")
                 (= block :stay-in-sync)
                 "Everyone gets a daily, personalized summary of what's important."
                 (= block :stay-in-sync-slack)
                 "Everyone gets a daily, personalized summary of what's important."
                 (= block :share-to-slack)
                 "Your Carrot posts are automatically shared to the right Slack #channel.")
        screenshot-num (cond
                        (= block :thoughtful-communication)
                        1
                        (= block :conversation)
                        2
                        (= block :threads)
                        3
                        (= block :analytics)
                        4
                        (= block :stay-in-sync)
                        5
                        (= block :stay-in-sync-slack)
                        7
                        (= block :share-to-slack)
                        6)]
    [:div.testimonials-screenshot-block
      {:class responsive-class}
      [:div.testimonials-screenshot-header
        header]
      [:div.testimonials-screenshot-subheader
        subline]
      [:img.testimonials-screenshot.mobile-only
        {:src (cdn (str "/img/ML/testimonials_screenshot_mobile_" screenshot-num ".png"))
         :srcSet (str
                  (cdn (str "/img/ML/testimonials_screenshot_mobile_" screenshot-num "@2x.png")) " 2x, "
                  (cdn (str "/img/ML/testimonials_screenshot_mobile_" screenshot-num "@3x.png")) " 3x, "
                  (cdn (str "/img/ML/testimonials_screenshot_mobile_" screenshot-num "@4x.png")) " 4x")}]
      [:img.testimonials-screenshot.big-web-tablet-only
        {:src (cdn (str "/img/ML/testimonials_screenshot_" screenshot-num ".png"))
         :srcSet (str
                  (cdn (str "/img/ML/testimonials_screenshot_" screenshot-num "@2x.png")) " 2x, "
                  (cdn (str "/img/ML/testimonials_screenshot_" screenshot-num "@3x.png")) " 3x, "
                  (cdn (str "/img/ML/testimonials_screenshot_" screenshot-num "@4x.png")) " 4x")}]]))

(defn testimonials-section [page]
  [:section.testimonials

    (dashed-string 1)

    (testimonial-block :ifttt "")

    (dashed-string 2)

    [:div.testimonials-floated-block.big-web-tablet-only
      [:div.testimonials-floated-block-inner.left-block.group
        [:img.testimonials-floated-screenshot
          {:src (cdn "/img/ML/testimonials_floated_screenshot_1.png")
           ; :srcSet (str
           ;          (cdn "/img/ML/testimonials_floated_screenshot_1@2x.png") " 2x, "
           ;          (cdn "/img/ML/testimonials_floated_screenshot_1@3x.png") " 3x, "
           ;          (cdn "/img/ML/testimonials_floated_screenshot_1@4x.png") " 4x")
           }]
        [:div.testimonials-floated-copy
          [:div.testimonials-floated-header
            "Stay focused with less noise"]
          [:div.testimonials-floated-subheader
            "Personalize your news feed to filter out topics you don't care about."
            [:br]
            "This saves you time and makes it faster to get caught up."]]]

      [:div.testimonials-floated-block-inner.right-block.group
        [:img.testimonials-floated-screenshot
          {:src (cdn "/img/ML/testimonials_floated_screenshot_2.png")
           ; :srcSet (str
           ;          (cdn "/img/ML/testimonials_floated_screenshot_2@2x.png") " 2x, "
           ;          (cdn "/img/ML/testimonials_floated_screenshot_2@3x.png") " 3x, "
           ;          (cdn "/img/ML/testimonials_floated_screenshot_2@4x.png") " 4x")
           }]
        [:div.testimonials-floated-copy
          [:div.testimonials-floated-header
            "Reduce interruptions"]
          [:div.testimonials-floated-subheader
            "Prefer to see team updates batched together to read them all at once?"
            [:br]
            "Your daily digest makes it easy to get caught up when it's more convenient."]]]

    ]

    (dashed-string 5)

    (testimonial-block :blend-labs "")
    
    (dashed-string 6)
    ])

(def pricing-table
  [:div.pricing-table.group
    ; [:div.pricing-table-left
    ;   [:div.pricing-table-left-price
    ;     "$0"]
    ;   [:div.pricing-table-left-subprice
    ;     "for teams of up to 20 people"]]
    ; [:div.pricing-table-divider-line]
    ; [:div.pricing-table-right.group
    ;   [:div.pricing-table-right-copy
    ;     (str
    ;      "Carrot is free for up to 20 people. After that, it’s "
    ;      "just $3.25 / month for each person with our annual plan. "
    ;      "If you prefer a monthly plan, it’s $4.00 / month.")]
    ;   [:a.pricing-table-right-link
    ;     {:href "/sign-up"}
    ;     "Try Carrot for free"]]

    [:table.pricing-table
      [:thead
        [:tr
          [:th
            [:h4
              "Free"]]
          [:th
            [:h4
              "Premium $5/user per month"]]]]
      [:tbody
        [:tr
          [:td
            [:span.bold
              "Unlimited users"]]
          [:td
            [:span.bold
              "Unlimited users"]]]
        [:tr
          [:td
            [:span.bold
              "Unlimited posts"]]
          [:td
            [:span.bold
              "Unlimited posts"]]]
        [:tr
          [:td
            [:span
              "Team-level access to all updates"]]
          [:td
            [:span
              "Team, private and public access"]]]
        [:tr
          [:td
            [:span
              "Anyone can add an update"]]
          [:td
            [:span
              "Editor and view-only permissions"]]]
        [:tr
          [:td]
          [:td
            [:span
              "Editor and view-only permissions"]]]
        [:tr
          [:td]
          [:td
            [:span
              "Admin advanced settings"]]]
        [:tr
          [:td]
          [:td
            [:span
              "Assign roles for team onboarding (coming)"]]]]]
      [:div.non-profits
        [:span
          "Nonprofits and K-12 education are always free."]]])

(def pricing-table-footer
  [:div.pricing-header-footer
    [:div.pricing-header-footer-logo]
    [:div.pricing-header-footer-subheadline
      "Have a team of 250+? "
      [:a.chat-with-us
        {:class "intercom-chat-link"
         :href "mailto:hello@carrot.io"}
        "Let’s chat about our Enterprise plan."]]])

(def pricing-headline "Open source and free")

(def pricing-chat
  [:div.pricing-subheadline
    "Questions? "
    [:a.chat-with-us
      {:class "intercom-chat-link"
       :href "mailto:hello@carrot.io"}
      "Let's chat"]])

(def pricing-footer
  [:section.pricing-footer

    [:h1.pricing-headline
      pricing-headline]    

    pricing-table
    
    ; pricing-table-footer

    pricing-chat])

(def testimonials-section-old
  [:section.testimonials-section-old.big-web-tablet-only
    [:div.testimonials-cards-container.group
      [:div.testimonials-cards-inner.group
        ;; First column
        [:div.testimonals-cards-column.group
          [:div.testimonial-card.peak-support
            [:div.testimonial-quote
              (str
               "“Carrot fixed our issue of email overload. "
               "We use it for important company communications, "
               "shout- outs, and announcements so they won’t get "
               "buried in everyone’s inbox. It’s been a big win for us.”")]
            [:div.testimonial-footer.group
              [:div.testimonial-image]
              [:div.testimonial-name
                "Jon Steiman"]
              [:div.testimonial-role
                [:a
                  {:href "https://www.peaksupport.io/"
                   :target "_blank"}
                  "Peak Support, CEO"]]]]
            [:div.testimonial-card.wayne-state-univerity
              [:div.testimonial-quote
                (str
                 "“Carrot helps me share things "
                 "the entire team needs to know "
                 "- instead of burying it somewhere "
                 "it won’t get noticed.”")]
              [:div.testimonial-footer.group
                [:div.testimonial-image]
                [:div.testimonial-name
                  "Nick DeNardis"]
                [:div.testimonial-role
                  [:a
                    {:href "https://wayne.edu/"
                     :targe "_blank"}
                    "Wayne State University, Director of Communications"]]]]]
        ;; Second column
        [:div.testimonals-cards-column.group
          [:div.testimonial-card.oval-money
            [:div.testimonial-quote
              (str
               "“Carrot keeps our distributed teams "
               "informed about what matters most.”")]
            [:div.testimonial-footer.group
              [:div.testimonial-image]
              [:div.testimonial-name
                "Edoardo Benedetto"]
              [:div.testimonial-role
                [:a
                  {:href "https://ovalmoney.com/"
                   :target "_blank"}
                  "Oval Money, Head of Design"]]]]
          [:div.testimonial-card.hellotickets
            [:div.testimonial-quote
              (str
               "“Staying aligned across two offices "
               "is hard. Slack feels too ‘light’ and "
               "crazy for important information; and "
               "no one wants to read weekly emails. With "
               "Carrot, we have a knowledge base of what's "
               "going on with the company that’s made it "
               "easy to stay aligned.”")]
            [:div.testimonial-footer.group
              [:div.testimonial-image]
              [:div.testimonial-name
                "Alberto Martinez"]
              [:div.testimonial-role
                [:a
                  {:href "https://ovalmoney.com/"
                   :target "_blank"}
                  "Hellotickets, CEO"]]]]]
        ;; Third column
        [:div.testimonals-cards-column.group
          [:div.testimonial-card.m-io
            [:div.testimonial-quote
              (str
               "“On Carrot, my updates get noticed "
               "and get the team talking. I love that.”")]
            [:div.testimonial-footer.group
              [:div.testimonial-image]
              [:div.testimonial-name
                "Tom Hadfield"]
              [:div.testimonial-role
                [:a
                  {:href "https://m.io/"
                   :target "_blank"}
                  "M.io, CEO"]]]]
          [:div.testimonial-card.partner-hero
            [:div.testimonial-quote
              (str
               "“Carrot is where we communicate important "
               "information when we need everyone to see it "
               "- regardless of their time zone.”")]
            [:div.testimonial-footer.group
              [:div.testimonial-image]
              [:div.testimonial-name
                "Andrew Love"]
              [:div.testimonial-role
                [:a
                  {:href "https://partnerhero.com/"
                   :targe "_blank"}
                  "PartnerHero, Director of R&D"]]]]]
        ;; Fourth column
        [:div.testimonals-cards-column.group
          [:div.testimonial-card.novo
            [:div.testimonial-quote
              (str
               "“We use Carrot when we need to make sure everyone "
               "is on the same page across all our offices here "
               "and abroad. It helps us share BIG wins and in a "
               "fast-growing startup keeping that family vibe, "
               "its awesome.”")]
            [:div.testimonial-footer.group
              [:div.testimonial-image]
              [:div.testimonial-name
                "Tyler McIntyre"]
              [:div.testimonial-role
                [:a
                  {:href "https://banknovo.com/"
                   :target "_blank"}
                  "Novo, CEO"]]]]
          [:div.testimonial-card.skylight-digital
            [:div.testimonial-quote
              (str
               "“Carrot is a perfect compliment for Slack. We "
               "use it for longer-form weekly updates no one "
               "should miss.”")]
            [:div.testimonial-footer.group
              [:div.testimonial-image]
              [:div.testimonial-name
                "Chris Cairns"]
              [:div.testimonial-role
                [:a
                  {:href "https://skylight.digital/"
                   :target "_blank"}
                  "Skylight Digital, Managing Director"]]]]]]]])

(def slack-hero-screenshot
  [:div.main-animation-container
    [:img.main-animation
      {:src (cdn "/img/ML/slack_screenshot.png")
       :srcSet (str
                (cdn "/img/ML/slack_screenshot@2x.png") " 2x, "
                (cdn "/img/ML/slack_screenshot@3x.png") " 3x, "
                (cdn "/img/ML/slack_screenshot@4x.png") " 4x")}]])

(def bootstrap-css
  ;; Bootstrap CSS //getbootstrap.com/
  [:link
    {:rel "stylesheet"
     :href "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"
     :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u"
     :crossorigin "anonymous"}])

(def bootstrap-js
  ;; Bootstrap JavaScript //getf.com/
  [:script
    {:src "//maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
     :type "text/javascript"
     :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
     :crossorigin "anonymous"}])

(def font-awesome
  ;; Font Awesome icon fonts //fortawesome.github.io/Font-Awesome/cheatsheet/
  [:link
    {:rel "stylesheet"
     :href "//maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css"}])

(def ie-jquery-fix
  ;; From https://stackoverflow.com/questions/5087549/access-denied-to-jquery-script-on-ie
  ;; Github: https://github.com/MoonScript/jQuery-ajaxTransport-XDomainRequest
  [:script
    {:src "//cdnjs.cloudflare.com/ajax/libs/jquery-ajaxtransport-xdomainrequest/1.0.4/jquery.xdomainrequest.min.js"
     :crossorigin "anonymous"}])

(def jquery
  [:script
    {:src "//ajax.googleapis.com/ajax/libs/jquery/2.1.4/jquery.min.js"
     :crossorigin "anonymous"}])

(def google-fonts
  ;; Google fonts Muli
  [:link {:href "https://fonts.googleapis.com/css2?family=IBM+Plex+Mono&family=Muli&family=PT+Serif:wght@700&display=swap" :rel "stylesheet"}])

(def stripe-js
  [:script {:src "https://js.stripe.com/v3/"}])

(defn google-analytics-init []
  [:script (let [ga-version (if (env :ga-version)
                              (str "'" (env :ga-version) "'")
                              false)
                 ga-tracking-id (if (env :ga-tracking-id)
                                  (str "'" (env :ga-tracking-id) "'")
                                  false)]
             (str "CarrotGA.init(" ga-version "," ga-tracking-id ");"))])

(defn fullstory-init []
  [:script (str "init_fullstory();")])

(def oc-js
  [:script {:type "text/javascript" :src "/oc.js"}])

(def oc-assets-js
  [:script {:type "text/javascript" :src "/oc_assets.js"}])

(def tag-manager-head
  [:script"
    (function(w,d,s,l,i){w[l]=w[l]||[];w[l].push({'gtm.start':
    new Date().getTime(),event:'gtm.js'});var f=d.getElementsByTagName(s)[0],
    j=d.createElement(s),dl=l!='dataLayer'?'&l='+l:'';j.async=true;j.src=
    'https://www.googletagmanager.com/gtm.js?id='+i+dl;f.parentNode.insertBefore(j,f);
    })(window,document,'script','dataLayer','GTM-5D4DTSB');"])

(def tag-manager-body
  [:noscript
    [:iframe
      {:src "https://www.googletagmanager.com/ns.html?id=GTM-5D4DTSB"
       :height "0"
       :width "0"
       :style {:display "none"
               :visibility "hidden"}}]])

(def ph-banner
  [:div.ph-banner
    [:div.ph-banner-content
      [:div.ph-banner-cat]
      [:div.ph-banner-copy
        [:span.heavy "Hello Product Hunter! "]
        " We can't wait to hear what you think about Carrot 2.0."]]
    [:div.ph-banner-opac-bg]
    [:button.mlb-reset.ph-banner-close-button
      {:onclick "OCStaticHidePHBanner();"}]])

(defn covid-banner [page]
  [:div.covid-banner
    [:div.covid-banner-content
      [:div.covid-banner-carrot-logo]
      [:div.covid-banner-copy.mobile-only
        "Given COVID-19, Carrot is now free."
        (when-not (= page :pricing)
          [:br])
        (when-not (= page :pricing)
          [:a
            {:href "/pricing"}
            "Learn more"])
        (when-not (= page :pricing)
          ".")]
      [:div.covid-banner-copy.big-web-tablet-only
        "Given the COVID-19 crisis, Carrot is free for unlimited users until further notice. "
        (when-not (= page :pricing)
          [:a
            {:href "/pricing"}
            "Learn more"])
        (when-not (= page :pricing)
          ".")]]
    [:button.mlb-reset.covid-banner-close-button
      {:onclick "OCStaticHideCovidBanner();"}]])

(defn head []
  [:head
    tag-manager-head
    ;; -------------
    [:meta {:charset "utf-8"}]
    [:meta {:content "IE=edge", :http-equiv "X-UA-Compatible"}]
    [:meta {:content "width=device-width, height=device-height, initial-scale=1", :name "viewport"}]
    [:meta {:name "slack-app-id" :content (env :oc-slack-app-id)}]
    ;; The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags
    [:title "Wut! | wuts.io"]
    (circular-book-font)
    (circular-bold-font)
    google-fonts
    bootstrap-css
    ;; Local css
    [:link {:href (cdn "/css/app.main.css"), :rel "stylesheet"}]
    ;; Fallback for the CDN compacted css
    [:link {:href (cdn "/main.css") :rel "stylesheet"}]
    ;; HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries
    ;; WARNING: Respond.js doesn't work if you view the page via file://
    "<!--[if lt IE 9]>
      <script src=\"//oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js\"></script>
      <script src=\"//oss.maxcdn.com/respond/1.4.2/respond.min.js\"></script>
    <![endif]-->"
    font-awesome
    ;; Favicon
    [:link {:rel "icon" :type "image/png" :href (cdn "/img/carrot_logo.png") :sizes "64x64"}]
    ;; jQuery needed by Bootstrap JavaScript
    jquery
    ie-jquery-fix
    ;; Static js files
    [:script {:src (cdn "/js/static-js.js")}]
    ;; Intercom (Support chat)
    [:script {:src (cdn "/js/intercom.js")}]
    ;; Google Analytics
    [:script {:type "text/javascript" :src "https://www.google-analytics.com/analytics.js"}]
    [:script {:type "text/javascript" :src "/lib/autotrack/autotrack.js"}]
    [:script {:type "text/javascript" :src "/lib/autotrack/google-analytics.js"}]
    (google-analytics-init)
    ;; TODO: enable when we want to use full story for static pages.
    ;;(when (= (env :fullstory) "true")
    ;;  [:script {:type "text/javascript" :src "/lib/fullstory.js"}])
    ;;(when (= (env :fullstory) "true") (fullstory-init))
    bootstrap-js])

(defn mobile-menu
  "Mobile menu used to show the collapsable menu in the marketing site."
  [active-page]
  [:div.site-mobile-menu.hidden
    [:div.site-mobile-menu-container
      [:div.site-mobile-menu-item
        [:a
          {:href "/?no_redirect=1"
           :class (when (= active-page "index") "active")}
          "Home"]]
      [:div.site-mobile-menu-item
        [:a
          {:href "/pricing"
           :class (when (= active-page "pricing") "active")}
          "Pricing"]]
      [:div.site-mobile-menu-item
        [:a
          {:href "/apps/detect"}
          "Get the mobile app"]]]
    [:div.site-mobile-menu-footer
      [:button.mlb-reset.login-btn
        {:id "site-mobile-menu-login"}
        "Login"]
      [:button.mlb-reset.get-started-button.get-started-action
        {:id "site-mobile-menu-getstarted"}
        "Start free"]]])

(defn nav
  "Static hiccup for the site header."
  [active-page]
  (let [is-slack-lander? (= active-page "slack-lander")
        is-slack-page? (= active-page "slack")
        site-navbar-container (if is-slack-lander?
                               :div.site-navbar-container.is-slack-header
                               :div.site-navbar-container)]
    [:nav.site-navbar
      [site-navbar-container
        [:a.navbar-brand-left
          {:href "/?no_redirect=1"}]
        [:div.navbar-brand-center
          [:a
            {:href "/?no_redirect=1"
             :class (when (= active-page "index") "active")}
            "Home"]
          [:div.apps-container
            [:button.mlb-reset.apps-bt
              {:class (when (= active-page "about") "active")}
              "Apps"]
            [:div.apps-dropdown-menu
              [:div.app-items-group
                "Desktop apps"]
              [:a.app-item
                {:href "/apps/mac"}
                [:span "Mac"]
                [:span.beta-app-label "BETA"]]
              [:a.app-item
                {:href "/apps/win"}
                [:span "Windows"]
                [:span.beta-app-label "BETA"]]
              [:div.app-items-group
                "Mobile apps"]
              [:a.app-item
                {:href "/apps/android"}
                [:span "Android"]
                [:span.beta-app-label "BETA"]]
              [:a.app-item
                {:href "/apps/ios"}
                [:span "iPhone"]
                [:span.beta-app-label "BETA"]]]]
          [:a
            {:href "/pricing"
             :class (when (= active-page "pricing") "active")}
            "Pricing"]]

        ;; Desktop & tablet
        (cond
          is-slack-page?
          [:div.site-navbar-right.big-web-tablet-only
            [:a.login
              {:id "site-header-login-item"
               :href (env :slack-signup-url)}
                "Add to Slack"]]
          is-slack-lander?
          [:div.site-navbar-right.big-web-tablet-only
            [:a.signup.continue-with-slack
              {:id "site-header-signup-item"
               :href (env :slack-signup-url)}
                "Continue with Slack"]]
          :else
          [:div.site-navbar-right.big-web-tablet-only
            [:a.login
              {:id "site-header-login-item"
               :href "/login"}
                "Login"]
            [:a.signup
              {:id "site-header-signup-item"
               :href "/sign-up"}
              "Start free"]])

        ;; Mobile
        (cond
          is-slack-lander?
          [:div.site-navbar-right.mobile-only
            [:a.signup.continue-with-slack
              {:id "site-header-signup-item-mobile"
               :href (env :slack-signup-url)}]]
          :else
          [:div.site-navbar-right.mobile-only
            [:a.login
              {:id "site-header-login-item-mobile"
               :data-bago false
               :href "/login"}
                "Login"]])
        [:div.mobile-ham-menu
          {:onClick "javascript:OCStaticSiteMobileMenuToggle();"}]]]))

(defn footer
  "Static hiccup for the site footer."
  [options]
  [:footer.navbar.navbar-default.navbar-bottom
    [:div.container-fluid.group
      [:div.right-column.group

        [:div.column.column-company
          [:div.column-title
            "Product"]
          [:div.column-item [:a {:href "/pricing"} "Pricing"]]
          [:div.column-item [:a {:href "https://carrot.news/" :target "_blank"} "What’s new"]]
          [:div.column-item [:a {:href (:oc-github options) :target "_blank"} "GitHub"]]
          [:div.column-item [:a {:href "/slack"} "Slack integration"]]]

        [:div.column.column-resources
          [:div.column-title
            "Company"]
          [:div.column-item [:a {:href "/about"} "About"]]
          [:div.column-item [:a {:href "https://twitter.com/carrot_hq" :target "_blank"} "Twitter"]]
          [:div.column-item [:a {:href "https://blog.carrot.io" :target "_blank"} "Blog"]]]

        [:div.column.column-support
          [:div.column-title
            "Resources"]
          [:div.column-item [:a {:href "https://help.carrot.io/" :target "_blank"} "Help center"]
          [:div.column-item
            [:a
              {:class "intercom-chat-link"
               :href "mailto:hello@carrot.io"}
              "Contact us"]]]]]
      [:div.left-column.group
        [:img.logo
          {:src (cdn "/img/ML/carrot_wordmark.svg")}]
        [:div.footer-small-links.static
          [:a {:href "/login"} "Login"]
          "or"
          [:a {:href "/sign-up"} "create your team"]]
        [:div.tos-and-pp
          [:a {:href "/privacy"}
           "Privacy"]
          " & "
          [:a {:href "/terms"}
           "Terms"]]
        [:div.copyright
          "© 2020 Carrot"]]]])