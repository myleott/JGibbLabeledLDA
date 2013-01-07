JAVAC=javac
JAR=lib/args4j-2.0.6.jar
OPTS=-Xlint:unchecked

src/jgibblda/%.class: src/jgibblda/%.java
	$(JAVAC) -cp $(JAR) $(OPTS) $^

all: src/jgibblda/*.class
	mkdir -p bin/jgibblda
	mv $^ bin/jgibblda
	cp $(JAR) bin/jgibblda
