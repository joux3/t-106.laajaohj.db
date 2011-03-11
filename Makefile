all: compile

compile:
	scalac -unchecked -deprecation minidb/*.scala minidb/*/*.scala

run: compile runonly

runonly:
	scala minidb.MiniDB

test: compile
	scala minidb.tests.RunTests

clean:
	rm minidb/*.class minidb/*/*.class

.PHONY: all compile run runonly test clean
