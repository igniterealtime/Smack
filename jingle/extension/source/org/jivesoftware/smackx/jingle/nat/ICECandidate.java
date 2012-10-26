/**
 * $RCSfile: ICECandidate.java,v $
 * $Revision: 1.2 $
 * $Date: 2007/07/03 16:36:31 $
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import org.jivesoftware.smackx.jingle.SmackLogger;

/**
 * ICE Transport candidate.
 * <p/>
 * A candidate represents the possible transport for data interchange between
 * the two endpoints.
 *
 * @author Thiago Camargo
 */
public class ICECandidate extends TransportCandidate implements Comparable<ICECandidate> {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(ICECandidate.class);

	private String id; // An identification

    private String username;

    private int preference;

    private Protocol proto;

    private Channel channel;

    private int network;

    private Type type;

    public enum Type {
        relay, srflx, prflx, local, host
    }

    public ICECandidate() {
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
     * @param type       type as defined in ICE-12
     */
    public ICECandidate(String ip, int generation, int network,
            String password, int port, String username,
            int preference, Type type) {
        super(ip, port, generation);

        proto = Protocol.UDP;
        channel = Channel.MYRTPVOICE;

        this.network = network;
        this.password = password;
        this.username = username;
        this.preference = preference;
        this.type = type;
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

    /**
     * Get the Candidate Type
     *
     * @return candidate Type
     */
    public Type getType() {
        return type;
    }

    /**
     * Set the Candidate Type
     *
     * @param type candidate type.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Check if a transport candidate is usable. The transport resolver should
     * check if the transport candidate the other endpoint has provided is
     * usable.
     * <p/>
     * ICE Candidate can check connectivity using UDP echo Test.
     */
    public void check(final List<TransportCandidate> localCandidates) {
        //TODO candidate is being checked trigger
        //candidatesChecking.add(cand);

        final ICECandidate checkingCandidate = this;

        Thread checkThread = new Thread(new Runnable() {
            public void run() {

                final TestResult result = new TestResult();

                // Media Proxy don't have Echo features.
                // If its a relayed candidate we assumpt that is NOT Valid while other candidates still being checked.
                // The negotiator MUST add then in the correct situations
                if (getType().equals("relay")) {
                    triggerCandidateChecked(false);
                    return;
                }

                ResultListener resultListener = new ResultListener() {
                    public void testFinished(TestResult testResult, TransportCandidate candidate) {
                        if (testResult.isReachable() && checkingCandidate.equals(candidate)) {
                            result.setResult(true);
                            LOGGER.debug("Candidate reachable: " + candidate.getIp() + ":" + candidate.getPort() + " from " + getIp() +":" + getPort());
                        }
                    }
                };

                for (TransportCandidate candidate : localCandidates) {
                    CandidateEcho echo = candidate.getCandidateEcho();
                    if (echo != null) {
                        if (candidate instanceof ICECandidate) {
                            ICECandidate iceCandidate = (ICECandidate) candidate;
                            if (iceCandidate.getType().equals(getType())) {
                                try {
                                    echo.addResultListener(resultListener);
                                    InetAddress address = InetAddress.getByName(getIp());
                                    echo.testASync(checkingCandidate, getPassword());
                                }
                                catch (UnknownHostException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }

                for (int i = 0; i < 10 && !result.isReachable(); i++)
                    try {
                        LOGGER.error("ICE Candidate retry #" + i);
                        Thread.sleep(400);
                    }
                    catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                for (TransportCandidate candidate : localCandidates) {
                    CandidateEcho echo = candidate.getCandidateEcho();
                    if (echo != null) {
                        echo.removeResultListener(resultListener);
                    }
                }

                triggerCandidateChecked(result.isReachable());

                //TODO candidate is being checked trigger
                //candidatesChecking.remove(cand);
            }
        }, "Transport candidate check");

        checkThread.setName("Transport candidate test");
        checkThread.start();
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

        final ICECandidate other = (ICECandidate) obj;
        if (getChannel() == null) {
            if (other.getChannel() != null) {
                return false;
            }
        }
        else if (!getChannel().equals(other.getChannel())) {
            return false;
        }
        if (getId() == null) {
            if (other.getId() != null) {
                return false;
            }
        }
        else if (!getId().equals(other.getId())) {
            return false;
        }
        if (getNetwork() != other.getNetwork()) {
            return false;
        }
        if (getPassword() == null) {
            if (other.getPassword() != null) {
                return false;
            }
        }
        else if (!getPassword().equals(other.password)) {
            return false;
        }
        if (getPreference() != other.getPreference()) {
            return false;
        }
        if (getProto() == null) {
            if (other.getProto() != null) {
                return false;
            }
        }
        else if (!getProto().equals(other.getProto())) {
            return false;
        }
        if (getUsername() == null) {
            if (other.getUsername() != null) {
                return false;
            }
        }
        else if (!getUsername().equals(other.getUsername())) {
            return false;
        }

        if (getIp() == null) {
            if (other.getIp() != null) {
                return false;
            }
        }
        else if (!getIp().equals(other.getIp())) {
            return false;
        }

        if (getPort() != other.getPort()) {
            return false;
        }

        if (getType() == null) {
            if (other.getType() != null) {
                return false;
            }
        }
        else if (!getType().equals(other.getType())) {
            return false;
        }

        return true;
    }

    public boolean isNull() {
        if (super.isNull()) {
            return true;
        }
        else if (getProto().isNull()) {
            return true;
        }
        else if (getChannel().isNull()) {
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
	public int compareTo(ICECandidate arg) {
		if (getPreference() < arg.getPreference()) {
			return -1;
		} else if (getPreference() > arg.getPreference()) {
			return 1;
		}
		return 0;
	}

}

