(ns hyphen-keeper.export
  (:require [clojure.core.async :as async]
            [clojure.java
             [io :as io]
             [shell :refer [sh]]]
            [clojure.string :as string]
            [clojure.tools.logging :as log]
            [hyphen-keeper
             [db :as db]
             [hyphenate :as hyphenate]]
            [org.tobereplaced.nio.file :as nio])
  (:import java.nio.file.StandardCopyOption))

(def ^:private substrings-program
  "Program to prepare the hyphenation dics. Expected to be installed
  on the system."
  "/usr/share/libhyphen/substrings.pl")

(def dictionaries {0 ["/tmp/whitelist_de_DE_OLDSPELL.txt"
                      "/usr/share/hyphen/generated/hyph_de_DE_OLDSPELL.dic"
                      "dicts/hyph_de_DE_OLDSPELL.dic"]
                   1 ["/tmp/whitelist_de.txt"
                      "/usr/share/hyphen/generated/hyph_de_DE.dic"
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
      (= (hyphenate/hyphenate-against-base word spelling) hyphenation)))
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

(defn- export*
  "Export all hyphenation patterns from the database and prepare for
  libhyphen consumption, i.e. run them through substrings.pl"
  []
  ;; I tried to run this in parallel (simply by using (dorun (pmap))
  ;; instead of (doseq)) but as it turns out the jobs are so uneven,
  ;; i.e. the first one is very small compared to the second one, we
  ;; end up waiting the same amount of time.
  (doseq [[spelling [white-list dictionary original]] dictionaries]
    (->
     spelling
     get-hyphenations
     (write-file white-list original))
    (log/infof "Wrote the white-list %s" white-list)
    (let [tmp-file (nio/absolute-path (nio/create-temp-file! "hyphen-" ".dic"))]
      (sh substrings-program white-list (str tmp-file))
      (log/infof "Ran substrings.pl on %s producing %s" white-list tmp-file)
      (nio/move! tmp-file dictionary StandardCopyOption/REPLACE_EXISTING)
      (log/infof "Move %s to %s" tmp-file dictionary))))

(defn- exporter
  "Create a channel and attach a listener to it so that events can be
  debounced, i.e. while an export is pending only store one more
  request. Return the channel where export requests can be sent to."
  []
  ;; we want to debounce, i.e. rate limit the amount of exports (see
  ;; https://en.wikipedia.org/wiki/Switch#Contact_bounce.) In other
  ;; words do not initiate another export while you are still doing
  ;; one. For that we simply use a dropping buffer of size one which
  ;; makes sure that we remember any request that came in while we
  ;; were blocked doing the export.
  (let [debounce-chan (async/chan (async/dropping-buffer 1))]
    (async/go-loop []
      (when-let [_ (async/<! debounce-chan)]
        (export*)
        (recur)))
    debounce-chan))

(def export-chan (exporter))

(defn export
  "Like [export*] but with debouncing"
  []
  (async/>!! export-chan true))
