/**
 *
 * Copyright 2003-2005 Jive Software.
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
package org.jivesoftware.smackx.jingleold.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.jingleold.nat.ICECandidate;
import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;
import org.jivesoftware.smackx.jingleold.packet.JingleTransport;
import org.jivesoftware.smackx.jingleold.packet.JingleTransport.JingleTransportCandidate;

/**
 * Provider for a Jingle transport element.
 *
 * @author Alvaro Saurin
 */
public abstract class JingleTransportProvider extends ExtensionElementProvider<JingleTransport> {

    /**
     * Obtain the corresponding TransportNegotiator instance.
     *
     * @return a new TransportNegotiator instance
     */
    protected JingleTransport getInstance() {
        return new JingleTransport();
    }

    /**
     * Parse a iq/jingle/transport element.
     *
     * @param parser the structure to parse
     * @return a transport element.
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     */
    @Override
    public JingleTransport parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException  {
        JingleTransport trans = getInstance();

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();

            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                String name = parser.getName();
                if (name.equals(JingleTransportCandidate.NODENAME)) {
                    JingleTransportCandidate jtc = parseCandidate(parser);
                    if (jtc != null) trans.addCandidate(jtc);
                }
                else {
                    // TODO: Should be SmackParseException.
                    throw new IOException("Unknown tag \"" + name + "\" in transport element.");
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return trans;
    }

    protected abstract JingleTransportCandidate parseCandidate(XmlPullParser parser);

    /**
     * RTP-ICE profile.
     */
    public static class Ice extends JingleTransportProvider {

        /**
         * Defauls constructor.
         */
        public Ice() {
            super();
        }

        /**
         * Obtain the corresponding TransportNegotiator.Ice instance.
         *
         * @return a new TransportNegotiator.Ice instance
         */
        @Override
        protected JingleTransport getInstance() {
            return new JingleTransport.Ice();
        }

        /**
         * Parse a iq/jingle/transport/candidate element.
         *
         * @param parser the structure to parse
         * @return a candidate element
         */
        @Override
        protected JingleTransportCandidate parseCandidate(XmlPullParser parser) {
            ICECandidate mt = new ICECandidate();

            String channel = parser.getAttributeValue("", "channel");
            String generation = parser.getAttributeValue("", "generation");
            String ip = parser.getAttributeValue("", "ip");
            String name = parser.getAttributeValue("", "name");
            String network = parser.getAttributeValue("", "network");
            String username = parser.getAttributeValue("", "username");
            String password = parser.getAttributeValue("", "password");
            String port = parser.getAttributeValue("", "port");
            String preference = parser.getAttributeValue("", "preference");
            String proto = parser.getAttributeValue("", "proto");
            String type = parser.getAttributeValue("", "type");

            if (channel != null) {
                mt.setChannel(new TransportCandidate.Channel(channel));
            }

            if (generation != null) {
                mt.setGeneration(Integer.parseInt(generation));
            }

            if (ip != null) {
                mt.setIp(ip);
            }
            else {
                return null;
            }

            if (name != null) {
                mt.setName(name);
            }

            if (network != null) {
                mt.setNetwork(Integer.parseInt(network));
            }

            if (username != null) {
                mt.setUsername(username);
            }

            if (password != null) {
                mt.setPassword(password);
            }

            if (port != null) {
                mt.setPort(Integer.parseInt(port));
            }

            if (preference != null) {
                mt.setPreference(Integer.parseInt(preference));
            }

            if (proto != null) {
                mt.setProto(new TransportCandidate.Protocol(proto));
            }

            if (type != null) {
                mt.setType(ICECandidate.Type.valueOf(type));
            }

            return new JingleTransport.Ice.Candidate(mt);
        }
    }

    /**
     * Raw UDP profile.
     */
    public static class RawUdp extends JingleTransportProvider {

        /**
         * Defauls constructor.
         */
        public RawUdp() {
            super();
        }

        /**
         * Obtain the corresponding TransportNegotiator.RawUdp instance.
         *
         * @return a new TransportNegotiator.RawUdp instance
         */
        @Override
        protected JingleTransport getInstance() {
            return new JingleTransport.RawUdp();
        }

        /**
         * Parse a iq/jingle/transport/candidate element.
         *
         * @param parser the structure to parse
         * @return a candidate element
         */
        @Override
        protected JingleTransportCandidate parseCandidate(XmlPullParser parser) {
            TransportCandidate.Fixed mt = new TransportCandidate.Fixed();

            String generation = parser.getAttributeValue("", "generation");
            String ip = parser.getAttributeValue("", "ip");
            String name = parser.getAttributeValue("", "name");
            String port = parser.getAttributeValue("", "port");

            // LOGGER.debug();

            if (generation != null) {
                mt.setGeneration(Integer.parseInt(generation));
            }

            if (ip != null) {
                mt.setIp(ip);
            }

            if (name != null) {
                mt.setName(name);
            }

            if (port != null) {
                mt.setPort(Integer.parseInt(port));
            }
            return new JingleTransport.RawUdp.Candidate(mt);
        }
    }
}
