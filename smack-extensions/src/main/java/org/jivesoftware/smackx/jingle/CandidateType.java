/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.jivesoftware.smackx.jingle;

/**
 * An enumeration containing allowed types for {@link IceUdpTransportCandidate}.
 * XEP-0176: Jingle ICE-UDP Transport Method 1.1.1 (2021-03-04)
 *
 * @author Emil Ivov
 * @see <a href="https://xmpp.org/extensions/xep-0176.html#protocol-syntax">XEP-0176 § 5.3 Syntax</a>
 */
public enum CandidateType {
    /**
     * Indicates that a candidate is a Host Candidate: <br/>
     * A candidate obtained by binding to a specific port from an IP address on the host. This
     * includes IP addresses on physical interfaces and logical ones, such as ones obtained through
     * Virtual Private Networks (VPNs) and Realm Specific IP.
     */
    host,

    /**
     * Indicates that a candidate is a Peer Reflexive Candidate: <br/>
     * A candidate whose IP address and port are a binding allocated by a NAT for an agent when it
     * sent a STUN Binding request through the NAT to its peer.
     */
    prflx,

    /**
     * Indicates that a candidate is a Relayed Candidate:<br/>
     * A candidate obtained by sending a TURN Allocate request from a host candidate to a TURN
     * server. The relayed candidate is resident on the TURN server, and the TURN server relays
     * packets back towards the agent.
     */
    relay,

    /**
     * Indicates that a candidate is a Server Reflexive Candidate:<br/>
     * A candidate whose IP address and port are a binding allocated by a NAT for an agent when it
     * sent a packet through the NAT to a server. Server reflexive candidates can be learned by STUN
     * servers using the Binding request, or TURN servers, which provides both a relayed and server
     * reflexive candidate.
     */
    srflx,

    /**
     * Old name for Server Reflexive Candidate used by Google Talk.
     */
    stun,

    /**
     * Old name for Host Candidate used by Google Talk.
     */
    local;

    CandidateType() {
    }

    public static CandidateType fromString(String name) {
        for (CandidateType t : CandidateType.values()) {
            if (t.toString().equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Illegal type: " + name);
    }
}
