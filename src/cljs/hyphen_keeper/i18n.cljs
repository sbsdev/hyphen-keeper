(ns hyphen-keeper.i18n)

;; the idea for this comes from
;; http://www.mattzabriskie.com/blog/detecting-locale
(defn lang-attribute []
  (.. js/document -documentElement (getAttribute "lang")))

(defn default-lang []
  (or (lang-attribute) "de"))

(def translations
  {:en {:brand "Hyphenation"
        :edit "Edit"
        :delete "Delete"
        :save "Save"
        :cancel "Cancel"
        :add "Add"
        :search "Search"
        :lookup "Lookup"
        :lookup-buttons "Buttons for hyphenation lookup"
        :insert-hyphenations "Insert Hyphenations"
        :edit-hyphenations "Edit Hyphenations"
        :toggle-nav "Toggle navigation"
        :delete-confirm "Really delete hyphenation pattern for '%1'?"
        :add-success "Word has been added"
        :add-fail "Failed to add hyphenation pattern: %1"
        :word "Word"
        :hyphenation "Hyphenation"
        :suggested-hyphenation "Suggested Hyphenation"
        :corrected-hyphenation "Corrected Hyphenation"
        :not-valid "Not valid"
        :already-defined "Word has already been defined. Use Edit to change it"
        :same-as-suggested "The hyphenation is the same as the suggestion"
        :similar "Similar words"
        :old-spelling "Old Spelling"
        :new-spelling "New Spelling"}
   :de {:brand "Trennungen"
        :edit "Editieren"
        :delete "Löschen"
        :save "Speichern"
        :cancel "Abbrechen"
        :add "Hinzufügen"
        :search "Suchen"
        :lookup "Nachschlagen"
        :lookup-buttons "Nachschlage-Schaltflächen"
        :insert-hyphenations "Trennungen einfügen"
        :edit-hyphenations "Trennungen editieren"
        :toggle-nav "Navigation umschalten"
        :delete-confirm "Trennung für '%1' wirklich löschen?"
        :add-success "Das Wort wurde hinzugefügt"
        :add-fail "Das Wort konnte nicht hinzugefügt werden: %1"
        :word "Wort"
        :hyphenation "Trennung"
        :suggested-hyphenation "Vorgeschlagene Trennung"
        :corrected-hyphenation "Korrigierte Trennung"
        :not-valid "Eingabe ungültig"
        :already-defined "Die Trennung ist schon definiert. Bitte benutzen Sie 'Editieren' um sie zu ändern"
        :same-as-suggested "Die Trennung ist gleich wie der Trenn-Vorschlag"
        :similar "Ähnliche Wörter"
        :old-spelling "Alte Rechtschreibung"
        :new-spelling "Neue Rechtschreibung"}})

