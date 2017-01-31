(ns hyphen-keeper.hyphenate
  "Functionality to hyphenate words for a given spelling.

  The list of provided hyphenators is stateful. After a change of the
  hyphenation dictionaries in the file system the hyphenators list
  needs to be reloaded."
  (:require [clojure.java.io :as io]
            [clojure.string :as string])
  (:import ch.sbs.jhyphen.Hyphenator))

(def hyphen-dictionaries
  {0 "/usr/share/hyphen/hyph_de_DE_OLDSPELL.dic"
   1 "/usr/share/hyphen/hyph_de_DE.dic"})

(def base-hyphen-dictionaries
  {0 (io/resource "dicts/hyph_de_DE_OLDSPELL.dic")
   1 (io/resource "dicts/hyph_de_DE.dic")})

(defn- load-hyphenators
  "Given a map of keys for spelling and paths to hyphenation
  dictionaries return a map of keys and materialized Hyphenators
  constructed from the dictionaries in the paths."
  [dics]
  (zipmap
    (keys dics)
    (map #(new Hyphenator (io/file %)) (vals dics))))

(def hyphenators
  "A stateful map of Hyphenators"
  (atom (load-hyphenators hyphen-dictionaries)))

(def base-hyphenators
  "The \"base hyphenators\" that use the hyphen tables as they are
  provided by upstream without the exceptions from the database"
  (load-hyphenators base-hyphen-dictionaries))

(defn reload!
  "Reload the hyphenator map. Typically this is needed when the
  hyphenation dicts are re-generated."
  []
  (reset! hyphenators (load-hyphenators)))

(defn- hyphenate*
  "Hyphenate given `text` using a given `hyphenator`"
  [text hyphenator]
  (if (string/blank? text)
    ""
    (.hyphenate hyphenator text \- nil)))

(defn hyphenate
  "Hyphenate the given `text` for given `spelling`"
  [text spelling]
  (let [default (get @hyphenators 1) ;; new spelling
        hyphenator (get @hyphenators spelling default)]
    (hyphenate* text hyphenator)))

(defn hyphenate-against-base
  "Hyphenate the given `text` for given `spelling` against the base
  upstream dictionaries which contain no exceptions"
  [text spelling]
  (let [default (get base-hyphenators 1) ;; new spelling
        hyphenator (get base-hyphenators spelling default)]
    (hyphenate* text hyphenator)))
