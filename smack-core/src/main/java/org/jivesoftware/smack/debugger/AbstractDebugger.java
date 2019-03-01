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

import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.ReconnectionListener;
import org.jivesoftware.smack.ReconnectionManager;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.util.ObservableReader;
import org.jivesoftware.smack.util.ObservableWriter;
import org.jivesoftware.smack.util.ReaderListener;
import org.jivesoftware.smack.util.WriterListener;

import org.jxmpp.jid.EntityFullJid;

public abstract class AbstractDebugger extends SmackDebugger {

    private static final Logger LOGGER = Logger.getLogger(AbstractDebugger.class.getName());

    public static boolean printInterpreted = false;

    private final ConnectionListener connListener;
    private final ReconnectionListener reconnectionListener;
    private final ReaderListener readerListener;
    private final WriterListener writerListener;

    private ObservableWriter writer;
    private ObservableReader reader;

    public AbstractDebugger(final XMPPConnection connection) {
        super(connection);

        // Create a special Reader that wraps the main Reader and logs data to the GUI.
        this.reader = new ObservableReader(reader);
        readerListener = new ReaderListener() {
            @Override
            public void read(String str) {
                log("RECV (" + connection.getConnectionCounter() + "): " + str);
            }
        };
        this.reader.addReaderListener(readerListener);

        // Create a special Writer that wraps the main Writer and logs data to the GUI.
        this.writer = new ObservableWriter(writer);
        writerListener = new WriterListener() {
            @Override
            public void write(String str) {
                log("SENT (" + connection.getConnectionCounter() + "): " + str);
            }
        };
        this.writer.addWriterListener(writerListener);

        connListener = new AbstractConnectionListener() {
            @Override
            public void connected(XMPPConnection connection) {
                log("XMPPConnection connected ("
                                + connection + ")");
            }
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                String logString = "XMPPConnection authenticated (" + connection + ")";
                if (resumed) {
                    logString += " and resumed";
                }
                log(logString);
            }
            @Override
            public void connectionClosed() {
                log(
                       "XMPPConnection closed (" +
                        connection +
                        ")");
            }

            @Override
            public void connectionClosedOnError(Exception e) {
                log(
                        "XMPPConnection closed due to an exception (" +
                        connection +
                        ")", e);
            }
        };

        reconnectionListener = new ReconnectionListener() {
            @Override
            public void reconnectionFailed(Exception e) {
                log(
                        "Reconnection failed due to an exception (" +
                        connection +
                        ")", e);
            }
            @Override
            public void reconnectingIn(int seconds) {
                log(
                        "XMPPConnection (" +
                        connection +
                        ") will reconnect in " + seconds);
            }
        };

        if (connection instanceof AbstractXMPPConnection) {
            AbstractXMPPConnection abstractXmppConnection = (AbstractXMPPConnection) connection;
            ReconnectionManager.getInstanceFor(abstractXmppConnection).addReconnectionListener(reconnectionListener);
        } else {
            LOGGER.info("The connection instance " + connection
                            + " is not an instance of AbstractXMPPConnection, thus we can not install the ReconnectionListener");
        }
    }

    protected abstract void log(String logMessage);

    protected abstract void log(String logMessage, Throwable throwable);

    @Override
    public final void outgoingStreamSink(CharSequence outgoingCharSequence) {
        log("SENT (" + connection.getConnectionCounter() + "): " + outgoingCharSequence);
    }

    @Override
    public final void incomingStreamSink(CharSequence incomingCharSequence) {
        log("RECV (" + connection.getConnectionCounter() + "): " + incomingCharSequence);
    }

    @Override
    public void userHasLogged(EntityFullJid user) {
        String localpart = user.getLocalpart().toString();
        boolean isAnonymous = "".equals(localpart);
        String title =
                "User logged (" + connection.getConnectionCounter() + "): "
                + (isAnonymous ? "" : localpart)
                + "@"
                + connection.getXMPPServiceDomain()
                + ":"
                + connection.getPort();
        title += "/" + user.getResourcepart();
        log(title);
        // Add the connection listener to the connection so that the debugger can be notified
        // whenever the connection is closed.
        connection.addConnectionListener(connListener);
    }

    @Override
    public void onIncomingStreamElement(TopLevelStreamElement streamElement) {
        if (printInterpreted) {
            log("RCV PKT (" + connection.getConnectionCounter() + "): " + streamElement.toXML());
        }
    }

    @Override
    public void onOutgoingStreamElement(TopLevelStreamElement streamElement) {
        // Does nothing (yet).
    }

}
