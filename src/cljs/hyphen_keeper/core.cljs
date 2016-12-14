(ns hyphen-keeper.core
  (:require [accountant.core :as accountant]
            [ajax.core :as ajax]
            [clojure.string :as string]
            [reagent.core :as reagent]
            [reagent.session :as session]
            [secretary.core :as secretary :include-macros true]))

(defonce app-state
  (reagent/atom
   {:hyphenations []
    :search ""
    :spelling 1}))

(def search (reagent/cursor app-state [:search]))
(def spelling (reagent/cursor app-state [:spelling]))

(defn update-hyphenations! [f & args]
  (apply swap! app-state update-in [:hyphenations] f args))

(defn add-hyphenation! [h]
  (update-hyphenations! conj h))

(defn remove-hyphenation! [h]
  (update-hyphenations! (fn [hs] (vec (remove #(= % h) hs))) h))

(defn load-hyphenation-patterns!
  [spelling search]
  (ajax/GET "/api/words"
            :params {:spelling spelling :search search}
            :handler (fn [hyphenations] (swap! app-state assoc :hyphenations hyphenations))
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

(defn hyphenation-pattern [pattern]
  [:tr
   [:td (:word pattern)]
   [:td (:hyphenation pattern)]
   [:td
    [:button.btn.btn-default
     {:on-click #(remove-hyphenation-pattern! pattern)}
     [:span.glyphicon.glyphicon-remove {:aria-hidden true}] " Delete"]]])

(defn word-field [val]
  [:div.form-group
   [:label.sr-only {:for "wordInput"} "Word"]
   [:input.form-control
    {:id "wordInput"
     :type "text"
     :placeholder "Word"
     :value @val
     :on-change #(reset! val (-> % .-target .-value))}]])

(defn hyphenation-field [val]
  [:div.form-group
   [:label.sr-only {:for "hyphenationInput"} "Hyphenation"]
   [:input.form-control
    {:type "text"
     :placeholder "Hyphenation"
     :value @val
     :on-change #(reset! val (-> % .-target .-value))}]])

(defn new-hyphenation []
  (let [word (reagent/atom "")
        hyphenation (reagent/atom "")]
    (fn []
      [:div.form-inline
       [word-field word]
       [hyphenation-field hyphenation]
       [:button.btn.btn-default
        {:on-click #(when (and @word @hyphenation)
                      (add-hyphenation-pattern! {:word @word :hyphenation @hyphenation :spelling @spelling})
                      (reset! word "")
                      (reset! hyphenation ""))
         :disabled (when (or (string/blank? @word)
                             (string/blank? @hyphenation))
                     "disabled")}
        "Add"]])))

(defn hyphenation-filter [search]
  [:input.form-control
   {:type "text"
    :placeholder "Search"
    :value @search
    :on-change (fn [e]
                 (reset! search (-> e .-target .-value))
                 (load-hyphenation-patterns! @spelling @search))}])

(defn spelling-filter []
  [:select {:value @spelling
            :on-change (fn [e]
                         (reset! spelling (-> e .-target .-value))
                         (load-hyphenation-patterns! @spelling @search))}
   [:option {:value 0} "Old"]
   [:option {:value 1} "New"]])

(defn hyphenation-list []
  [:div.container
   [:h1 "Hyphenation list"]
   [:div.row
    [:div.col-md-6
     [hyphenation-filter search]]
    [:div.col-md-6
     [spelling-filter]]]
    [:div.row
    [:table#hyphenations.table.table-striped
     [:thead [:tr [:th "Word"] [:th "Hyphenation"]]]
     [:tbody
      (for [h (sort-by :word (:hyphenations @app-state))]
        ^{:key (:word h)} [hyphenation-pattern h])]]]
   [:div.row
    [new-hyphenation]]])

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
  (load-hyphenation-patterns! @spelling @search)
  (accountant/configure-navigation!
    {:nav-handler
     (fn [path]
       (secretary/dispatch! path))
     :path-exists?
     (fn [path]
       (secretary/locate-route path))})
  (accountant/dispatch-current!)
  (mount-root))
