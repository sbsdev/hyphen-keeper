(ns hyphen-keeper.core
  (:require [accountant.core :as accountant]
            [ajax.core :as ajax]
            [clojure.string :as string]
            [hyphen-keeper.i18n :as i18n]
            [hyphen-keeper.util :refer [hyphenation-valid?]]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]
            [taoensso.tempura :as tempura]))

(def lang (keyword (i18n/default-lang)))
(def tr (partial tempura/tr {:dict i18n/translations} [lang]))

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
(def hyphenations (reagent/cursor app-state [:hyphenations]))
(def hyphenation (reagent/cursor app-state [:hyphenation]))
(def suggested-hyphenation (reagent/cursor app-state [:suggested-hyphenation]))
(def feedback (reagent/cursor app-state [:feedback]))

(defn update-hyphenations! [f & args]
  (apply swap! app-state update-in [:hyphenations] f args))

(defn remove-hyphenation! [{:keys [word]}]
  (update-hyphenations! dissoc word))

(defn set-feedback! [message kind]
  (reset! feedback {:message message :kind kind})
  (.setTimeout js/window #(reset! feedback {:message "" :kind :none}) 5000))

(defn load-hyphenation-patterns!
  [spelling word]
  (ajax/GET "/api/words"
   :params {:spelling spelling :search word}
   :handler (fn [hyphenations]
              (swap! app-state assoc :hyphenations
                     (into (sorted-map)
                           (map (fn [{:keys [word] :as pattern}] [word pattern])
                                hyphenations))))
   :error-handler (fn [details]
                    (.warn js/console
                           (str "Failed to refresh hyphenation patterns from server: " details)))
   :response-format :json
   :keywords? true))

(defn add-hyphenation-pattern!
  [pattern on-success on-error]
  (ajax/POST "/api/words"
   :params pattern
   :handler on-success
   :error-handler on-error
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
  "Fetch the hyphenation pattern from the server for given `spelling`
  and `word`. If a `handler` is given invoke it on success. Otherwise
  use a default handler which updates the `app-state` in
  `:suggested-hyphenation` and `:hyphenation`."
  ([spelling word]
   (let [handler (fn [hyphenated]
                   (do
                     (swap! app-state assoc :suggested-hyphenation hyphenated)
                     (swap! app-state assoc :hyphenation hyphenated)))]
     (lookup-hyphenation-pattern! spelling word handler)))
  ([spelling word handler]
   (ajax/GET "/api/hyphenate"
             :params {:word word :spelling spelling}
             :handler handler
             :error-handler (fn [details]
                              (.warn js/console
                                     (str "Failed to lookup hyphenation patterns for word: " details))))))

(defn hyphenation-pattern-ui [{:keys [word hyphenation]}]
  [:tr
   [:td word]
   [:td hyphenation]])

(defn- hyphenation-pattern-readonly-ui
  [word hyphenation start remove]
  [:tr
   [:td word]
   [:td hyphenation]
   [:td
    [:div.btn-group
     [:button.btn.btn-default
      {:on-click start}
      [:span.glyphicon.glyphicon-edit {:aria-hidden true}] " " (tr [:edit])]
     [:button.btn.btn-default
      {:on-click #(when (js/confirm (tr [:delete-confirm] [word])) (remove))}
      [:span.glyphicon.glyphicon-trash {:aria-hidden true}] " " (tr [:delete])]]]])

(defn- hyphenation-pattern-edit-ui
  [word new-hyphenation new-suggestion stop save]
  (let [valid? (hyphenation-valid? @new-hyphenation word)
        same-as-suggested? (= @new-hyphenation @new-suggestion)
        klass (cond
                (not valid?) "has-error"
                same-as-suggested? "has-warning")
        help-text (cond
                    (not valid?) (tr [:not-valid])
                    same-as-suggested? (tr [:same-as-suggested]))]
    [:tr
     [:td word]
     [:td
      [:div.form-group
       {:class klass}
       [:input.form-control
        {:type "text"
         :auto-focus true
         :aria-describedby "hyphenationEditHelp"
         :on-change #(reset! new-hyphenation (-> % .-target .-value))
         :on-key-down #(case (.-which %)
                         27 (stop)
                         nil)
         :value @new-hyphenation}]
       (when help-text
         [:span#hyphenationEditHelp.help-block help-text])]]
     [:td
      [:div.btn-group
       [:button.btn.btn-default
        {:on-click save
         :disabled (when (or (not valid?) same-as-suggested?) "disabled")}
        [:span.glyphicon.glyphicon-ok {:aria-hidden true}] " " (tr [:save])]
       [:button.btn.btn-default
        {:on-click stop}
        [:span.glyphicon.glyphicon-remove {:aria-hidden true}] " " (tr [:cancel])]]]]))

(defn hyphenation-pattern-item-ui [{:keys [hyphenation] :as pattern}]
  (let [editing (reagent/atom false)
        new-hyphenation (reagent/atom hyphenation)
        new-suggestion (reagent/atom "")]
    (fn [{:keys [word hyphenation] :as pattern}]
      (let [stop #(do (reset! new-hyphenation hyphenation)
                      (reset! editing false))
            start #(do (reset! editing true)
                       (lookup-hyphenation-pattern!
                        @spelling word
                        (fn [hyphenation] (reset! new-suggestion hyphenation))))
            save #(do (when (hyphenation-valid? @new-hyphenation word)
                        (let [pattern (assoc pattern :hyphenation @new-hyphenation)
                              on-success (fn []
                                           (swap! app-state assoc-in [:hyphenations word] pattern)
                                           (reset! editing false))
                              on-error (fn [details]
                                         (.warn js/console
                                          (str "Failed to update hyphenation pattern: " details)))]
                          (add-hyphenation-pattern! pattern on-success on-error))))
            remove #(remove-hyphenation-pattern! pattern)]
        (if-not @editing
          [hyphenation-pattern-readonly-ui word hyphenation start remove]
          [hyphenation-pattern-edit-ui word new-hyphenation new-suggestion stop save])))))

(defn word-ui []
  (let [label (tr [:word])
        already-defined? (contains? @hyphenations @word)
        klass (when already-defined? "has-warning")
        help-text (when already-defined?
                    (tr [:already-defined]))]
    [:div.form-group
     {:class klass}
     [:label.control-label {:for "wordInput"} label]
     [:input.form-control
      {:id "wordInput"
       :type "text"
       :placeholder label
       :auto-focus true
       :aria-describedby "wordHelp"
       :value @word
       :on-change (fn [e]
                    (reset! word (-> e .-target .-value string/lower-case))
                    (load-hyphenation-patterns! @spelling @word)
                    (lookup-hyphenation-pattern! @spelling @word))}]
     (when help-text
       [:span#wordHelp.help-block help-text])]))

(defn hyphenation-ui []
  (let [label (tr [:corrected-hyphenation])
        blank? (or (string/blank? @word) (string/blank? @suggested-hyphenation))
        valid? (or blank? (hyphenation-valid? @hyphenation @word))
        same-as-suggested? (and (not blank?) (= @hyphenation @suggested-hyphenation))
        klass (cond
                (not valid?) "has-error"
                same-as-suggested? "has-warning")
        help-text (cond
                    (not valid?) (tr [:not-valid])
                    same-as-suggested? (tr [:same-as-suggested]))]
    [:div.form-group
     {:class klass}
     [:label.control-label {:for "hyphenationInput"} label]
     [:input.form-control
      {:type "text"
       :placeholder label
       :aria-describedby "hyphenationHelp"
       :value @hyphenation
       :on-change #(reset! hyphenation (-> % .-target .-value string/lower-case))}]
     (when help-text
       [:span#hyphenationHelp.help-block help-text])]))

(defn- hyphenation-add-ui []
  [:div.form-group
   [:button.btn.btn-default
    {:on-click #(when (and (not (string/blank? @word))
                           (hyphenation-valid? @hyphenation @word)
                           (not (contains? @hyphenations @word))
                           (not= @hyphenation @suggested-hyphenation))
                  (let [pattern {:word @word :hyphenation @hyphenation :spelling @spelling}
                        on-success (fn []
                                     (set-feedback! (tr [:add-success]) :success)
                                     ;; reset all form fields as if we had submitted the form
                                     (reset! word "")
                                     (reset! suggested-hyphenation "")
                                     (reset! hyphenation "")
                                     ;; reload the patterns
                                     (load-hyphenation-patterns! @spelling @word))
                        on-error (fn [details]
                                   (set-feedback!
                                    (tr [:add-fail] [details])
                                    :danger))]
                    (add-hyphenation-pattern! pattern on-success on-error)))
     :disabled (when (or (string/blank? @word)
                         (not (hyphenation-valid? @hyphenation @word))
                         (contains? @hyphenations @word)
                         (= @hyphenation @suggested-hyphenation))
                 "disabled")}
    (tr [:add])]])

(defn feedback-ui [feedback]
  (let [{:keys [kind message]} @feedback
        klass (when-not (= kind :none) (str "alert-" (name kind)))]
    (when-not (= kind :none)
      [:div.alert {:class klass :role "alert"} message])))

(defn spelling-ui []
  [:div.form-group
   [:select {:value @spelling
             :on-change (fn [e]
                          (reset! spelling (-> e .-target .-value js/parseInt))
                          (load-hyphenation-patterns! @spelling @word)
                          (lookup-hyphenation-pattern! @spelling @word))}
    [:option {:value 0} (tr [:old-spelling])]
    [:option {:value 1} (tr [:new-spelling])]]])

(defn search-ui [search]
  [:input.form-control
   {:type "text"
    :placeholder (tr [:search])
    :value @search
    :on-change (fn [e]
                 (reset! search (-> e .-target .-value))
                 (load-hyphenation-patterns! @spelling @search))}])

(defn suggested-hyphenation-ui []
  (let [id "suggestedHyphenation"
        label (tr [:suggested-hyphenation])]
    [:div.form-group
     [:label {:for id} label]
     [:input.form-control
      {:id id
       :type "text"
       :placeholder label
       :disabled "disabled"
       :value @suggested-hyphenation}]]))

(defn- button [label href disabled]
  [:a.btn.btn-default {:href href :target "_blank" :disabled disabled} label])

(defn hyphenation-lookup-ui [spelling word]
  (when (= @spelling 1)
    [:div
     [:h2 (tr [:lookup])]
     (let [disabled (when (string/blank? word) "disabled")]
       [:div.row
        [:div.btn-group {:role "group"
                         :aria-label (tr [:lookup-buttons])}
         (button "Duden" (str "http://www.duden.de/suchen/dudenonline/" word) disabled)
         (button "TU Chemnitz" (str "http://dict.tu-chemnitz.de/?query=" word) disabled)
         (button "PONS" (str "http://de.pons.eu/dict/search/results/?l=dede&q=" word) disabled)]])]))

(defn- navbar-ui [active]
  [:nav.navbar.navbar-default
   [:div.container-fluid
    [:div.navbar-header
     [:button.navbar-toggle.collapsed
      {:type "button"
       :data-toggle "collapse"
       :data-target "#navbar-collapse"
       :aria-expanded "false"}
      [:span.sr-only (tr [:toggle-nav])]
      [:span.icon-bar]
      [:span.icon-bar]
      [:span.icon-bar]]
     [:a.navbar-brand {:href "/"} (tr [:brand])]]
    [:div#navbar-collapse.navbar-collapse.collapse
     [:ul.nav.navbar-nav.navbar-right
      [:li
       (if (= active :edit) {:class "active"} {})
       [:a {:href "/edit"} (tr [:edit])]]]]]])

;; -------------------------
;; Views

(defn home-page-ui []
  [:div.container
   [navbar-ui :insert]
   [:h2 (tr [:insert-hyphenations])]
   [:div.row
    [:div.col-md-6
     [:div.form
      [spelling-ui]
      [word-ui]
      [suggested-hyphenation-ui]
      [hyphenation-ui]
      [hyphenation-add-ui]
      [feedback-ui feedback]]]]
   [hyphenation-lookup-ui spelling @word]
   [:h2 (tr [:similar])]
   [:div.row
    [:table#hyphenations.table.table-striped
     [:thead [:tr [:th (tr [:word])] [:th (tr [:hyphenation])]]]
     [:tbody
      (for [[word pattern] (:hyphenations @app-state)]
        ^{:key word} [hyphenation-pattern-ui pattern])]]]])

(defn edit-page-ui []
  [:div.container
   [navbar-ui :edit]
   [:h2 (tr [:edit-hyphenations])]
   [:div.row
    [:div.col-md-6
     [spelling-ui]]
    [:div.col-md-6
     [search-ui word]]]
   [:div.row
    [:table#hyphenations.table.table-striped
     [:thead [:tr [:th (tr [:word])] [:th (tr [:hyphenation])] [:th ""]]]
     [:tbody
      (for [[word pattern] (:hyphenations @app-state)]
        ^{:key word} [hyphenation-pattern-item-ui pattern])]]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page-ui))

(secretary/defroute "/edit" []
  (session/put! :current-page #'edit-page-ui))

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
