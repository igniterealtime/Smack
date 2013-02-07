/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

