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
       ;; only allow chars in the unicode range of Basic Latin (a-z),
       ;; Latin-1 Supplement (the printable chars in 00C0-00FF) and
       ;; Latin Extended-A (0100—017F), see
       ;; https://unicode-table.com/en/blocks/basic-latin,
       ;; https://unicode-table.com/en/blocks/latin-1-supplement and
       ;; https://unicode-table.com/en/blocks/latin-extended-a

       ;; Exclude Latin Small Letter Dotless I (0131) because it seems
       ;; to cause libhyphen to crash
       (some? (re-matches #"[a-z\xC0-\xFF\u0100-\u0130\u0132-\u017F-]+" hyphenation))))

