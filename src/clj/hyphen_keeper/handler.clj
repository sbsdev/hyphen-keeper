(ns hyphen-keeper.handler
  (:require [clojure.string :as string]
            [compojure
             [coercions :refer [as-int]]
             [core :refer [defroutes DELETE GET POST PUT]]
             [route :refer [not-found resources]]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hyphen-keeper
             [db :as db]
             [export :as export]
             [hyphenate :as hyphenate]
             [middleware :refer [wrap-api-middleware wrap-site-middleware]]]
            [immutant.util :refer [context-path]]
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
   [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
   [:meta {:name "viewport"
           :content "width=device-width, initial-scale=1"}]
   (include-css "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css")])

(defn loading-page [lang]
  (html5
   {:lang lang}
   (head)
   [:body {:class "body-container"}
    mount-target
    (include-js (str (context-path) "/js/app.js"))]))

(defn- word-list [search spelling offset max-rows]
  (let [resp (if (string/blank? search)
               (db/read-words-paginated spelling (or (as-int offset) 0) (or (as-int max-rows) 100))
               (db/search-words spelling search))]
    (response/response resp)))

(defn- word-add [word hyphenation spelling]
  (db/save-word! word hyphenation spelling)
  (export/export)
  (response/created (str "/api/words/" word)))

(defn- word-delete [word spelling]
  (db/remove-word! word spelling)
  (export/export)
  (-> nil
   response/response
   (response/status 204)))

(defroutes api-routes
  (GET "/hyphenate" [word spelling :<< as-int] (hyphenate/hyphenate word spelling))
  (GET "/words" [search spelling  :<< as-int offset max-rows] (word-list search spelling offset max-rows))
  (POST "/words" [word hyphenation spelling] (word-add word hyphenation spelling))
  (PUT "/words" [word hyphenation spelling] (word-add word hyphenation spelling))
  (DELETE "/words/:word" [word spelling] (word-delete word spelling))
  (not-found "Not Found"))

(defroutes site-routes
  (GET "/" {{language :language} :accept} (loading-page language))
  (resources "/")
  (not-found "Not Found"))

(def site
  (wrap-site-middleware site-routes))

(def api
  (wrap-api-middleware api-routes))
