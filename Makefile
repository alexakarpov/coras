compile: clean
	lein with-profile prod uberjar
clean:
	cat /dev/null > /tmp/journal.out
	lein clean
run: compile
	lein ring server
make check:
	cat /tmp/journal.out
