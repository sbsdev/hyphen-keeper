(ns hyphen-keeper.server
  (:gen-class)
  (:require [hyphen-keeper.handler :refer [site api]]
            [immutant.web :as web]))

(defn -main [& args]
  ;; start web server
  (web/run site)
  (web/run api :path "/api"))
