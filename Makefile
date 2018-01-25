compile:
	lein uberjar
clean:
	cat /dev/null > /tmp/journal.log
	lein clean
run:
	java -jar target/uberjar/coras-0.1.0-SNAPSHOT-standalone.jar
make check:
	cat /tmp/journal.txt
