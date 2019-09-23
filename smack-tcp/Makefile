.PHONY := all clean

GRADLE_QUITE_ARGS := --quiet --console plain

XMPP_NIO_TCP_CONNECTION_STATE_GRAPH_PNG := src/javadoc/org/jivesoftware/smack/tcp/doc-files/XmppNioTcpConnectionStateGraph.png
XMPP_NIO_TCP_CONNECTION_STATE_GRAPH_DOT := $(XMPP_NIO_TCP_CONNECTION_STATE_GRAPH_PNG:.png=.dot)

GENERATED_FILES := $(XMPP_NIO_TCP_CONNECTION_STATE_GRAPH_PNG) $(XMPP_NIO_TCP_CONNECTION_STATE_GRAPH_DOT)

all: $(XMPP_NIO_TCP_CONNECTION_STATE_GRAPH_PNG)

clean:
	rm -f $(GENERATED_FILES)

%.png: %.dot
	dot -Tpng -o $@ $^

$(XMPP_NIO_TCP_CONNECTION_STATE_GRAPH_DOT): src/main/java/org/jivesoftware/smack/tcp/XmppNioTcpConnection.java ../smack-core/src/main/java/org/jivesoftware/smack/fsm/AbstractXmppStateMachineConnection.java
	# TODO: This also creates the dot file even if the command
	# fails. It would be better if this was not the case.
	gradle $(GRADLE_QUITE_ARGS) :smack-repl:printXmppNioTcpConnectionStateGraph > $@
