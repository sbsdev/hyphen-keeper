-- name: words
-- Get the words for given spelling
SELECT word, hyphenation, spelling
FROM words
WHERE spelling = :spelling

-- name: words-paginated
-- Get the words for given spelling
SELECT word, hyphenation, spelling
FROM words
WHERE spelling = :spelling
LIMIT :offset, :max_rows

-- name: words-search
-- Get the words for given spelling that match the given search term
SELECT word, hyphenation, spelling
FROM words
WHERE spelling = :spelling
AND word REGEXP :search
LIMIT 100

-- name: save-word-internal!
-- Insert or update the given `word` to the dictionary.
INSERT INTO words (word, hyphenation, spelling)
VALUES (:word, :hyphenation, :spelling)
ON DUPLICATE KEY UPDATE
hyphenation = values(hyphenation);

-- name: remove-word-internal!
-- Delete the given `word` from the dictionary.
DELETE FROM words
WHERE word = :word
AND spelling = :spelling
