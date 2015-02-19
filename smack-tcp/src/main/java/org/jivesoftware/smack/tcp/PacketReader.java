/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.tcp;

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.parsing.UnparsablePacket;
import org.jivesoftware.smack.sasl.SASLMechanism.Challenge;
import org.jivesoftware.smack.sasl.SASLMechanism.SASLFailure;
import org.jivesoftware.smack.sasl.SASLMechanism.Success;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.SecurityRequiredException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Listens for XML traffic from the XMPP server and parses it into packet objects.
 * The packet reader also invokes all packet listeners and collectors.<p>
 *
 * @see XMPPConnection#createPacketCollector
 * @see XMPPConnection#addPacketListener
 * @author Matt Tucker
 */
class PacketReader {

    private Thread readerThread;

    private XMPPTCPConnection connection;
    private XmlPullParser parser;

    /**
     * Set to true if the last features stanza from the server has been parsed. A XMPP connection
     * handshake can invoke multiple features stanzas, e.g. when TLS is activated a second feature
     * stanza is send by the server. This is set to true once the last feature stanza has been
     * parsed.
     */
    private volatile boolean lastFeaturesParsed;

    volatile boolean done;

    protected PacketReader(final XMPPTCPConnection connection) throws SmackException {
        this.connection = connection;
        this.init();
    }

    /**
     * Initializes the reader in order to be used. The reader is initialized during the
     * first connection and when reconnecting due to an abruptly disconnection.
     *
     * @throws SmackException if the parser could not be reset.
     */
    protected void init() throws SmackException {
        done = false;
        lastFeaturesParsed = false;

        readerThread = new Thread() {
            public void run() {
                parsePackets(this);
            }
        };
        readerThread.setName("Smack Packet Reader (" + connection.getConnectionCounter() + ")");
        readerThread.setDaemon(true);

        resetParser();
    }

    /**
     * Starts the packet reader thread and returns once a connection to the server
     * has been established or if the server's features could not be parsed within
     * the connection's PacketReplyTimeout.
     *
     * @throws NoResponseException if the server fails to send an opening stream back
     *      within packetReplyTimeout.
     * @throws IOException 
     */
    synchronized public void startup() throws NoResponseException, IOException {
        readerThread.start();

        try {
            // Wait until either:
            // - the servers last features stanza has been parsed
            // - an exception is thrown while parsing
            // - the timeout occurs
            wait(connection.getPacketReplyTimeout());
        }
        catch (InterruptedException ie) {
            // Ignore.
        }
        if (!lastFeaturesParsed) {
            connection.throwConnectionExceptionOrNoResponse();
        }
    }

    /**
     * Shuts the packet reader down. This method simply sets the 'done' flag to true.
     */
    public void shutdown() {
        done = true;
    }

    /**
     * Resets the parser using the latest connection's reader. Reseting the parser is necessary
     * when the plain connection has been secured or when a new opening stream element is going
     * to be sent by the server.
     *
     * @throws SmackException if the parser could not be reset.
     */
    private void resetParser() throws SmackException {
        try {
            parser = PacketParserUtils.newXmppParser();
            parser.setInput(connection.getReader());
        }
        catch (XmlPullParserException e) {
            throw new SmackException(e);
        }
    }

    /**
     * Parse top-level packets in order to process them further.
     *
     * @param thread the thread that is being used by the reader to parse incoming packets.
     */
    private void parsePackets(Thread thread) {
        try {
            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {
                    int parserDepth = parser.getDepth();
                    ParsingExceptionCallback callback = connection.getParsingExceptionCallback();
                    if (parser.getName().equals("message")) {
                        Packet packet;
                        try {
                            packet = PacketParserUtils.parseMessage(parser);
                        } catch (Exception e) {
                            String content = PacketParserUtils.parseContentDepth(parser, parserDepth);
                            UnparsablePacket message = new UnparsablePacket(content, e);
                            if (callback != null) {
                                callback.handleUnparsablePacket(message);
                            }
                            // The parser is now at the end tag of the unparsable stanza. We need to advance to the next
                            // start tag in order to avoid an exception which would again lead to the execution of the
                            // catch block becoming effectively an endless loop.
                            eventType = parser.next();
                            continue;
                        }
                        connection.processPacket(packet);
                    }
                    else if (parser.getName().equals("iq")) {
                        IQ iq;
                        try {
                            iq = PacketParserUtils.parseIQ(parser, connection);
                        } catch (Exception e) {
                            String content = PacketParserUtils.parseContentDepth(parser, parserDepth);
                            UnparsablePacket message = new UnparsablePacket(content, e);
                            if (callback != null) {
                                callback.handleUnparsablePacket(message);
                            }
                            // The parser is now at the end tag of the unparsable stanza. We need to advance to the next
                            // start tag in order to avoid an exception which would again lead to the execution of the
                            // catch block becoming effectively an endless loop.
                            eventType = parser.next();
                            continue;
                        }
                        connection.processPacket(iq);
                    }
                    else if (parser.getName().equals("presence")) {
                        Presence presence;
                        try {
                            presence = PacketParserUtils.parsePresence(parser);
                        } catch (Exception e) {
                            String content = PacketParserUtils.parseContentDepth(parser, parserDepth);
                            UnparsablePacket message = new UnparsablePacket(content, e);
                            if (callback != null) {
                                callback.handleUnparsablePacket(message);
                            }
                            // The parser is now at the end tag of the unparsable stanza. We need to advance to the next
                            // start tag in order to avoid an exception which would again lead to the execution of the
                            // catch block becoming effectively an endless loop.
                            eventType = parser.next();
                            continue;
                        }
                        connection.processPacket(presence);
                    }
                    // We found an opening stream. Record information about it, then notify
                    // the connectionID lock so that the packet reader startup can finish.
                    else if (parser.getName().equals("stream")) {
                        // Ensure the correct jabber:client namespace is being used.
                        if ("jabber:client".equals(parser.getNamespace(null))) {
                            // Get the connection id.
                            for (int i=0; i<parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals("id")) {
                                    // Save the connectionID
                                    connection.connectionID = parser.getAttributeValue(i);
                                }
                            }
                        }
                    }
                    else if (parser.getName().equals("error")) {
                        throw new StreamErrorException(PacketParserUtils.parseStreamError(parser));
                    }
                    else if (parser.getName().equals("features")) {
                        parseFeatures(parser);
                    }
                    else if (parser.getName().equals("proceed")) {
                        // Secure the connection by negotiating TLS
                        connection.proceedTLSReceived();
                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                        resetParser();
                    }
                    else if (parser.getName().equals("failure")) {
                        String namespace = parser.getNamespace(null);
                        if ("urn:ietf:params:xml:ns:xmpp-tls".equals(namespace)) {
                            // TLS negotiation has failed. The server will close the connection
                            throw new Exception("TLS negotiation has failed");
                        }
                        else if ("http://jabber.org/protocol/compress".equals(namespace)) {
                            // Stream compression has been denied. This is a recoverable
                            // situation. It is still possible to authenticate and
                            // use the connection but using an uncompressed connection
                            connection.streamCompressionNegotiationDone();
                        }
                        else {
                            // SASL authentication has failed. The server may close the connection
                            // depending on the number of retries
                            final SASLFailure failure = PacketParserUtils.parseSASLFailure(parser);
                            connection.processPacket(failure);
                            connection.getSASLAuthentication().authenticationFailed(failure);
                        }
                    }
                    else if (parser.getName().equals("challenge")) {
                        // The server is challenging the SASL authentication made by the client
                        String challengeData = parser.nextText();
                        connection.processPacket(new Challenge(challengeData));
                        connection.getSASLAuthentication().challengeReceived(challengeData);
                    }
                    else if (parser.getName().equals("success")) {
                        connection.processPacket(new Success(parser.nextText()));
                        // We now need to bind a resource for the connection
                        // Open a new stream and wait for the response
                        connection.packetWriter.openStream();
                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                        resetParser();
                        // The SASL authentication with the server was successful. The next step
                        // will be to bind the resource
                        connection.getSASLAuthentication().authenticated();
                    }
                    else if (parser.getName().equals("compressed")) {
                        // Server confirmed that it's possible to use stream compression. Start
                        // stream compression
                        connection.startStreamCompression();
                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                        resetParser();
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("stream")) {
                        // Disconnect the connection
                        connection.disconnect();
                    }
                }
                eventType = parser.next();
            } while (!done && eventType != XmlPullParser.END_DOCUMENT && thread == readerThread);
        }
        catch (Exception e) {
            // The exception can be ignored if the the connection is 'done'
            // or if the it was caused because the socket got closed
            if (!(done || connection.isSocketClosed())) {
                synchronized(this) {
                    this.notify();
                }
                // Close the connection and notify connection listeners of the
                // error.
                connection.notifyConnectionError(e);
            }
        }
    }

    private void parseFeatures(XmlPullParser parser) throws Exception {
        boolean startTLSReceived = false;
        boolean startTLSRequired = false;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("starttls")) {
                    startTLSReceived = true;
                }
                else if (parser.getName().equals("mechanisms")) {
                    // The server is reporting available SASL mechanisms. Store this information
                    // which will be used later while logging (i.e. authenticating) into
                    // the server
                    connection.getSASLAuthentication()
                            .setAvailableSASLMethods(PacketParserUtils.parseMechanisms(parser));
                }
                else if (parser.getName().equals("bind")) {
                    // The server requires the client to bind a resource to the stream
                    connection.serverRequiresBinding();
                }
                // Set the entity caps node for the server if one is send
                // See http://xmpp.org/extensions/xep-0115.html#stream
                else if (parser.getName().equals("c")) {
                    String node = parser.getAttributeValue(null, "node");
                    String ver = parser.getAttributeValue(null, "ver");
                    if (ver != null && node != null) {
                        String capsNode = node + "#" + ver;
                        // In order to avoid a dependency from smack to smackx
                        // we have to set the services caps node in the connection
                        // and not directly in the EntityCapsManager
                        connection.setServiceCapsNode(capsNode);
                    }
                }
                else if (parser.getName().equals("session")) {
                    // The server supports sessions
                    connection.serverSupportsSession();
                }
                else if (parser.getName().equals("ver")) {
                    if (parser.getNamespace().equals("urn:xmpp:features:rosterver")) {
                        connection.setRosterVersioningSupported();
                    }
                }
                else if (parser.getName().equals("compression")) {
                    // The server supports stream compression
                    connection.setAvailableCompressionMethods(PacketParserUtils.parseCompressionMethods(parser));
                }
                else if (parser.getName().equals("register")) {
                    connection.serverSupportsAccountCreation();
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("starttls")) {
                    // Confirm the server that we want to use TLS
                    connection.startTLSReceived(startTLSRequired);
                }
                else if (parser.getName().equals("required") && startTLSReceived) {
                    startTLSRequired = true;
                }
                else if (parser.getName().equals("features")) {
                    done = true;
                }
            }
        }

        // If TLS is required but the server doesn't offer it, disconnect
        // from the server and throw an error. First check if we've already negotiated TLS
        // and are secure, however (features get parsed a second time after TLS is established).
        if (!connection.isSecureConnection()) {
            if (!startTLSReceived && connection.getConfiguration().getSecurityMode() ==
                    ConnectionConfiguration.SecurityMode.required)
            {
                throw new SecurityRequiredException();
            }
        }

        // Release the lock after TLS has been negotiated or we are not interested in TLS. If the
        // server announced TLS and we choose to use it, by sending 'starttls', which the server
        // replied with 'proceed', the server is required to send a new stream features element that
        // "MUST NOT include the STARTTLS feature" (RFC6120 5.4.3.3. 5.). We are therefore save to
        // release the connection lock once either TLS is disabled or we received a features stanza
        // without starttls.
        if (!startTLSReceived || connection.getConfiguration().getSecurityMode() ==
                ConnectionConfiguration.SecurityMode.disabled)
        {
            lastFeaturesParsed = true;
            // This synchronized block prevents this thread from calling notify() before the other
            // thread had called wait() (it would cause an Exception if wait() hadn't been called)
            synchronized (this) {
                notify();
            }
        }
    }
}
