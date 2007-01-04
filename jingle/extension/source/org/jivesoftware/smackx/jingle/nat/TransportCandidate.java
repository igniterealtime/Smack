/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPConnection;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Transport candidate.
 *
 * A candidate represents the possible transport for data interchange between
 * the two endpoints.
 *
 * @author Thiago Camargo
 * @author Alvaro Saurin
 */
public abstract class TransportCandidate {

    private String name;

    private String ip; // IP address

    private int port; // Port to use, or 0 for any port

    private String localIp;

    private int generation;

    protected String password;

    private String sessionId;

    private XMPPConnection connection;

    private TransportCandidate symmetric;

    // Listeners for events
    private final List<TransportResolverListener.Checker> listeners = new ArrayList();

    public String getIp() {
        return ip;
    }

    /**
     * Set the IP address.
     *
     * @param ip the IP address
     */
    public void setIp(String ip) {
        this.ip = ip;
    }

    /**
     * Get local IP to bind to this candidate
     *
     * @return
     */
    public String getLocalIp() {
        return localIp == null ? ip : localIp;
    }

    /**
     * Set local IP to bind to this candidate
     *
     * @param localIp
     */
    public void setLocalIp(String localIp) {
        this.localIp = localIp;
    }

    /**
     * Get the symetric candidate for this candidate if it exists.
     *
     * @return
     */
    public TransportCandidate getSymmetric() {
        return symmetric;
    }

    /**
     * Set the symetric candidate for this candidate.
     *
     * @param symetric
     */
    public void setSymmetric(TransportCandidate symetric) {
        this.symmetric = symetric;
    }

    /**
     * Get the password used by ICE or relayed candidate
     *
     * @return a password
     */
    public String getPassword() {
        return password;
    }

    /**
     * Set the password used by ICE or relayed candidate
     *
     * @param password a password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Get the XMPPConnection use to send or receive this candidate
     *
     * @return
     */
    public XMPPConnection getConnection() {
        return connection;
    }

    /**
     * Set the XMPPConnection use to send or receive this candidate
     *
     * @param connection
     */
    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Get the jingle´s sessionId that is using this candidate
     *
     * @return
     */
    public String getSessionId() {
        return sessionId;
    }

    /**
     * Set the jingle´s sessionId that is using this candidate
     *
     * @param sessionId
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Empty constructor
     */
    public TransportCandidate() {
        this(null, 0, 0);
    }

    /**
     * Constructor with IP address and port
     *
     * @param ip   The IP address.
     * @param port The port number.
     */
    public TransportCandidate(String ip, int port) {
        this(ip, port, 0);
    }

    /**
     * Constructor with IP address and port
     *
     * @param ip         The IP address.
     * @param port       The port number.
     * @param generation The generation
     */
    public TransportCandidate(String ip, int port, int generation) {
        this.ip = ip;
        this.port = port;
        this.generation = generation;
    }

    /**
     * Return true if the candidate is not valid.
     *
     * @return true if the candidate is null.
     */
    public boolean isNull() {
        if (ip == null) {
            return true;
        } else if (ip.length() == 0) {
            return true;
        } else if (port < 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Get the port, or 0 for any port.
     *
     * @return the port or 0
     */
    public int getPort() {
        return port;
    }

    /**
     * Set the port, using 0 for any port
     *
     * @param port the port
     */
    public void setPort(int port) {
        this.port = port;
    }

    /**
     * Get the generation for a transportElement definition
     *
     * @return the generation
     */
    public int getGeneration() {
        return generation;
    }

    /**
     * Set the generation for a transportElement definition.
     *
     * @param generation the generation number
     */
    public void setGeneration(int generation) {
        this.generation = generation;
    }

    /**
     * Get the name used for identifying this transportElement method (optional)
     *
     * @return a name used for identifying this transportElement (ie,
     *         "myrtpvoice1")
     */
    public String getName() {
        return name;
    }

    /**
     * Set a name for identifying this transportElement.
     *
     * @param name the name used for the transportElement
     */
    public void setName(String name) {
        this.name = name;
    }

    /*
      * (non-Javadoc)
      *
      * @see java.lang.Object#equals(java.lang.Object)
      */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TransportCandidate other = (TransportCandidate) obj;
        if (generation != other.generation) {
            return false;
        }
        if (getIp() == null) {
            if (other.getIp() != null) {
                return false;
            }
        } else if (!getIp().equals(other.getIp())) {
            return false;
        }
        if (getName() == null) {
            if (other.getName() != null) {
                return false;
            }
        } else if (!getName().equals(other.getName())) {
            return false;
        }
        if (getPort() != other.getPort()) {
            return false;
        }
        return true;
    }


    /**
     * Check if a transport candidate is usable. The transport resolver should
     * check if the transport candidate the other endpoint has provided is
     * usable.
     * <p/>
     * This method provides a basic check where it sends a "ping" to the remote
     * address provided in the candidate. If the "ping" succedess, the candidate
     * is accepted. Subclasses should provide better methods if they can...
     */
    public void check() {
        //TODO candidate is being checked trigger
        //candidatesChecking.add(cand);

        Thread checkThread = new Thread(new Runnable() {
            public void run() {
                boolean isUsable;

                InetAddress candAddress;
                try {
                    candAddress = InetAddress.getByName(getIp());
                    isUsable = true;//candAddress.isReachable(CHECK_TIMEOUT);
                } catch (Exception e) {
                    isUsable = false;
                }
                triggerCandidateChecked(isUsable);

                //TODO candidate is being checked trigger
                //candidatesChecking.remove(cand);
            }
        }, "Transport candidate check");

        checkThread.setName("Transport candidate test");
        checkThread.start();
    }

    /**
     * Trigger a new candidate checked event.
     *
     * @param result The result.
     */
    private void triggerCandidateChecked(boolean result) {

        for (TransportResolverListener.Checker trl : getListenersList()) {
            trl.candidateChecked(this, result);
        }
    }

    /**
     * Get the list of listeners
     *
     * @return the list of listeners
     */
    public List<TransportResolverListener.Checker> getListenersList() {
        synchronized (listeners) {
            return new ArrayList(listeners);
        }
    }

    /**
     * Add a transport resolver listener.
     *
     * @param li The transport resolver listener to be added.
     */
    public void addListener(TransportResolverListener.Checker li) {
        synchronized (listeners) {
            listeners.add(li);
        }
    }

    /**
     * Fixed transport candidate
     */
    public static class Fixed extends TransportCandidate {

        public Fixed() {
            super();
        }

        /**
         * Constructor with IP address and port
         *
         * @param ip   The IP address.
         * @param port The port number.
         */
        public Fixed(String ip, int port) {
            super(ip, port);
        }

        /**
         * Constructor with IP address and port
         *
         * @param ip         The IP address.
         * @param port       The port number.
         * @param generation The generation
         */
        public Fixed(String ip, int port, int generation) {
            super(ip, port, generation);
        }
    }

    /**
     * Ice candidate.
     */
    public static class Ice extends TransportCandidate implements Comparable {

        private String id; // An identification

        private String username;

        private int preference;

        private Protocol proto;

        private Channel channel;

        private int network;

        public Ice() {
            super();
        }

        /**
         * Constructor with the basic elements of a transport definition.
         *
         * @param ip         the IP address to use as a local address
         * @param generation used to keep track of the candidates
         * @param network    used for diagnostics (used when the machine has
         *                   several NICs)
         * @param password   user name, as it is used in ICE
         * @param port       the port at the candidate IP address
         * @param username   user name, as it is used in ICE
         * @param preference preference for this transportElement, as it is used
         *                   in ICE
         */
        public Ice(String ip, int generation, int network,
                   String password, int port, String username,
                   int preference) {
            super(ip, port, generation);

            proto = Protocol.UDP;
            channel = Channel.MYRTPVOICE;

            this.network = network;
            this.password = password;
            this.username = username;
            this.preference = preference;
        }

        /**
         * Get the ID
         *
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * Set the ID
         *
         * @param id the id to set
         */
        public void setId(String id) {
            this.id = id;
        }

        /**
         * Get the protocol used for the transmission
         *
         * @return the protocol used for transmission
         */
        public Protocol getProto() {
            return proto;
        }

        /**
         * Set the protocol for the transmission
         *
         * @param proto the protocol to use
         */
        public void setProto(Protocol proto) {
            this.proto = proto;
        }

        /**
         * Get the network interface used for this connection
         *
         * @return the interface number
         */
        public int getNetwork() {
            return network;
        }

        /**
         * Set the interface for this connection
         *
         * @param network the interface number
         */
        public void setNetwork(int network) {
            this.network = network;
        }

        /**
         * Get the username for this transportElement in ICE
         *
         * @return a username string
         */
        public String getUsername() {
            return username;
        }

        /**
         * Get the channel
         *
         * @return the channel associated
         */
        public Channel getChannel() {
            return channel;
        }

        /**
         * Set the channel for this transportElement
         *
         * @param channel the new channel
         */
        public void setChannel(Channel channel) {
            this.channel = channel;
        }

        /**
         * Set the username for this transportElement in ICE
         *
         * @param username the username used in ICE
         */
        public void setUsername(String username) {
            this.username = username;
        }

        /**
         * Get the preference number for this transportElement
         *
         * @return the preference for this transportElement
         */
        public int getPreference() {
            return preference;
        }

        /**
         * Set the preference order for this transportElement
         *
         * @param preference a number identifying the preference (as defined in
         *                   ICE)
         */
        public void setPreference(int preference) {
            this.preference = preference;
        }

        /*
           * (non-Javadoc)
           *
           * @see java.lang.Object#equals(java.lang.Object)
           */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!super.equals(obj)) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }

            final Ice other = (Ice) obj;
            if (getChannel() == null) {
                if (other.getChannel() != null) {
                    return false;
                }
            } else if (!getChannel().equals(other.getChannel())) {
                return false;
            }
            if (getId() == null) {
                if (other.getId() != null) {
                    return false;
                }
            } else if (!getId().equals(other.getId())) {
                return false;
            }
            if (getNetwork() != other.getNetwork()) {
                return false;
            }
            if (getPassword() == null) {
                if (other.getPassword() != null) {
                    return false;
                }
            } else if (!getPassword().equals(other.password)) {
                return false;
            }
            if (getPreference() != other.getPreference()) {
                return false;
            }
            if (getProto() == null) {
                if (other.getProto() != null) {
                    return false;
                }
            } else if (!getProto().equals(other.getProto())) {
                return false;
            }
            if (getUsername() == null) {
                if (other.getUsername() != null) {
                    return false;
                }
            } else if (!getUsername().equals(other.getUsername())) {
                return false;
            }
            return true;
        }

        public boolean isNull() {
            if (super.isNull()) {
                return true;
            } else if (getProto().isNull()) {
                return true;
            } else if (getChannel().isNull()) {
                return true;
            }
            return false;
        }

        /**
         * Compare the to other Transport candidate.
         *
         * @param arg another Transport candidate
         * @return a negative integer, zero, or a positive integer as this
         *         object is less than, equal to, or greater than the specified
         *         object
         */
        public int compareTo(Object arg) {
            if (arg instanceof TransportCandidate.Ice) {
                TransportCandidate.Ice tc = (TransportCandidate.Ice) arg;
                if (getPreference() < tc.getPreference()) {
                    return -1;
                } else if (getPreference() > tc.getPreference()) {
                    return 1;
                }
            }
            return 0;
        }
    }

    /**
     * Type-safe enum for the transportElement protocol
     */
    public static class Protocol {

        public static final Protocol UDP = new Protocol("udp");

        public static final Protocol TCP = new Protocol("tcp");

        public static final Protocol TCPACT = new Protocol("tcp-act");

        public static final Protocol TCPPASS = new Protocol("tcp-pass");

        public static final Protocol SSLTCP = new Protocol("ssltcp");

        private String value;

        public Protocol(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the Protocol constant associated with the String value.
         */
        public static Protocol fromString(String value) {
            if (value == null) {
                return UDP;
            }
            value = value.toLowerCase();
            if (value.equals("udp")) {
                return UDP;
            } else if (value.equals("tcp")) {
                return TCP;
            } else if (value.equals("tcp-act")) {
                return TCPACT;
            } else if (value.equals("tcp-pass")) {
                return TCPPASS;
            } else if (value.equals("ssltcp")) {
                return SSLTCP;
            } else {
                return UDP;
            }
        }

        /*
           * (non-Javadoc)
           *
           * @see java.lang.Object#equals(java.lang.Object)
           */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Protocol other = (Protocol) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }

        /**
         * Return true if the protocol is not valid.
         *
         * @return true if the protocol is null
         */
        public boolean isNull() {
            if (value == null) {
                return true;
            } else if (value.length() == 0) {
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * Type-safe enum for the transportElement channel
     */
    public static class Channel {

        public static final Channel MYRTPVOICE = new Channel("myrtpvoice");

        public static final Channel MYRTCPVOICE = new Channel("myrtcpvoice");

        private String value;

        public Channel(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the MediaChannel constant associated with the String value.
         */
        public static Channel fromString(String value) {
            if (value == null) {
                return MYRTPVOICE;
            }
            value = value.toLowerCase();
            if (value.equals("myrtpvoice")) {
                return MYRTPVOICE;
            } else if (value.equals("tcp")) {
                return MYRTCPVOICE;
            } else {
                return MYRTPVOICE;
            }
        }

        /*
           * (non-Javadoc)
           *
           * @see java.lang.Object#equals(java.lang.Object)
           */
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final Channel other = (Channel) obj;
            if (value == null) {
                if (other.value != null) {
                    return false;
                }
            } else if (!value.equals(other.value)) {
                return false;
            }
            return true;
        }

        /**
         * Return true if the channel is not valid.
         *
         * @return true if the channel is null
         */
        public boolean isNull() {
            if (value == null) {
                return true;
            } else if (value.length() == 0) {
                return true;
            } else {
                return false;
            }
        }
    }
}
