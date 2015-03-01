/**
 *
 * Copyright the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack.debugger;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.ObservableReader;
import org.jivesoftware.smack.util.ObservableWriter;
import org.jivesoftware.smack.util.ReaderListener;
import org.jivesoftware.smack.util.WriterListener;
import org.jxmpp.util.XmppStringUtils;

import java.io.Reader;
import java.io.Writer;

public abstract class AbstractDebugger implements SmackDebugger {

    public static boolean printInterpreted = false;

    private final XMPPConnection connection;

    private final StanzaListener listener;
    private final ConnectionListener connListener;
    private final ReaderListener readerListener;
    private final WriterListener writerListener;

    private ObservableWriter writer;
    private ObservableReader reader;

    public AbstractDebugger(final XMPPConnection connection, Writer writer, Reader reader) {
        this.connection = connection;

        // Create a special Reader that wraps the main Reader and logs data to the GUI.
        this.reader = new ObservableReader(reader);
        readerListener = new ReaderListener() {
            public void read(String str) {
                log("RECV (" + connection.getConnectionCounter() + "): " + str);
            }
        };
        this.reader.addReaderListener(readerListener);

        // Create a special Writer that wraps the main Writer and logs data to the GUI.
        this.writer = new ObservableWriter(writer);
        writerListener = new WriterListener() {
            public void write(String str) {
                log("SENT (" + connection.getConnectionCounter() + "): " + str);
            }
        };
        this.writer.addWriterListener(writerListener);

        // Create a thread that will listen for all incoming packets and write them to
        // the GUI. This is what we call "interpreted" packet data, since it's the packet
        // data as Smack sees it and not as it's coming in as raw XML.
        listener = new StanzaListener() {
            public void processPacket(Stanza packet) {
                if (printInterpreted) {
                    log("RCV PKT (" + connection.getConnectionCounter() + "): " + packet.toXML());
                }
            }
        };

        connListener = new ConnectionListener() {
            public void connected(XMPPConnection connection) {
                log("XMPPConnection connected ("
                                + connection.getConnectionCounter() + ")");
            }
            public void authenticated(XMPPConnection connection, boolean resumed) {
                String logString = "XMPPConnection authenticated (" + connection.getConnectionCounter() + ")";
                if (resumed) {
                    logString += " and resumed";
                }
                log(logString);
            }
            public void connectionClosed() {
                log(
                       "XMPPConnection closed (" +
                        connection.getConnectionCounter() +
                        ")");
            }

            public void connectionClosedOnError(Exception e) {
                log(
                        "XMPPConnection closed due to an exception (" +
                        connection.getConnectionCounter() +
                        ")");
                e.printStackTrace();
            }
            public void reconnectionFailed(Exception e) {
                log(
                        "Reconnection failed due to an exception (" +
                        connection.getConnectionCounter() +
                        ")");
                e.printStackTrace();
            }
            public void reconnectionSuccessful() {
                log(
                        "XMPPConnection reconnected (" +
                        connection.getConnectionCounter() +
                        ")");
            }
            public void reconnectingIn(int seconds) {
                log(
                        "XMPPConnection (" +
                        connection.getConnectionCounter() +
                        ") will reconnect in " + seconds);
            }
        };
    }

    protected abstract void log(String logMessage);

    public Reader newConnectionReader(Reader newReader) {
        reader.removeReaderListener(readerListener);
        ObservableReader debugReader = new ObservableReader(newReader);
        debugReader.addReaderListener(readerListener);
        reader = debugReader;
        return reader;
    }

    public Writer newConnectionWriter(Writer newWriter) {
        writer.removeWriterListener(writerListener);
        ObservableWriter debugWriter = new ObservableWriter(newWriter);
        debugWriter.addWriterListener(writerListener);
        writer = debugWriter;
        return writer;
    }

    public void userHasLogged(String user) {
        String localpart = XmppStringUtils.parseLocalpart(user);
        boolean isAnonymous = "".equals(localpart);
        String title =
                "User logged (" + connection.getConnectionCounter() + "): "
                + (isAnonymous ? "" : localpart)
                + "@"
                + connection.getServiceName()
                + ":"
                + connection.getPort();
        title += "/" + XmppStringUtils.parseResource(user);
        log(title);
        // Add the connection listener to the connection so that the debugger can be notified
        // whenever the connection is closed.
        connection.addConnectionListener(connListener);
    }

    public Reader getReader() {
        return reader;
    }

    public Writer getWriter() {
        return writer;
    }

    public StanzaListener getReaderListener() {
        return listener;
    }

    public StanzaListener getWriterListener() {
        return null;
    }
}
