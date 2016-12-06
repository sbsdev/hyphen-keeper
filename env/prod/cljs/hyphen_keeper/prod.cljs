(ns hyphen-keeper.prod
  (:require [hyphen-keeper.core :as core]))

;;ignore println statements in prod
(set! *print-fn* (fn [& _]))

(core/init!)
