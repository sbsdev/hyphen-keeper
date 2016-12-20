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
    :suggested-hyphenation ""}))

(def spelling (reagent/cursor app-state [:spelling]))
(def word (reagent/cursor app-state [:word]))
(def hyphenation (reagent/cursor app-state [:hyphenation]))
(def suggested-hyphenation (reagent/cursor app-state [:suggested-hyphenation]))

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

(defn add-hyphenation-pattern!
  [pattern]
  (ajax/POST "/api/words"
             :params pattern
             :handler (fn [] (add-hyphenation! pattern))
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

(defn hyphenation-pattern [pattern]
  [:tr
   [:td (:word pattern)]
   [:td (:hyphenation pattern)]
   [:td
    [:div.btn-group
     [:button.btn.btn-default
      [:span.glyphicon.glyphicon-edit {:aria-hidden true}] " Edit"]
     [:button.btn.btn-default
      {:on-click #(remove-hyphenation-pattern! pattern)}
      [:span.glyphicon.glyphicon-trash {:aria-hidden true}] " Delete"]]]])

(defn word-field []
  [:div.form-group
   [:label {:for "wordInput"} "Word"]
   [:input.form-control
    {:id "wordInput"
     :type "text"
     :placeholder "Word"
     :value @word
     :on-change (fn [e]
                  (reset! word (-> e .-target .-value string/lower-case))
                  (load-hyphenation-patterns! @spelling @word))
     :on-blur #(lookup-hyphenation-pattern! @spelling @word)}]])

(defn- hyphenation-valid? [s]
  (and (not (string/blank? s))
       (string/includes? s "-")
       (re-matches #"[a-z\xDF-\xFF-]+" s)))

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
  [:button.btn.btn-default
   {:on-click #(when (and @word (hyphenation-valid? @hyphenation))
                 (add-hyphenation-pattern! {:word @word :hyphenation @hyphenation :spelling @spelling})
                 (reset! word "")
                 (reset! hyphenation ""))
    :disabled (when (or (string/blank? @word)
                        (not (hyphenation-valid? @hyphenation))
                        (= @hyphenation @suggested-hyphenation))
                "disabled")}
   "Add"])

(defn spelling-filter []
  [:div.form-group
   [:select {:value @spelling
             :on-change (fn [e]
                          (reset! spelling (-> e .-target .-value))
                          (load-hyphenation-patterns! @spelling @word)
                          (lookup-hyphenation-pattern! @spelling @word))}
    [:option {:value 0} "Old Spelling"]
    [:option {:value 1} "New Spelling"]]])

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

(defn new-hyphenation []
  [:div.form
   [spelling-filter]
   [word-field]
   [suggested-hyphenation-field]
   [hyphenation-field]
   [hyphenation-add-button]])

(defn hyphenation-list []
  [:div.container
   [:h1 "Hyphenation"]
   [:div.row
    [:div.col-md-6
     [new-hyphenation]]]
   [:h1 "Similar words"]
   [:div.row
    [:table#hyphenations.table.table-striped
     [:thead [:tr [:th "Word"] [:th "Hyphenation"]]]
     [:tbody
      (for [[word pattern] (:hyphenations @app-state)]
        ^{:key word} [hyphenation-pattern pattern])]]]])

;; -------------------------
;; Views

(defn home-page []
  [hyphenation-list])

(defn about-page []
  [:div [:h2 "About hyphen-keeper"]
   [:div [:a {:href "/"} "go to the home page"]]])

(defn current-page []
  [:div [(session/get :current-page)]])

;; -------------------------
;; Routes

(secretary/defroute "/" []
  (session/put! :current-page #'home-page))

(secretary/defroute "/about" []
  (session/put! :current-page #'about-page))

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
