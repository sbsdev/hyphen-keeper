(ns hyphen-keeper.db
  "Persistence for the hyphenation dictionary"
  (:require [yesql.core :refer [defqueries]])
  (:import java.sql.SQLSyntaxErrorException))

(def ^:private db {:name "java:jboss/datasources/hyphenations"})

(defqueries "db/queries.sql" {:connection db})

(defn read-words
  "Return a coll of words for given `spelling`"
  [spelling]
  (-> {:spelling spelling} words))

(defn read-words-paginated
  "Return a coll of words for given `spelling` using `max-rows` and `offset` for pagination"
  [spelling offset max-rows]
  (-> {:spelling spelling :max_rows max-rows :offset offset}
      words-paginated))

(defn search-words
  "Return a coll of words for given `spelling` and given `search` term"
  [spelling search]
  (try
    (-> {:spelling spelling :search search}
        words-search)
    ;; ignore exceptions due to incomplete regexps
    (catch SQLSyntaxErrorException e ())))

(defn save-word!
  "Persist `word` with given `hyphenation` and `spelling`"
  [word hyphenation spelling]
  (-> {:word word :hyphenation hyphenation :spelling spelling}
      save-word-internal!))

(defn remove-word! [word spelling]
  (-> {:word word :spelling spelling}
      remove-word-internal!))
