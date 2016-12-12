-- name: words
-- Get the words for given spelling
SELECT word, hyphenation, spelling
FROM words
WHERE spelling = :spelling

-- name: save-word-internal!
-- Insert or update the given `word` to the dictionary.
INSERT INTO words (word, hyphenation, spelling)
VALUES (:word, :hyphenation, :spelling)
ON DUPLICATE KEY UPDATE
hyphenation = values(hyphenation);
