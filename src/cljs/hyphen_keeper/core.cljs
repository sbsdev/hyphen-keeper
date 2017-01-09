(ns hyphen-keeper.core
  (:require [accountant.core :as accountant]
            [ajax.core :as ajax]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]))

(defonce app-state
  (reagent/atom
   {:hyphenations {}
    :spelling 1
    :word ""
    :hyphenation ""
    :suggested-hyphenation ""
    :feedback {:message "" :kind :none}}))

(def spelling (reagent/cursor app-state [:spelling]))
(def word (reagent/cursor app-state [:word]))
(def hyphenation (reagent/cursor app-state [:hyphenation]))
(def suggested-hyphenation (reagent/cursor app-state [:suggested-hyphenation]))
(def feedback (reagent/cursor app-state [:feedback]))

(defn update-hyphenations! [f & args]
  (apply swap! app-state update-in [:hyphenations] f args))

(defn remove-hyphenation! [{:keys [word]}]
  (update-hyphenations! dissoc word))

(defn load-hyphenation-patterns!
  [spelling word]
  (ajax/GET "/api/words"
            :params {:spelling spelling :search word}
            :handler (fn [hyphenations] (swap! app-state assoc :hyphenations
                                               (into (sorted-map)
                                                     (map (fn [{:keys [word] :as pattern}] [word pattern])
                                                          hyphenations))))
            :error-handler (fn [details]
                             (.warn js/console
                                    (str "Failed to refresh hyphenation patterns from server: " details)))
            :response-format :json
            :keywords? true))

(defn set-feedback! [message kind]
  (reset! feedback {:message message :kind kind})
  (.setTimeout js/window #(reset! feedback {:message "" :kind :none}) 2000)
  )

(defn reset-all! []
  (set-feedback! "Word successfully added" :success)
  (reset! word "")
  (reset! hyphenation "")
  (reset! suggested-hyphenation "")
  (load-hyphenation-patterns! @spelling @word))

(defn add-hyphenation-pattern!
  [pattern]
  (ajax/POST "/api/words"
             :params pattern
             :handler (fn [] (reset-all!))
             :error-handler (fn [details]
                              (.warn js/console
                                     (str "Failed to add hyphenation pattern: " details)))
             :format :json))

(defn remove-hyphenation-pattern!
  [pattern]
  (ajax/DELETE (str "/api/words/" (:word pattern))
               :params pattern
               :handler (fn [] (remove-hyphenation! pattern))
               :error-handler (fn [details]
                                (.warn js/console
                                       (str "Failed to remove hyphenation pattern: " details)))
               :format :json))

(defn lookup-hyphenation-pattern!
  [spelling word]
  (ajax/GET "/api/hyphenate"
            :params {:word word :spelling spelling}
            :handler (fn [hyphenated] (do
                                        (swap! app-state assoc :suggested-hyphenation hyphenated)
                                        (swap! app-state assoc :hyphenation hyphenated)))
            :error-handler (fn [details]
                             (.warn js/console
                                    (str "Failed to lookup hyphenation patterns for word: " details)))))

(defn hyphenation-pattern [{:keys [word hyphenation]}]
  [:tr
   [:td word]
   [:td hyphenation]])

(defn- hyphenation-valid? [s]
  (and (not (string/blank? s))
       (string/includes? s "-")
       (re-matches #"[a-z\xDF-\xFF-]+" s)))

(defn hyphenation-pattern-editable-item [{:keys [hyphenation]}]
  (let [editing (reagent/atom false)
        new-hyphenation (reagent/atom hyphenation)]
    (fn [{:keys [word hyphenation] :as pattern}]
      (if-not @editing
        [:tr
         [:td word]
         [:td hyphenation]
         [:td
          [:div.btn-group
           [:button.btn.btn-default
            {:on-click #(reset! editing true)}
            [:span.glyphicon.glyphicon-edit {:aria-hidden true}] " Edit"]
           [:button.btn.btn-default
            {:on-click #(remove-hyphenation-pattern! pattern)}
            [:span.glyphicon.glyphicon-trash {:aria-hidden true}] " Delete"]]]]
        [:tr
         [:td word]
         [:td
          [:input.form-control
           {:type "text"
            :auto-focus true
            :on-change #(reset! new-hyphenation (-> % .-target .-value))
            :on-key-down #(case (.-which %)
                            27 (do
                                 (reset! new-hyphenation hyphenation)
                                 (reset! editing false))
                            nil)
            :value @new-hyphenation}]]
         [:td
          [:div.btn-group
           [:button.btn.btn-default
            {:on-click #(do (when (hyphenation-valid? @new-hyphenation)
                              (add-hyphenation-pattern!
                               (assoc pattern :hyphenation @new-hyphenation)))
                            (reset! editing false))}
            [:span.glyphicon.glyphicon-ok {:aria-hidden true}] " Save"]
           [:button.btn.btn-default
            {:on-click #(do
                          (reset! new-hyphenation hyphenation)
                          (reset! editing false))}
            [:span.glyphicon.glyphicon-remove {:aria-hidden true}] " Cancel"]]]]))))

(defn word-field []
  [:div.form-group
   [:label {:for "wordInput"} "Word"]
   [:input.form-control
    {:id "wordInput"
     :type "text"
     :placeholder "Word"
     :auto-focus true
     :value @word
     :on-change (fn [e]
                  (reset! word (-> e .-target .-value string/lower-case))
                  (load-hyphenation-patterns! @spelling @word))
     :on-blur #(lookup-hyphenation-pattern! @spelling @word)}]])

(defn hyphenation-field []
  (let [label "Corrected Hyphenation"
        valid? (or (string/blank? @hyphenation) (hyphenation-valid? @hyphenation))
        klass (if valid? "form-group" "form-group has-error")]
    [:div
     {:class klass}
     [:label {:for "hyphenationInput"} label]
     [:input.form-control
      {:type "text"
       :placeholder label
       :value @hyphenation
       :on-change #(reset! hyphenation (-> % .-target .-value string/lower-case))}]]))

(defn- hyphenation-add-button []
  [:div.form-group
   [:button.btn.btn-default
    {:on-click #(when (and @word (hyphenation-valid? @hyphenation))
                  (add-hyphenation-pattern! {:word @word :hyphenation @hyphenation :spelling @spelling}))
     :disabled (when (or (string/blank? @word)
                         (not (hyphenation-valid? @hyphenation))
                         (= @hyphenation @suggested-hyphenation))
                 "disabled")}
    "Add"]])

(defn spelling-filter []
  [:div.form-group
   [:select {:value @spelling
             :on-change (fn [e]
                          (reset! spelling (-> e .-target .-value))
                          (load-hyphenation-patterns! @spelling @word)
                          (lookup-hyphenation-pattern! @spelling @word))}
    [:option {:value 0} "Old Spelling"]
    [:option {:value 1} "New Spelling"]]])

(defn hyphenation-filter [search]
  [:input.form-control
   {:type "text"
    :placeholder "Search"
    :value @search
    :on-change (fn [e]
                 (reset! search (-> e .-target .-value))
                 (load-hyphenation-patterns! @spelling @search))}])

(defn suggested-hyphenation-field []
  (let [id "suggestedHyphenation"
        label "Suggested Hyphenation"]
    [:div.form-group
     [:label {:for id} label]
     [:input.form-control
      {:id id
       :type "text"
       :placeholder label
       :disabled "disabled"
       :value @suggested-hyphenation}]]))

(defn feedback-alert []
  (let [msg (:message @feedback)
        klass (if-let [kind (:kind @feedback)]
                (str "alert alert-" (name kind))
                "alert")]
    [:div {:class klass :role "alert"} msg]))

(defn new-hyphenation []
  [:div.form
   [spelling-filter]
   [word-field]
   [suggested-hyphenation-field]
   [hyphenation-field]
   [hyphenation-add-button]
   (when (not= :none (:kind @feedback))
     [feedback-alert])])

(defn- button [label href disabled]
  [:a.btn.btn-default {:href href :target "_blank" :disabled disabled} label])

(defn hyphenation-lookup [word]
  (let [disabled (when (string/blank? word) "disabled")]
    [:div.row
     [:div.btn-group {:role "group" :aria-label "Buttons for hyphenation lookup"}
      (button "Duden" (str "http://www.duden.de/suchen/dudenonline/" word) disabled)
      (button "TU Chemnitz" (str "http://dict.tu-chemnitz.de/?query=" word) disabled)
      (button "PONS" (str "http://de.pons.eu/dict/search/results/?l=dede&q=" word) disabled)]]))

(defn- navbar [active]
  [:nav.navbar.navbar-default
   [:div.container-fluid
    [:div.navbar-header
     [:button.navbar-toggle.collapsed
      {:type "button"
       :data-toggle "collapse"
       :data-target "#navbar-collapse"
       :aria-expanded "false"}
      [:span.sr-only "Toggle navigation"]
      [:span.icon-bar]
      [:span.icon-bar]
      [:span.icon-bar]]
     [:a.navbar-brand {:href "/"} "Hyphenation"]]
    [:div#navbar-collapse.navbar-collapse.collapse
     [:ul.nav.navbar-nav.navbar-right
      [:li
       (if (= active :edit) {:class "active"} {})
       [:a {:href "/edit"} "Edit"]]]]]])

;; -------------------------
;; Views

(defn home-page []
  [:div.container
   [navbar :insert]
   [:h2 "Insert Hyphenations"]
   [:div.row
    [:div.col-md-6
     [new-hyphenation]]]
   [:h2 "Lookup"]
   [hyphenation-lookup @word]
   [:h2 "Similar words"]
   [:div.row
    [:table#hyphenations.table.table-striped
     [:thead [:tr [:th "Word"] [:th "Hyphenation"]]]
     [:tbody
      (for [[word pattern] (:hyphenations @app-state)]
        ^{:key word} [hyphenation-pattern pattern])]]]])

(defn edit-page []
  [:div.container
   [navbar :edit]
   [:h2 "Edit Hyphenations"]
   [:div.row
    [:div.col-md-6
     [spelling-filter]]
    [:div.col-md-6
     [hyphenation-filter word]]]
   [:div.row
    [:table#hyphenations.table.table-striped
     [:thead [:tr [:th "Word"] [:th "Hyphenation"] [:th ""]]]
     [:tbody
      (for [[word pattern] (:hyphenations @app-state)]
        ^{:key word} [hyphenation-pattern-editable-item pattern])]]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/edit" []
  (session/put! :current-page #'edit-page))

;; -------------------------
;; Initialize app

(defn mount-root []
  (reagent/render [current-page] (.getElementById js/document "app")))

(defn init! []
  (load-hyphenation-patterns! @spelling @word)
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
