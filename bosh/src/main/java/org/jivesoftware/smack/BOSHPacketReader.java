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

package org.jivesoftware.smack;

import java.io.StringReader;

import org.jivesoftware.smack.sasl.SASLMechanism.Challenge;
import org.jivesoftware.smack.sasl.SASLMechanism.SASLFailure;
import org.jivesoftware.smack.sasl.SASLMechanism.Success;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.xmlpull.v1.XmlPullParserFactory;
import org.xmlpull.v1.XmlPullParser;
import org.igniterealtime.jbosh.AbstractBody;
import org.igniterealtime.jbosh.BOSHClientResponseListener;
import org.igniterealtime.jbosh.BOSHMessageEvent;
import org.igniterealtime.jbosh.BodyQName;
import org.igniterealtime.jbosh.ComposableBody;

/**
 * Listens for XML traffic from the BOSH connection manager and parses it into
 * packet objects.
 * 
 * @author Guenther Niess
 */
public class BOSHPacketReader implements BOSHClientResponseListener {

    private BOSHConnection connection;

    /**
     * Create a packet reader which listen on a BOSHConnection for received
     * HTTP responses, parse the packets and notifies the connection.
     * 
     * @param connection the corresponding connection for the received packets.
     */
    public BOSHPacketReader(BOSHConnection connection) {
        this.connection = connection;
    }

    /**
     * Parse the received packets and notify the corresponding connection.
     * 
     * @param event the BOSH client response which includes the received packet.
     */
    public void responseReceived(BOSHMessageEvent event) {
        AbstractBody body = event.getBody();
        if (body != null) {
            try {
                if (connection.sessionID == null) {
                    connection.sessionID = body.getAttribute(BodyQName.create(BOSHConnection.BOSH_URI, "sid"));
                }
                if (connection.authID == null) {
                    connection.authID = body.getAttribute(BodyQName.create(BOSHConnection.BOSH_URI, "authid"));
                }
                final XmlPullParser parser = XmlPullParserFactory.newInstance().newPullParser();
                parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                        true);
                parser.setInput(new StringReader(body.toXML()));
                int eventType = parser.getEventType();
                do {
                    eventType = parser.next();
                    if (eventType == XmlPullParser.START_TAG) {
                        if (parser.getName().equals("body")) {
                            // ignore the container root element
                        } else if (parser.getName().equals("message")) {
                            connection.processPacket(PacketParserUtils.parseMessage(parser));
                        } else if (parser.getName().equals("iq")) {
                            connection.processPacket(PacketParserUtils.parseIQ(parser, connection));
                        } else if (parser.getName().equals("presence")) {
                            connection.processPacket(PacketParserUtils.parsePresence(parser));
                        } else if (parser.getName().equals("challenge")) {
                            // The server is challenging the SASL authentication
                            // made by the client
                            final String challengeData = parser.nextText();
                            connection.getSASLAuthentication()
                                    .challengeReceived(challengeData);
                            connection.processPacket(new Challenge(
                                    challengeData));
                        } else if (parser.getName().equals("success")) {
                            connection.send(ComposableBody.builder()
                                    .setNamespaceDefinition("xmpp", BOSHConnection.XMPP_BOSH_NS)
                                    .setAttribute(
                                            BodyQName.createWithPrefix(BOSHConnection.XMPP_BOSH_NS, "restart", "xmpp"),
                                            "true")
                                    .setAttribute(
                                            BodyQName.create(BOSHConnection.BOSH_URI, "to"),
                                            connection.getServiceName())
                                    .build());
                            connection.getSASLAuthentication().authenticated();
                            connection.processPacket(new Success(parser.nextText()));
                        } else if (parser.getName().equals("features")) {
                            parseFeatures(parser);
                        } else if (parser.getName().equals("failure")) {
                            if ("urn:ietf:params:xml:ns:xmpp-sasl".equals(parser.getNamespace(null))) {
                                final SASLFailure failure = PacketParserUtils.parseSASLFailure(parser);
                                connection.getSASLAuthentication().authenticationFailed(failure);
                                connection.processPacket(failure);
                            }
                        } else if (parser.getName().equals("error")) {
                            throw new StreamErrorException(PacketParserUtils.parseStreamError(parser));
                        }
                    }
                } while (eventType != XmlPullParser.END_DOCUMENT);
            }
            catch (Exception e) {
                if (connection.isConnected()) {
                    connection.notifyConnectionError(e);
                }
            }
        }
    }

    /**
     * Parse and setup the XML stream features.
     * 
     * @param parser the XML parser, positioned at the start of a message packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    private void parseFeatures(XmlPullParser parser) throws Exception {
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("mechanisms")) {
                    // The server is reporting available SASL mechanisms. Store
                    // this information
                    // which will be used later while logging (i.e.
                    // authenticating) into
                    // the server
                    connection.getSASLAuthentication().setAvailableSASLMethods(
                            PacketParserUtils.parseMechanisms(parser));
                } else if (parser.getName().equals("bind")) {
                    // The server requires the client to bind a resource to the
                    // stream
                    connection.getSASLAuthentication().bindingRequired();
                } else if (parser.getName().equals("session")) {
                    // The server supports sessions
                    connection.getSASLAuthentication().sessionsSupported();
                } else if (parser.getName().equals("register")) {
                    connection.getAccountManager().setSupportsAccountCreation(
                            true);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("features")) {
                    done = true;
                }
            }
        }
    }
}
