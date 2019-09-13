(ns hyphen-keeper.util
  (:require [clojure.string :as string]))

(defn hyphenation-valid?
  "Return true if the `hyphenation` is not blank, is equal to
  `word` (modulo the hyphenation marks) and contains at least one of
  the letters 'a-z', '\u00DF-\u00FF' or '-'. Also each '-' in the
  hyphenation should be surrounded by letters."
  [hyphenation word]
  (and (not (string/blank? hyphenation))
       (= word (string/replace hyphenation "-" ""))
       (not (string/starts-with? hyphenation "-"))
       (not (string/ends-with? hyphenation "-"))
       (not (string/includes? hyphenation "--"))
       (some? (re-matches #"[a-z\xC0-\xFF\u0100-\u017F-]+" hyphenation))))

