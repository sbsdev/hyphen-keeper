(ns hyphen-keeper.middleware
  (:require [ring.middleware
             [defaults :refer [api-defaults site-defaults wrap-defaults]]
             [json :refer [wrap-json-params wrap-json-response]]]))

(defn wrap-api-middleware [handler]
  (-> handler
      wrap-json-params
      wrap-json-response
      (wrap-defaults api-defaults)))

(defn wrap-site-middleware [handler]
  (wrap-defaults handler site-defaults))
