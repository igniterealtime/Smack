/**
 *
 * Copyright 2009 Jive Software.
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

package org.jivesoftware.smack.bosh;

import java.io.IOException;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.GenericConnectionException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.SmackWrappedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.igniterealtime.jbosh.AbstractBody;
import org.igniterealtime.jbosh.BOSHClient;
import org.igniterealtime.jbosh.BOSHClientConfig;
import org.igniterealtime.jbosh.BOSHClientConnEvent;
import org.igniterealtime.jbosh.BOSHClientConnListener;
import org.igniterealtime.jbosh.BOSHClientRequestListener;
import org.igniterealtime.jbosh.BOSHClientResponseListener;
import org.igniterealtime.jbosh.BOSHException;
import org.igniterealtime.jbosh.BOSHMessageEvent;
import org.igniterealtime.jbosh.BodyQName;
import org.igniterealtime.jbosh.ComposableBody;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Creates a connection to an XMPP server via HTTP binding.
 * This is specified in the XEP-0206: XMPP Over BOSH.
 *
 * @see XMPPConnection
 * @author Guenther Niess
 */
public class XMPPBOSHConnection extends AbstractXMPPConnection {
    private static final Logger LOGGER = Logger.getLogger(XMPPBOSHConnection.class.getName());

    /**
     * The XMPP Over Bosh namespace.
     */
    public static final String XMPP_BOSH_NS = "urn:xmpp:xbosh";

    /**
     * The BOSH namespace from XEP-0124.
     */
    public static final String BOSH_URI = "http://jabber.org/protocol/httpbind";

    /**
     * The used BOSH client from the jbosh library.
     */
    private BOSHClient client;

    /**
     * Holds the initial configuration used while creating the connection.
     */
    @SuppressWarnings("HidingField")
    private final BOSHConfiguration config;

    // a flag to indicate incomingStreamXmlEnvironment has been initialized
    private boolean xmlLangInit = false;

    // The readerPipe and consumer thread are used for the debugger.
    private PipedWriter readerPipe;
    private Thread readerConsumer;

    /**
     * The session ID for the BOSH session with the connection manager.
     */
    protected String sessionID = null;

    /**
     * Create a HTTP Binding connection to an XMPP server.
     *
     * @param username the username to use.
     * @param password the password to use.
     * @param https true if you want to use SSL
     *             (e.g. false for http://domain.lt:7070/http-bind).
     * @param host the hostname or IP address of the connection manager
     *             (e.g. domain.lt for http://domain.lt:7070/http-bind).
     * @param port the port of the connection manager
     *             (e.g. 7070 for http://domain.lt:7070/http-bind).
     * @param filePath the file which is described by the URL
     *             (e.g. /http-bind for http://domain.lt:7070/http-bind).
     * @param xmppServiceDomain the XMPP service name
     *             (e.g. domain.lt for the user alice@domain.lt)
     */
    public XMPPBOSHConnection(String username, String password, boolean https, String host, int port, String filePath, DomainBareJid xmppServiceDomain) {
        this(BOSHConfiguration.builder().setUseHttps(https).setHost(host)
                .setPort(port).setFile(filePath).setXmppDomain(xmppServiceDomain)
                .setUsernameAndPassword(username, password).build());
    }

    /**
     * Create a HTTP Binding connection to an XMPP server.
     *
     * @param config The configuration which is used for this connection.
     */
    public XMPPBOSHConnection(BOSHConfiguration config) {
        super(config);
        this.config = config;
    }

    @SuppressWarnings("deprecation")
    @Override
    protected void connectInternal() throws SmackException, InterruptedException, XMPPException {
        try {
            // Ensure a clean starting state
            if (client != null) {
                client.close();
                client = null;
            }
            sessionID = null;

            // Initialize BOSH client
            BOSHClientConfig.Builder cfgBuilder = BOSHClientConfig.Builder
                    .create(config.getURI(), config.getXMPPServiceDomain().toString());
            if (config.isProxyEnabled()) {
                cfgBuilder.setProxy(config.getProxyAddress(), config.getProxyPort());
            }

            cfgBuilder.setCompressionEnabled(config.isCompressionEnabled());

            for (Map.Entry<String, String> h : config.getHttpHeaders().entrySet()) {
                cfgBuilder.addHttpHeader(h.getKey(), h.getValue());
            }

            client = BOSHClient.create(cfgBuilder.build());

            // Initialize the debugger before addBOSHClientResponseListener(new BOSHPacketReader());
            // BOSHPacketReader may hold and send response prior to display of the request i.e. <response/> before <challenge/>
            if (debugger != null) {
                initDebugger();
            }

            client.addBOSHClientConnListener(new BOSHConnectionListener());
            client.addBOSHClientResponseListener(new BOSHPacketReader());

            // Send the session creation request
            client.send(ComposableBody.builder()
                    .setNamespaceDefinition("xmpp", XMPP_BOSH_NS)
                    .setAttribute(BodyQName.createWithPrefix(XMPP_BOSH_NS, "version", "xmpp"), "1.0")
                    .build());
        } catch (Exception e) {
            throw new GenericConnectionException(e);
        }

        // Wait with SASL auth until the SASL mechanisms have been received
        waitForConditionOrThrowConnectionException(() -> saslFeatureReceived, "SASL mechanisms stream feature from server");
    }

    @Override
    public boolean isSecureConnection() {
        // TODO: Implement SSL usage
        return false;
    }

    @Override
    public boolean isUsingCompression() {
        // TODO: Implement compression
        return false;
    }

    @Override
    protected void loginInternal(String username, String password, Resourcepart resource) throws XMPPException,
                    SmackException, IOException, InterruptedException {
        // Authenticate using SASL
        authenticate(username, password, config.getAuthzid(), null);

        bindResourceAndEstablishSession(resource);

        afterSuccessfulLogin(false);
    }

    @Override
    public void sendNonza(Nonza element) throws NotConnectedException {
        sendElement(element);
    }

    @Override
    protected void sendStanzaInternal(Stanza stanza) throws NotConnectedException {
        sendElement(stanza);
    }

    private void sendElement(Element element) throws NotConnectedException {
        try {
            send(ComposableBody.builder().setPayloadXML(element.toXML(BOSH_URI).toString()).build());
            if (element instanceof Stanza) {
                firePacketSendingListeners((Stanza) element);
            }
        }
        catch (BOSHException e) {
            LOGGER.log(Level.SEVERE, "BOSHException in sendStanzaInternal", e);
        }
    }

    /**
     * Closes the connection by setting presence to unavailable and closing the
     * HTTP client. The shutdown logic will be used during a planned disconnection or when
     * dealing with an unexpected disconnection. Unlike {@link #disconnect()} the connection's
     * BOSH stanza reader will not be removed; thus connection's state is kept.
     *
     */
    @Override
    protected void shutdown() {

        if (client != null) {
            try {
                client.disconnect();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "shutdown", e);
            }
            client = null;
        }

        instantShutdown();
    }

    @Override
    public void instantShutdown() {
        setWasAuthenticated();
        sessionID = null;
        authenticated = false;
        connected = false;

        // Close down the readers and writers.
        CloseableUtil.maybeClose(readerPipe, LOGGER);
        CloseableUtil.maybeClose(reader, LOGGER);
        CloseableUtil.maybeClose(writer, LOGGER);

        // set readerConsumer = null before reader
        readerConsumer = null;
        reader = null;
        readerPipe = null;
        writer = null;
    }

    /**
     * Send a HTTP request to the connection manager with the provided body element.
     *
     * @param body the body which will be sent.
     * @throws BOSHException if an BOSH (Bidirectional-streams Over Synchronous HTTP, XEP-0124) related error occurs
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws IllegalArgumentException if the body is null or empty.
     */
    protected void send(ComposableBody body) throws BOSHException, NotConnectedException, IllegalArgumentException {

        Objects.requireNonNull(body, "Body must not be null");
        if (!connected) {
            throw new NotConnectedException("Not connected to a server!");
        }
        if (sessionID != null) {
            body = body.rebuild().setAttribute(
                    BodyQName.create(BOSH_URI, "sid"), sessionID).build();
        }
        client.send(body);
    }

    /**
     * Initialize the SmackDebugger which allows to log and debug XML traffic.
     */
    @Override
    protected void initDebugger() {
        // TODO: Maybe we want to extend the SmackDebugger for simplification
        //       and a performance boost.

        // Initialize a empty writer which discards all data.
        writer = new Writer() {
            @Override
            public void write(char[] cbuf, int off, int len) {
                /* ignore */ }

            @Override
            public void close() {
                /* ignore */ }

            @Override
            public void flush() {
                /* ignore */ }
        };

        // Initialize a pipe for received raw data.
        try {
            readerPipe = new PipedWriter();
            reader = new PipedReader(readerPipe);
        }
        catch (IOException e) {
            // Ignore
        }

        // Call the method from the parent class which initializes the debugger.
        super.initDebugger();

        // Add listeners for the received and sent raw data.
        client.addBOSHClientResponseListener(new BOSHClientResponseListener() {
            @Override
            public void responseReceived(BOSHMessageEvent event) {
                if (event.getBody() != null) {
                    try {
                        readerPipe.write(event.getBody().toXML());
                        readerPipe.flush();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        });
        client.addBOSHClientRequestListener(new BOSHClientRequestListener() {
            @Override
            public void requestSent(BOSHMessageEvent event) {
                if (event.getBody() != null) {
                    try {
                        writer.write(event.getBody().toXML());
                        writer.flush();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        });

        // Create and start a thread which discards all read data.
        readerConsumer = new Thread() {
            private Thread thread = this;
            private int bufferLength = 1024;

            @Override
            public void run() {
                try {
                    char[] cbuf = new char[bufferLength];
                    while (readerConsumer == thread) {
                        reader.read(cbuf, 0, bufferLength);
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        };
        readerConsumer.setDaemon(true);
        readerConsumer.start();
    }

    @Override
    protected void afterSaslAuthenticationSuccess()
                    throws NotConnectedException, InterruptedException, SmackWrappedException {
        // XMPP over BOSH is unusual when it comes to SASL authentication: Instead of sending a new stream open, it
        // requires a special XML element ot be send after successful SASL authentication.
        // See XEP-0206 § 5., especially the following is example 8 of XEP-0206.
        ComposableBody composeableBody = ComposableBody.builder()
                .setNamespaceDefinition("xmpp", XMPP_BOSH_NS)
                .setAttribute(BodyQName.createWithPrefix(XMPP_BOSH_NS, "lang", "xmpp"), "en")
                .setAttribute(BodyQName.createWithPrefix(XMPP_BOSH_NS, "restart", "xmpp"), "true")
                .setAttribute(BodyQName.create(BOSH_URI, "to"), getXMPPServiceDomain().toString())
                .build();

        try {
            send(composeableBody);
        } catch (BOSHException e) {
            // jbosh's exception API does not really match the one of Smack.
            throw new SmackException.SmackWrappedException(e);
        }
    }

    /**
     * A listener class which listen for BOSH connection errors and notifies the XMPPConnection.
     * All other exceptions are handled by AbstractXMPPConnection class
     */
    private class BOSHConnectionListener implements BOSHClientConnListener {
        @Override
        public void connectionEvent(BOSHClientConnEvent connEvent) {
            if (connEvent.isError() && isConnected()) {
                // stanza sending disrupted gets <h1>400 Bad Request</h1>
                Throwable cause = connEvent.getCause();
                Exception e;
                if (cause instanceof Exception) {
                    e = (Exception) cause;
                } else {
                    e = new Exception(cause);
                }
                notifyConnectionError(e);
            }
        }
    }

    /**
     * Listens for XML traffic from the BOSH connection manager and parses it into
     * stanza objects.
     *
     * @author Guenther Niess
     */
    private class BOSHPacketReader implements BOSHClientResponseListener {

        /**
         * Parse the received packets and notify the corresponding connection.
         *
         * @param event the BOSH client response which includes the received packet.
         */
        @Override
        public void responseReceived(BOSHMessageEvent event) {
            AbstractBody body = event.getBody();
            if (body != null) {
                try {
                    if (sessionID == null) {
                        sessionID = body.getAttribute(BodyQName.create(XMPPBOSHConnection.BOSH_URI, "sid"));
                    }
                    if (streamId == null) {
                        streamId = body.getAttribute(BodyQName.create(XMPPBOSHConnection.BOSH_URI, "authid"));
                    }
                    final XmlPullParser parser = PacketParserUtils.getParserFor(body.toXML());

                    XmlPullParser.Event eventType;
                    do {
                        eventType = parser.next();
                        switch (eventType) {
                        case START_ELEMENT:
                            String name = parser.getName();
                            switch (name) {
                            case IQ.IQ_ELEMENT:
                                // cmeng: a temporary patch to init incomingStreamXmlEnvironment for BOSHConnection
                                // BOSHConnection does not have a stream:stream Nonza.
                                if (!xmlLangInit && body.toXML().contains(RosterPacket.NAMESPACE)) {
                                    onStreamOpen(parser);
                                    xmlLangInit = true;
                                }
                            case Message.ELEMENT:
                            case Presence.ELEMENT:
                                parseAndProcessStanza(parser);
                                break;
                            case "features":
                                parseFeaturesAndNotify(parser);
                                break;
                            case "error":
                                // Some BOSH error isn't stream error. parser.getNamespace(null) <= http://jabber.org/protocol/httpbind
                                StreamError streamError = PacketParserUtils.parseStreamError(parser);
                                // saslFeatureReceived.reportFailure(new StreamErrorException(streamError));
                                // throwCurrentConnectionException();

                                if (StreamError.NAMESPACE.equals(streamError.getNamespace())) {
                                    throw new StreamErrorException(streamError);
                                }
                                else {
                                    StanzaError stanzaError = PacketParserUtils.parseError(parser);
                                    throw new XMPPException.XMPPErrorException(null, stanzaError);
                                }
                            default:
                                parseAndProcessNonza(parser);
                                break;
                            }
                            break;
                        default:
                            // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                            break;
                        }
                    }
                    while (eventType != XmlPullParser.Event.END_DOCUMENT);
                }
                catch (Exception e) {
                    if (isConnected()) {
                        notifyConnectionError(e);
                    }
                }
            }
        }
    }
}
