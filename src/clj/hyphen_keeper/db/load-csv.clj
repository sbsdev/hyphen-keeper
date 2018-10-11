(ns hyphen-keeper.db.load-csv
  "Load some corpus from a CSV file into the database"
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.java.jdbc :as jdbc]
            [clojure.set :as set]
            [hyphen-keeper.db :as db]
            [hyphen-keeper.util :refer [hyphenation-valid?]]))

(def ^:private db "jdbc:mysql://localhost:3306/hyphenation?user=hyphenation&serverTimezone=UTC")
(def spelling 1)

(defn- valid-words [words]
  (filter
   (fn [{:keys [hyphenation word]}] (hyphenation-valid? hyphenation word))
   words))

(defn- insert-words
  [words table]
  (jdbc/insert-multi! db table words))

(defn- update-words
  [words table]
  (doseq [word words]
    (jdbc/update! db table word ["word = ? and spelling = ?" (:word word) (:spelling word)])))

(defn csv->maps [csv]
  (map zipmap
       (repeat [:word :hyphenation :spelling])
       (map (fn [[word hyphenation]] [word hyphenation spelling]) csv)))

(defn- read-file
  "Read  hyphenation data from a csv file"
  [file]
  (with-open [reader (io/reader file)]
    (doall
     (csv/read-csv reader))))

(defn load-csv [file table]
  (let [words-new (-> file io/file read-file csv->maps valid-words)
        words-old (db/read-words spelling)
        old-set (set (map :word words-old))
        new-set (set (map :word words-new))
        insert-set (set/difference new-set old-set)
        update-set (set/intersection old-set new-set)
        words-to-insert (filter #(insert-set (:word %)) words-new)
        words-to-update (filter #(update-set (:word %)) words-new)]
    (insert-words words-to-insert table)
    (update-words words-to-update table)))
