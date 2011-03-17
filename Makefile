all: compile

compile:
	scalac -unchecked -deprecation -d bin/ minidb/*.scala minidb/*/*.scala

run: compile runonly

runonly:
	scala -cp bin minidb.MiniDB

test: compile
	scala -cp bin minidb.tests.RunTests

clean:
	rm -rf bin/minidb

.PHONY: all compile run runonly test clean
