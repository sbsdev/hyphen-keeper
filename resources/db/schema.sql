-- Create a db
-- CREATE DATABASE hyphenation CHARACTER SET utf8 COLLATE utf8_bin;

-- Create a user
-- CREATE USER 'hyphenation'@'localhost' IDENTIFIED BY 'sekret';
-- GRANT ALL ON hyphenation.* TO 'hyphenation'@'localhost';
-- FLUSH PRIVILEGES;

-- Type of spelling
-- a classic reference table
CREATE TABLE spelling (
  id TINYINT PRIMARY KEY,
  name VARCHAR(256) NOT NULL
);

CREATE TABLE words (
  word VARCHAR(255) NOT NULL,
  hyphenation VARCHAR(255) NOT NULL,
  spelling TINYINT NOT NULL,
  PRIMARY KEY(word, spelling),
  FOREIGN KEY(spelling) REFERENCES spelling(id)
);

INSERT INTO spelling VALUES
(0, 'Orthographische Konferenz von 1901'),
(1, 'Deutsche Rechtschreibreform von 1996');
