JAVAC=javac
CLASSPATH=-cp lib/args4j-2.0.6.jar
OPTS=-Xlint:unchecked

src/jgibblda/%.class: src/jgibblda/%.java
	$(JAVAC) $(OPTS) $(CLASSPATH) $^

all: src/jgibblda/*.class
	mkdir -p bin/jgibblda
	mv $^ bin/jgibblda
