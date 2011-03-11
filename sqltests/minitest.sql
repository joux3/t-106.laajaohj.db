-- Simple test containing a few SQL commands
-- rm minitest.db; sqlite3 minitest.db <minitest.sql

CREATE TABLE foo (a TEXT, b INT, c DOUBLE);

INSERT INTO foo VALUES ("qwer", 3, 1.5);
INSERT INTO foo VALUES ("tyui", 5, 4.3);
INSERT INTO foo VALUES ("abc", 9, 9.5);
INSERT INTO foo VALUES ("def", 7, 8.3);

CREATE INDEX fooindex ON foo (b);

SELECT * FROM foo;

SELECT * FROM foo WHERE b > 6;

SELECT * FROM foo WHERE b < c;

