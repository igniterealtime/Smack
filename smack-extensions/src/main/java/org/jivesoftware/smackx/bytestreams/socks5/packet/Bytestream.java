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
package org.jivesoftware.smackx.bytestreams.socks5.packet;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.jid.Jid;

/**
 * A stanza(/packet) representing part of a SOCKS5 Bytestream negotiation.
 * 
 * @author Alexander Wenckus
 */
public class Bytestream extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;

    /**
     * The XMPP namespace of the SOCKS5 Bytestream.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/bytestreams";

    private String sessionID;

    private Mode mode = Mode.tcp;

    private final List<StreamHost> streamHosts = new ArrayList<StreamHost>();

    private StreamHostUsed usedHost;

    private Activate toActivate;

    /**
     * The default constructor.
     */
    public Bytestream() {
        super(ELEMENT, NAMESPACE);
    }

    /**
     * A constructor where the session ID can be specified.
     * 
     * @param SID The session ID related to the negotiation.
     * @see #setSessionID(String)
     */
    public Bytestream(final String SID) {
        this();
        setSessionID(SID);
    }

    /**
     * Set the session ID related to the bytestream. The session ID is a unique identifier used to
     * differentiate between stream negotiations.
     * 
     * @param sessionID the unique session ID that identifies the transfer.
     */
    public void setSessionID(final String sessionID) {
        this.sessionID = sessionID;
    }

    /**
     * Returns the session ID related to the bytestream negotiation.
     * 
     * @return Returns the session ID related to the bytestream negotiation.
     * @see #setSessionID(String)
     */
    public String getSessionID() {
        return sessionID;
    }

    /**
     * Set the transport mode. This should be put in the initiation of the interaction.
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
     * Adds a potential stream host that the remote user can connect to to receive the file.
     * 
     * @param JID The JID of the stream host.
     * @param address The internet address of the stream host.
     * @return The added stream host.
     */
    public StreamHost addStreamHost(final Jid JID, final String address) {
        return addStreamHost(JID, address, 0);
    }

    /**
     * Adds a potential stream host that the remote user can connect to to receive the file.
     * 
     * @param JID The JID of the stream host.
     * @param address The internet address of the stream host.
     * @param port The port on which the remote host is seeking connections.
     * @return The added stream host.
     */
    public StreamHost addStreamHost(final Jid JID, final String address, final int port) {
        StreamHost host = new StreamHost(JID, address, port);
        addStreamHost(host);

        return host;
    }

    /**
     * Adds a potential stream host that the remote user can transfer the file through.
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
    public List<StreamHost> getStreamHosts() {
        return Collections.unmodifiableList(streamHosts);
    }

    /**
     * Returns the stream host related to the given JID, or null if there is none.
     * 
     * @param JID The JID of the desired stream host.
     * @return Returns the stream host related to the given JID, or null if there is none.
     */
    public StreamHost getStreamHost(final Jid JID) {
        if (JID == null) {
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
     * Upon connecting to the stream host the target of the stream replies to the initiator with the
     * JID of the SOCKS5 host that they used.
     * 
     * @param JID The JID of the used host.
     */
    public void setUsedHost(final Jid JID) {
        this.usedHost = new StreamHostUsed(JID);
    }

    /**
     * Returns the SOCKS5 host connected to by the remote user.
     * 
     * @return Returns the SOCKS5 host connected to by the remote user.
     */
    public StreamHostUsed getUsedHost() {
        return usedHost;
    }

    /**
     * Returns the activate element of the stanza(/packet) sent to the proxy host to verify the identity of
     * the initiator and match them to the appropriate stream.
     * 
     * @return Returns the activate element of the stanza(/packet) sent to the proxy host to verify the
     *         identity of the initiator and match them to the appropriate stream.
     */
    public Activate getToActivate() {
        return toActivate;
    }

    /**
     * Upon the response from the target of the used host the activate stanza(/packet) is sent to the SOCKS5
     * proxy. The proxy will activate the stream or return an error after verifying the identity of
     * the initiator, using the activate packet.
     * 
     * @param targetID The JID of the target of the file transfer.
     */
    public void setToActivate(final Jid targetID) {
        this.toActivate = new Activate(targetID);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        switch(getType()) {
        case set:
            xml.optAttribute("sid", getSessionID());
            xml.optAttribute("mode", getMode());
            xml.rightAngleBracket();
            if (getToActivate() == null) {
                for (StreamHost streamHost : getStreamHosts()) {
                    xml.append(streamHost.toXML());
                }
            }
            else {
                xml.append(getToActivate().toXML());
            }
            break;
        case result:
            xml.rightAngleBracket();
            xml.optAppend(getUsedHost());
            // TODO Bytestream can include either used host *or* streamHosts. Never both. This should be ensured by the
            // constructions mechanisms of Bytestream
            // A result from the server can also contain stream hosts
            for (StreamHost host : streamHosts) {
                xml.append(host.toXML());
            }
            break;
        case get:
            xml.setEmptyElement();
            break;
        default:
            throw new IllegalStateException();
        }

        return xml;
    }

    /**
     * Stanza(/Packet) extension that represents a potential SOCKS5 proxy for the file transfer. Stream hosts
     * are forwarded to the target of the file transfer who then chooses and connects to one.
     * 
     * @author Alexander Wenckus
     */
    public static class StreamHost implements NamedElement {

        public static String ELEMENTNAME = "streamhost";

        private final Jid JID;

        private final String addy;

        private final int port;

        public StreamHost(Jid jid, String address) {
            this(jid, address, 0);
        }

        /**
         * Default constructor.
         * 
         * @param JID The JID of the stream host.
         * @param address The internet address of the stream host.
         */
        public StreamHost(final Jid JID, final String address, int port) {
            this.JID = Objects.requireNonNull(JID, "StreamHost JID must not be null");
            this.addy = StringUtils.requireNotNullOrEmpty(address, "StreamHost address must not be null");
            this.port = port;
        }

        /**
         * Returns the JID of the stream host.
         * 
         * @return Returns the JID of the stream host.
         */
        public Jid getJID() {
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
         * Returns the port on which the potential stream host would accept the connection.
         * 
         * @return Returns the port on which the potential stream host would accept the connection.
         */
        public int getPort() {
            return port;
        }

        @Override
        public String getElementName() {
            return ELEMENTNAME;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("jid", getJID());
            xml.attribute("host", getAddress());
            if (getPort() != 0) {
                xml.attribute("port", Integer.toString(getPort()));
            } else {
                xml.attribute("zeroconf", "_jabber.bytestreams");
            }
            xml.closeEmptyElement();
            return xml;
        }
    }

    /**
     * After selected a SOCKS5 stream host and successfully connecting, the target of the file
     * transfer returns a byte stream stanza(/packet) with the stream host used extension.
     * 
     * @author Alexander Wenckus
     */
    public static class StreamHostUsed implements NamedElement {

        public static String ELEMENTNAME = "streamhost-used";

        private final Jid JID;

        /**
         * Default constructor.
         * 
         * @param JID The JID of the selected stream host.
         */
        public StreamHostUsed(final Jid JID) {
            this.JID = JID;
        }

        /**
         * Returns the JID of the selected stream host.
         * 
         * @return Returns the JID of the selected stream host.
         */
        public Jid getJID() {
            return JID;
        }

        @Override
        public String getElementName() {
            return ELEMENTNAME;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("jid", getJID());
            xml.closeEmptyElement();
            return xml;
        }
    }

    /**
     * The stanza(/packet) sent by the stream initiator to the stream proxy to activate the connection.
     * 
     * @author Alexander Wenckus
     */
    public static class Activate implements NamedElement {

        public static String ELEMENTNAME = "activate";

        private final Jid target;

        /**
         * Default constructor specifying the target of the stream.
         * 
         * @param target The target of the stream.
         */
        public Activate(final Jid target) {
            this.target = target;
        }

        /**
         * Returns the target of the activation.
         * 
         * @return Returns the target of the activation.
         */
        public Jid getTarget() {
            return target;
        }

        @Override
        public String getElementName() {
            return ELEMENTNAME;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();
            xml.escape(getTarget());
            xml.closeElement(this);
            return xml;
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
            catch (Exception ex) {
                mode = tcp;
            }

            return mode;
        }
    }
}
