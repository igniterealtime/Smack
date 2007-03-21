/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;

import java.util.*;

/**
 * A packet representing part of a Socks5 Bytestream negotiation.
 *
 * @author Alexander Wenckus
 */
public class Bytestream extends IQ {

    private String sessionID;

    private Mode mode = Mode.tcp;

    private final List<StreamHost> streamHosts = new ArrayList<StreamHost>();

    private StreamHostUsed usedHost;

    private Activate toActivate;

    /**
     * The default constructor
     */
    public Bytestream() {
        super();
    }

    /**
     * A constructor where the session ID can be specified.
     *
     * @param SID The session ID related to the negotiation.
     * @see #setSessionID(String)
     */
    public Bytestream(final String SID) {
        super();
        setSessionID(SID);
    }

    /**
     * Set the session ID related to the Byte Stream. The session ID is a unique
     * identifier used to differentiate between stream negotations.
     *
     * @param sessionID the unique session ID that identifies the transfer.
     */
    public void setSessionID(final String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Returns the session ID related to the Byte Stream negotiation.
     *
     * @return Returns the session ID related to the Byte Stream negotiation.
     * @see #setSessionID(String)
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * Set the transport mode. This should be put in the initiation of the
     * interaction.
     *
     * @param mode the transport mode, either UDP or TCP
     * @see Mode
     */
    public void setMode(final Mode mode) {
        this.mode = mode;
    }

    /**
     * Returns the transport mode.
     *
     * @return Returns the transport mode.
     * @see #setMode(Mode)
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Adds a potential stream host that the remote user can connect to to
     * receive the file.
     *
     * @param JID     The jabber ID of the stream host.
     * @param address The internet address of the stream host.
     * @return The added stream host.
     */
    public StreamHost addStreamHost(final String JID, final String address) {
        return addStreamHost(JID, address, 0);
    }

    /**
     * Adds a potential stream host that the remote user can connect to to
     * receive the file.
     *
     * @param JID     The jabber ID of the stream host.
     * @param address The internet address of the stream host.
     * @param port    The port on which the remote host is seeking connections.
     * @return The added stream host.
     */
    public StreamHost addStreamHost(final String JID, final String address,
            final int port) {
        StreamHost host = new StreamHost(JID, address);
        host.setPort(port);
        addStreamHost(host);

        return host;
    }

    /**
     * Adds a potential stream host that the remote user can transfer the file
     * through.
     *
     * @param host The potential stream host.
     */
    public void addStreamHost(final StreamHost host) {
        streamHosts.add(host);
    }

    /**
     * Returns the list of stream hosts contained in the packet.
     *
     * @return Returns the list of stream hosts contained in the packet.
     */
    public Collection<StreamHost> getStreamHosts() {
        return Collections.unmodifiableCollection(streamHosts);
    }

    /**
     * Returns the stream host related to the given jabber ID, or null if there
     * is none.
     *
     * @param JID The jabber ID of the desired stream host.
     * @return Returns the stream host related to the given jabber ID, or null
     *         if there is none.
     */
    public StreamHost getStreamHost(final String JID) {
        if(JID == null) {
            return null;
        }
        for (StreamHost host : streamHosts) {
            if (host.getJID().equals(JID)) {
                return host;
            }
        }

        return null;
    }

    /**
     * Returns the count of stream hosts contained in this packet.
     *
     * @return Returns the count of stream hosts contained in this packet.
     */
    public int countStreamHosts() {
        return streamHosts.size();
    }

    /**
     * Upon connecting to the stream host the target of the stream replys to the
     * initiator with the jabber id of the Socks5 host that they used.
     *
     * @param JID The jabber ID of the used host.
     */
    public void setUsedHost(final String JID) {
        this.usedHost = new StreamHostUsed(JID);
    }

    /**
     * Returns the Socks5 host connected to by the remote user.
     *
     * @return Returns the Socks5 host connected to by the remote user.
     */
    public StreamHostUsed getUsedHost() {
        return usedHost;
    }

    /**
     * Returns the activate element of the packet sent to the proxy host to
     * verify the identity of the initiator and match them to the appropriate
     * stream.
     *
     * @return Returns the activate element of the packet sent to the proxy host
     *         to verify the identity of the initiator and match them to the
     *         appropriate stream.
     */
    public Activate getToActivate() {
        return toActivate;
    }

    /**
     * Upon the response from the target of the used host the activate packet is
     * sent to the Socks5 proxy. The proxy will activate the stream or return an
     * error after verifying the identity of the initiator, using the activate
     * packet.
     *
     * @param targetID The jabber ID of the target of the file transfer.
     */
    public void setToActivate(final String targetID) {
        this.toActivate = new Activate(targetID);
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<query xmlns=\"http://jabber.org/protocol/bytestreams\"");
        if (this.getType().equals(IQ.Type.SET)) {
            if (getSessionID() != null)
                buf.append(" sid=\"").append(getSessionID()).append("\"");
            if (getMode() != null)
                buf.append(" mode = \"").append(getMode()).append("\"");
            buf.append(">");
            if (getToActivate() == null) {
                for (StreamHost streamHost : getStreamHosts()) {
                    buf.append(streamHost.toXML());
                }
            }
            else {
                buf.append(getToActivate().toXML());
            }
        }
        else if (this.getType().equals(IQ.Type.RESULT)) {
            buf.append(">");
            if (getUsedHost() != null)
                buf.append(getUsedHost().toXML());
            // A result from the server can also contain stream hosts
            else if (countStreamHosts() > 0) {
                for (StreamHost host : streamHosts) {
                    buf.append(host.toXML());
                }
            }
        }
        else {
            return null;
        }
        buf.append("</query>");

        return buf.toString();
    }

    /**
     * Packet extension that represents a potential Socks5 proxy for the file
     * transfer. Stream hosts are forwared to the target of the file transfer
     * who then chooses and connects to one.
     *
     * @author Alexander Wenckus
     */
    public static class StreamHost implements PacketExtension {

        public static String NAMESPACE = "";

        public static String ELEMENTNAME = "streamhost";

        private final String JID;

        private final String addy;

        private int port = 0;

        /**
         * Default constructor.
         *
         * @param JID     The jabber ID of the stream host.
         * @param address The internet address of the stream host.
         */
        public StreamHost(final String JID, final String address) {
            this.JID = JID;
            this.addy = address;
        }

        /**
         * Returns the jabber ID of the stream host.
         *
         * @return Returns the jabber ID of the stream host.
         */
        public String getJID() {
            return JID;
        }

        /**
         * Returns the internet address of the stream host.
         *
         * @return Returns the internet address of the stream host.
         */
        public String getAddress() {
            return addy;
        }

        /**
         * Sets the port of the stream host.
         *
         * @param port The port on which the potential stream host would accept
         *             the connection.
         */
        public void setPort(final int port) {
            this.port = port;
        }

        /**
         * Returns the port on which the potential stream host would accept the
         * connection.
         *
         * @return Returns the port on which the potential stream host would
         *         accept the connection.
         */
        public int getPort() {
            return port;
        }

        public String getNamespace() {
            return NAMESPACE;
        }

        public String getElementName() {
            return ELEMENTNAME;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();

            buf.append("<").append(getElementName()).append(" ");
            buf.append("jid=\"").append(getJID()).append("\" ");
            buf.append("host=\"").append(getAddress()).append("\" ");
            if (getPort() != 0)
                buf.append("port=\"").append(getPort()).append("\"");
            else
                buf.append("zeroconf=\"_jabber.bytestreams\"");
            buf.append("/>");

            return buf.toString();
        }
    }

    /**
     * After selected a Socks5 stream host and successfully connecting, the
     * target of the file transfer returns a byte stream packet with the stream
     * host used extension.
     *
     * @author Alexander Wenckus
     */
    public static class StreamHostUsed implements PacketExtension {

        public String NAMESPACE = "";

        public static String ELEMENTNAME = "streamhost-used";

        private final String JID;

        /**
         * Default constructor.
         *
         * @param JID The jabber ID of the selected stream host.
         */
        public StreamHostUsed(final String JID) {
            this.JID = JID;
        }

        /**
         * Returns the jabber ID of the selected stream host.
         *
         * @return Returns the jabber ID of the selected stream host.
         */
        public String getJID() {
            return JID;
        }

        public String getNamespace() {
            return NAMESPACE;
        }

        public String getElementName() {
            return ELEMENTNAME;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<").append(getElementName()).append(" ");
            buf.append("jid=\"").append(getJID()).append("\" ");
            buf.append("/>");
            return buf.toString();
        }
    }

    /**
     * The packet sent by the stream initiator to the stream proxy to activate
     * the connection.
     *
     * @author Alexander Wenckus
     */
    public static class Activate implements PacketExtension {

        public String NAMESPACE = "";

        public static String ELEMENTNAME = "activate";

        private final String target;

        /**
         * Default constructor specifying the target of the stream.
         *
         * @param target The target of the stream.
         */
        public Activate(final String target) {
            this.target = target;
        }

        /**
         * Returns the target of the activation.
         *
         * @return Returns the target of the activation.
         */
        public String getTarget() {
            return target;
        }

        public String getNamespace() {
            return NAMESPACE;
        }

        public String getElementName() {
            return ELEMENTNAME;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();
            buf.append("<").append(getElementName()).append(">");
            buf.append(getTarget());
            buf.append("</").append(getElementName()).append(">");
            return buf.toString();
        }
    }

    /**
     * The stream can be either a TCP stream or a UDP stream.
     *
     * @author Alexander Wenckus
     */
    public enum Mode {

        /**
         * A TCP based stream.
         */
        tcp,

        /**
         * A UDP based stream.
         */
        udp;

        public static Mode fromName(String name) {
            Mode mode;
            try {
                mode = Mode.valueOf(name);
            }
            catch(Exception ex) {
                mode = tcp;
            }

            return mode;
        }
    }
}
