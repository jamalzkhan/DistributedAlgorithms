JC = javac
SRCDIR = src
BINDIR = bin
JFLAGS = -g:none
.SUFFIXES: .java .class .out .err
.java.class:
	@$(JC) $(JFLAGS) $*.java
CLASSES = $(wildcard *.java)
default: classes
classes: $(CLASSES:.java=.class)
clean:
	$(RM) *.class
	$(RM) *.out
	$(RM) *.err
