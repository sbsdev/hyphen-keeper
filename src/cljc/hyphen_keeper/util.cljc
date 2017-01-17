(ns hyphen-keeper.util
  (:require [clojure.string :as string]))

(defn hyphenation-valid?
  "Return true if the `hyphenation` is not blank, is equal to
  `word` (modulo the hyphenation marks) and only contains letters a-z,
  \u00DF-\u00FF and '-'"
  [hyphenation word]
  (and (not (string/blank? hyphenation))
       (= word (string/replace hyphenation "-" ""))
       (re-matches #"[a-z\xDF-\xFF-]+" hyphenation)))

