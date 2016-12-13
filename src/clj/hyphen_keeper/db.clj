(ns hyphen-keeper.db
  "Persistence for the hyphenation dictionary"
  (:require [yesql.core :refer [defqueries]]))

(def ^:private db "jdbc:mysql://localhost:3306/hyphenation?user=hyphenation&serverTimezone=UTC")

(defqueries "hyphen_keeper/queries.sql" {:connection db})

(defn read-words [spelling]
  "Return a coll of words for given `spelling`"
  (-> {:spelling spelling}
      words))

(defn save-word! [word hyphenation spelling]
  "Persist `word` with given `hyphenation` and `spelling`"
  (-> {:word word :hyphenation hyphenation :spelling spelling}
      save-word-internal!))

(defn remove-word! [word hyphenation spelling]
  (-> {:word word :hyphenation hyphenation :spelling spelling}
      remove-word-internal!))
