.PHONY := clean generate

GRADLE_QUITE_ARGS := --quiet --console plain

GENERATED_FILES := src/javadoc/org/jivesoftware/smack/tcp/doc-files/XmppNioTcpConnectionStateGraph.png
generate: $(GENERATED_FILES)

clean:
	rm -f $(GENERATED_FILES)

src/javadoc/org/jivesoftware/smack/tcp/doc-files/XmppNioTcpConnectionStateGraph.png: src/main/java/org/jivesoftware/smack/tcp/XmppNioTcpConnection.java ../smack-core/src/main/java/org/jivesoftware/smack/fsm/AbstractXmppStateMachineConnection.java
	gradle $(GRADLE_QUITE_ARGS) :smack-repl:printXmppNioTcpConnectionStateGraph | dot -Tpng -o $@
