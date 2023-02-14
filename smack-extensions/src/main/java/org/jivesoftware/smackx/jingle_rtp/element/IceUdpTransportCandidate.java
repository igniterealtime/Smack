/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp.element;

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;
import org.jivesoftware.smackx.jingle_rtp.CandidateType;

/**
 * IceUdpTransportCandidate for Jingle TransportCandidate transports.
 * XEP-0176: Jingle ICE-UDP Transport Method 1.1.1 (2021-03-04)
 * @see <a href="https://xmpp.org/extensions/xep-0176.html#table-2">XEP-0176 Table 2: Candidate Attributes</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5245">RFC5245: Interactive Connectivity Establishment (ICE):
 * A Protocol for Network Address Translator (NAT) Traversal for Offer/Answer Protocols</a>
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public class IceUdpTransportCandidate extends AbstractXmlElement implements Comparable<IceUdpTransportCandidate> {
    /**
     * The name of the "candidate" element.
     */
    public static final String ELEMENT = "candidate";

    public static final String NAMESPACE = IceUdpTransport.NAMESPACE;

    /**
     * The "component" ID for RTP components.
     */
    public static final int RTP_COMPONENT_ID = 1;

    /**
     * The "component" ID for RTCP components.
     */
    public static final int RTCP_COMPONENT_ID = 2;

    /**
     * The name of the "component" element.
     */
    public static final String ATTR_COMPONENT = "component";

    /**
     * The name of the "foundation" element.
     */
    public static final String ATTR_FOUNDATION = "foundation";

    /**
     * The name of the "generation" element.
     */
    public static final String ATTR_GENERATION = "generation";

    /**
     * The name of the "id" element.
     */
    public static final String ATTR_ID = "id";

    /**
     * The name of the "ip" element.
     */
    public static final String ATTR_IP = "ip";

    /**
     * The name of the "network" element.
     */
    public static final String ATTR_NETWORK = "network";

    /**
     * The name of the "port" element.
     */
    public static final String ATTR_PORT = "port";

    /**
     * The name of the "priority" element.
     */
    public static final String ATTR_PRIORITY = "priority";

    /**
     * The name of the "protocol" element.
     */
    public static final String ATTR_PROTOCOL = "protocol";

    /**
     * The name of the "rel-addr" element.
     */
    public static final String ATTR_REL_ADDR = "rel-addr";

    /**
     * The name of the "rel-port" element.
     */
    public static final String ATTR_REL_PORT = "rel-port";

    /**
     * The name of the "type" element.
     */
    public static final String ATTR_TYPE = "type";

    /**
     * The name of the "tcptype" element.
     */
    public static final String ATTR_TCPTYPE = "tcptype";

    public static final String ATTR_HOST = "host";

    public IceUdpTransportCandidate() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>IceUdpTransportCandidate</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public IceUdpTransportCandidate(Builder builder) {
        super(builder);
    }

    /**
     * Returns a component ID as defined in ICE-CORE.
     *
     * @return a component ID as defined in ICE-CORE.
     */
    public int getComponent() {
        return getAttributeAsInt(ATTR_COMPONENT);
    }

    /**
     * Returns the candidate foundation as defined in ICE-CORE.
     *
     * @return the candidate foundation as defined in ICE-CORE.
     */
    public String getFoundation() {
        return getAttributeValue(ATTR_FOUNDATION);
    }

    /**
     * Returns this candidate's generation. A generation is an index, starting at 0, that enables
     * the parties to keep track of updates to the candidate throughout the life of the session. For
     * details, see the ICE Restarts section of XEP-0176.
     *
     * @return this candidate's generation index.
     */
    public int getGeneration() {
        return getAttributeAsInt(ATTR_GENERATION);
    }

    /**
     * Returns this candidates' unique identifier <code>String</code>.
     *
     * @return this candidates' unique identifier <code>String</code>
     */
    public String getID() {
        return getAttributeValue(ATTR_ID);
    }

    /**
     * Returns this candidate's Internet Protocol (IP) address; this can be either an IPv4 address
     * or an IPv6 address.
     *
     * @return this candidate's IPv4 or IPv6 address.
     */
    public String getIP() {
        return getAttributeValue(ATTR_IP);
    }

    /**
     * Returns the network index indicating the interface that the candidate belongs to. The network
     * ID is used for diagnostic purposes only in cases where the calling hardware has more than one
     * Network Interface Card.
     *
     * @return the network index indicating the interface that the candidate belongs to.
     */
    public int getNetwork() {
        return getAttributeAsInt(ATTR_NETWORK);
    }

    /**
     * Returns this candidate's port number.
     *
     * @return this candidate's port number.
     */
    public int getPort() {
        return getAttributeAsInt(ATTR_PORT);
    }

    /**
     * This candidate's priority as defined in ICE's RFC 5245.
     *
     * @return this candidate's priority
     */
    public int getPriority() {
        return getAttributeAsInt(ATTR_PRIORITY);
    }

    /**
     * Sets this candidate's transport protocol.
     *
     * @return this candidate's transport protocol.
     */
    public String getProtocol() {
        return getAttributeValue(ATTR_PROTOCOL);
    }

    /**
     * Returns this candidate's related address as described by ICE's RFC 5245.
     *
     * @return this candidate's related address as described by ICE's RFC 5245.
     */
    public String getRelAddr() {
        return getAttributeValue(ATTR_REL_ADDR);
    }

    /**
     * Returns this candidate's related port as described by ICE's RFC 5245.
     *
     * @return this candidate's related port as described by ICE's RFC 5245.
     */
    public int getRelPort() {
        return getAttributeAsInt(ATTR_REL_PORT);
    }

    /**
     * Returns a Candidate Type as defined in ICE-CORE. The allowable values are "host" for host candidates,
     * "prflx" for peer reflexive candidates, "relay" for relayed candidates, and
     * "srflx" for server reflexive candidates. All allowable values are enumerated in the
     * {@link CandidateType} enum.
     *
     * @return this candidates' type as per ICE's RFC 5245.
     */
    public CandidateType getType() {
        return CandidateType.fromString(getAttributeValue(ATTR_TYPE));
    }

    /**
     * Gets the TCP type for this <code>IceUdpTransportCandidate</code>.
     *
     * @return TcpType string
     */
    public String getTcpType() {
        return getAttributeValue(ATTR_TCPTYPE);
    }

    /**
     * Compares this instance with another IceUdpTransportCandidate by preference of type:
     * host &lt; local &lt; prflx &lt; srflx &lt; stun &lt; relay.
     *
     * @return 0 if the type are equal. -1 if this instance type is preferred. Otherwise 1.
     */
    @Override
    public int compareTo(IceUdpTransportCandidate iceUdpCandidate) {
        // If the types are different.
        if (this.getType() != iceUdpCandidate.getType()) {
            CandidateType[] types = {
                    CandidateType.host,
                    CandidateType.local,
                    CandidateType.prflx,
                    CandidateType.srflx,
                    CandidateType.stun,
                    CandidateType.relay
            };

            for (CandidateType type : types) {
                // this object is preferred.
                if (type == this.getType()) {
                    return -1;
                }
                // the candidatePacketExtension is preferred.
                else if (type == iceUdpCandidate.getType()) {
                    return 1;
                }
            }
        }
        // If the types are equal.
        return 0;
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for IceUdpTransportCandidate. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the IceUdpTransportCandidate.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, IceUdpTransportCandidate> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets a component ID as defined in ICE-CORE.
         *
         * @param component a component ID as defined in ICE-CORE.
         * @return builder instance
         */
        public Builder setComponent(int component) {
            addAttribute(ATTR_COMPONENT, component);
            return this;
        }

        /**
         * Sets the candidate foundation as defined in ICE-CORE.
         *
         * @param foundation the candidate foundation as defined in ICE-CORE.
         * @return builder instance
         */
        public Builder setFoundation(String foundation) {
            addAttribute(ATTR_FOUNDATION, foundation);
            return this;
        }

        /**
         * Sets this candidate's generation index. A generation is an index, starting at 0, that enables
         * the parties to keep track of updates to the candidate throughout the life of the session. For
         * details, see the ICE Restarts section of XEP-0176.
         *
         * @param generation this candidate's generation index.
         * @return builder instance
         */
        public Builder setGeneration(int generation) {
            addAttribute(ATTR_GENERATION, generation);
            return this;
        }

        /**
         * Sets this candidates' unique identifier <code>String</code>.
         *
         * @param id this candidates' unique identifier <code>String</code>
         * @return builder instance
         */
        public Builder setID(String id) {
            addAttribute(ATTR_ID, id);
            return this;
        }

        /**
         * Sets this candidate's Internet Protocol (IP) address; this can be either an IPv4 address or
         * an IPv6 address.
         *
         * @param ip this candidate's IPv4 or IPv6 address.
         * @return builder instance
         */
        public Builder setIP(String ip) {
            addAttribute(ATTR_IP, ip);
            return this;
        }

        /**
         * The network index indicating the interface that the candidate belongs to. The network ID is
         * used for diagnostic purposes only in cases where the calling hardware has more than one
         * Network Interface Card.
         *
         * @param network the network index indicating the interface that the candidate belongs to.
         * @return builder instance
         */
        public Builder setNetwork(int network) {
            addAttribute(ATTR_NETWORK, network);
            return this;
        }

        /**
         * Sets this candidate's port number.
         *
         * @param port this candidate's port number.
         * @return builder instance
         */
        public Builder setPort(int port) {
            if (port < 0) {
                throw new IllegalArgumentException("Port MUST NOT be less than 0.");
            }
            addAttribute(ATTR_PORT, port);
            return this;
        }

        /**
         * This candidate's priority as defined in ICE's RFC 5245.
         *
         * @param priority this candidate's priority
         * @return builder instance
         */
        public Builder setPriority(long priority) {
            if (priority < 0) {
                throw new IllegalArgumentException("Priority MUST NOT be less than 0.");
            }
            addAttribute(ATTR_PRIORITY, Long.toString(priority));
            return this;
        }

        /**
         * Sets this candidate's transport protocol.
         *
         * @param protocol this candidate's transport protocol.
         * @return builder instance
         */
        public Builder setProtocol(String protocol) {
            addAttribute(ATTR_PROTOCOL, protocol);
            return this;
        }

        /**
         * Sets this candidate's related address as described by ICE's RFC 5245.
         *
         * @param relAddr this candidate's related address as described by ICE's RFC 5245.
         * @return builder instance
         */
        public Builder setRelAddr(String relAddr) {
            addAttribute(ATTR_REL_ADDR, relAddr);
            return this;
        }

        /**
         * Sets this candidate's related port as described by ICE's RFC 5245.
         *
         * @param relPort this candidate's related port as described by ICE's RFC 5245.
         * @return builder instance
         */
        public Builder setRelPort(int relPort) {
            addAttribute(ATTR_REL_PORT, relPort);
            return this;
        }

        /**
         * Sets a Candidate Type as defined in ICE-CORE. The allowable values are "host" for host
         * candidates, "prflx" for peer reflexive candidates, "relay" for relayed candidates, and
         * "srflx" for server reflexive candidates. All allowable values are enumerated in the
         * {@link CandidateType} enum.
         *
         * @param type the candidates' type as per ICE's RFC 5245.
         * @return builder instance
         */
        public Builder setType(CandidateType type) {
            addAttribute(ATTR_TYPE, type.toString());
            return this;
        }

        /**
         * Sets the TCP type for this <code>IceUdpTransportCandidate</code>.
         *
         * @param tcpType TCP Type
         * @return builder instance
         */
        public Builder setTcpType(String tcpType) {
            addAttribute(ATTR_TCPTYPE, tcpType);
            return this;
        }

        @Override
        public IceUdpTransportCandidate build() {
            return new IceUdpTransportCandidate(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
