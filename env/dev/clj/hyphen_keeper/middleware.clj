(ns hyphen-keeper.middleware
  (:require [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware
             [defaults :refer [api-defaults site-defaults wrap-defaults]]
             [json :refer [wrap-json-params wrap-json-response]]
             [reload :refer [wrap-reload]]]))

(defn wrap-api-middleware [handler]
  (-> handler
      wrap-json-params
      wrap-json-response
      (wrap-defaults api-defaults)
      wrap-exceptions
      wrap-reload))

(defn wrap-site-middleware [handler]
  (-> handler
      (wrap-defaults site-defaults)
      wrap-exceptions
      wrap-reload))
