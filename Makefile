compile:
	lein with-profile prod uberjar
clean:
	cat /dev/null > /tmp/journal.out
	lein clean
run:
	java -jar target/uberjar/coras-0.1.0-SNAPSHOT-standalone.jar < resources/events.in
make check:
	cat /tmp/journal.out
