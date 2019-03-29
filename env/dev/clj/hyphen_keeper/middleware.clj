(ns hyphen-keeper.middleware
  (:require [hiccup.middleware :refer [wrap-base-url]]
            [prone.middleware :refer [wrap-exceptions]]
            [ring.middleware
             [accept :refer [wrap-accept]]
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
      (wrap-accept {:language ["en" "de"]})
      wrap-base-url
      wrap-exceptions
      wrap-reload))
