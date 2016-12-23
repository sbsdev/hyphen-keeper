(ns hyphen-keeper.export
  (:require [clojure.java
             [io :as io]
             [shell :refer [sh]]]
            [clojure.string :as string]
            [hyphen-keeper
             [db :as db]
             [hyphenate :as hyphenate]]))

(def dictionaries {0 ["/tmp/whitelist_de_DE_OLDSPELL.txt"
                      "/tmp/hyph_de_DE_OLDSPELL.dic"
                      "dicts/hyph_de_DE_OLDSPELL.dic"]
                   1 ["/tmp/whitelist_de.txt"
                      "/tmp/hyph_de_DE.dic"
                      "dicts/hyph_de_DE.dic"]})

(defn- prepare-for-libhyphen
  "Prepare a hyphenation string for consumption by libhyphen"
  [s]
  (-> s
   (string/replace #"(.)" "$18") ; place "8" between each char
   (string/replace #"^(.*)$" ".$1.") ; suround with .
   (string/replace "8-8" "9") ; give hyphens more weight
   (string/replace "8." "."))) ; drop the last 8

(defn- get-hyphenations [spelling]
  (->>
   ;; get words for given spelling from db
   (db/read-words spelling)
   ;; filter the ones that aren't hyphenated correctly
   (remove
    (fn [{:keys [word hyphenation]}]
      (= (hyphenate/hyphenate spelling word) hyphenation)))
   (map :hyphenation)
   (remove string/blank?) ; drop empty ones
   (filter #(string/includes? % "-")) ; drop hyphenations w/o a hyphen
   (map string/lower-case) ; make sure it's lowercase
   sort
   (map prepare-for-libhyphen)))


(defn- write-file [words file-name original-dict-name]
  (with-open [w (io/writer file-name :encoding "ISO-8859-1")]
    ;; insert the original dict
    (io/copy (io/reader (io/resource original-dict-name) :encoding "ISO-8859-1") w)
    (doseq [word words]
      (.write w word)
      (.newLine w))))

;; I tried to run this in parallel (simply by using (dorun (pmap))
;; instead of (doseq)) but as it turns out the jobs are so uneven,
;; i.e. the first one is very small compared to the second one, we end
;; up waiting the same time.
(defn export []
  (let [program (.getAbsolutePath (io/file (io/resource "perl/substrings.pl")))]
    (doseq [[spelling [white-list dictionary original]] dictionaries]
      (->
       spelling
       get-hyphenations
       (write-file white-list original))
      (sh program white-list dictionary))))

