(ns hyphen-keeper.handler
  (:require [compojure
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

(defn- word-list [spelling]
  (response/response (db/read-words spelling)))

(defn- word-add [spelling word hyphenation]
  (db/save-word! word hyphenation spelling)
  (response/created (str "/api/" spelling "/" word)))

(defn- word-delete [spelling {:keys [word hyphenation]}]
  (db/remove-word! word hyphenation spelling)
  (response/response))

(defroutes api-routes
  (context "/api/:spelling" [spelling]
   (GET "/words" [] (word-list spelling))
   (POST "/words" [word hyphenation :as r] (do (println r) (word-add spelling word hyphenation)))
   (PUT "/words" [r] (word-add spelling r))
   (DELETE "/words" [r] (word-delete spelling r))))

(defroutes site-routes
  (GET "/" [] (loading-page))
  (resources "/")
  (not-found "Not Found"))

(defroutes app
  (wrap-api-middleware api-routes)
  (wrap-site-middleware site-routes))
