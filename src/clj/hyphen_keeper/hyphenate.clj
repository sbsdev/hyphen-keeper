(ns hyphen-keeper.hyphenate
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import ch.sbs.jhyphen.Hyphenator))

(def hyphen-dictionaries
  {0 "/usr/share/hyphen/hyph_de_DE_OLDSPELL.dic"
   1 "/usr/share/hyphen/hyph_de_DE.dic"})

(def hyphenators
  (zipmap
   (keys hyphen-dictionaries)
   (map #(new Hyphenator (io/file %)) (vals hyphen-dictionaries))))

(defn hyphenate
  "Hyphenate the given text by inserting the given `hyphen` char"
  ([spelling text]
   (hyphenate spelling text \-))
  ([spelling text hyphen]
   (if (string/blank? text)
     ""
     (let [hyphenator
           (get hyphenators
                ;; if a valid spelling was provided use it, otherwise
                ;; just use new spelling
                (if (contains? hyphen-dictionaries spelling) spelling 1))]
       (.hyphenate hyphenator text hyphen nil)))))
