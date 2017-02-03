(ns hyphen-keeper.middleware
  (:require [ring.middleware
             [accept :refer [wrap-accept]]
             [defaults :refer [api-defaults site-defaults wrap-defaults]]
             [json :refer [wrap-json-params wrap-json-response]]]))

(defn wrap-api-middleware [handler]
  (-> handler
      wrap-json-params
      wrap-json-response
      (wrap-defaults api-defaults)))

(defn wrap-site-middleware [handler]
  (-> handler
      (wrap-defaults site-defaults)
      (wrap-accept {:language ["en" "de"]})))
