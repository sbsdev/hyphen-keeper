(ns hyphen-keeper.handler
  (:require [clojure.string :as string]
            [compojure
             [core :refer [context defroutes DELETE GET POST PUT]]
             [route :refer [not-found resources]]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hyphen-keeper
             [db :as db]
             [middleware :refer [wrap-api-middleware wrap-site-middleware]]]
            [ring.util.response :as response]))

(def mount-target
  [:div#app
      [:h3 "ClojureScript has not been compiled!"]
      [:p "please run "
       [:b "lein figwheel"]
       " in order to start the compiler"]])

(defn head []
  [:head
   [:meta {:charset "utf-8"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")])

(defn loading-page []
  (html5
    (head)
    [:body {:class "body-container"}
     mount-target
     (include-js "/js/app.js")]))

(defn- word-list [spelling search]
  (let [search-term (if-not (string/blank? search) search ".*")]
    (response/response (db/read-words spelling search-term))))

(defn- word-add [word hyphenation spelling]
  (db/save-word! word hyphenation spelling)
  (response/created (str "/api/words/" word)))

(defn- word-delete [word spelling]
  (db/remove-word! word spelling)
  (-> nil
   response/response
   (response/status 204)))

(defroutes api-routes
  (context "/api" []
   (GET "/words" [spelling search] (word-list spelling search))
   (POST "/words" [word hyphenation spelling] (word-add word hyphenation spelling))
   (PUT "/words" [word hyphenation spelling] (word-add word hyphenation spelling))
   (DELETE "/words/:word" [word spelling] (word-delete word spelling))))

(defroutes site-routes
  (GET "/" [] (loading-page))
  (resources "/")
  (not-found "Not Found"))

(defroutes app
  (wrap-api-middleware api-routes)
  (wrap-site-middleware site-routes))
