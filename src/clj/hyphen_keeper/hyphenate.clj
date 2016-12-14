(ns hyphen-keeper.hyphenate
  (:require [clojure.java.io :as io])
  (:import ch.sbs.jhyphen.Hyphenator))

(def hyphenator (new Hyphenator (io/file "/usr/share/hyphen/hyph_de_DE.dic")))

(defn hyphenate
  "Hyphenate the given text by inserting the given `hyphen` char"
  ([spelling text]
   (hyphenate spelling text \-))
  ([spelling text hyphen]
   (.hyphenate hyphenator text hyphen nil)))
