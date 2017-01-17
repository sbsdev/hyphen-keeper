(ns hyphen-keeper.db.load
  "Load the initial corpus from an XML file into the database"
  (:require [clojure
             [string :as string]
             [xml :as xml]
             [zip :as zip]]
            [clojure.data.zip.xml :refer [text xml-> xml1->]]
            [clojure.java
             [io :as io]
             [jdbc :as j]]
            [hyphen-keeper.util :refer [hyphenation-valid?]]))

(def ^:private db "jdbc:mysql://localhost:3306/hyphenation?user=hyphenation&serverTimezone=UTC")

(def ^:private param-mapping
  {:word [:ss]
   :hyphenation [:so]
   :spelling [:rs]})

(defn- clean-raw-item
  [{:keys [spelling] :as item}]
  (assoc item :spelling (if (= spelling "NEU") 1 0)))

(defn- read-file
  "Read the initial hyphenation data from an XML file"
  [file]
  (let [root (-> file io/file xml/parse zip/xml-zip)]
    (for [record (xml-> root :wort)]
      (->> (for [[key path] param-mapping
                 :let [val (some->
                            (apply xml1-> record path)
                            text
                            string/trim)]
                 :when (some? val)]
             [key val])
           (into {})
           clean-raw-item))))

(defn- valid-patterns [patterns]
  (filter
   (fn [{:keys [hyphenation word]}] (hyphenation-valid? hyphenation word))
   patterns))

(defn- initial-load
  [words]
  (j/insert-multi! db :words words))
