/**
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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.io.IOException;
import java.net.Socket;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.Cache;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;

/**
 * Socks5BytestreamRequest class handles incoming SOCKS5 Bytestream requests.
 * 
 * @author Henning Staib
 */
public class Socks5BytestreamRequest implements BytestreamRequest {

    /* lifetime of an Item in the blacklist */
    private static final long BLACKLIST_LIFETIME = 60 * 1000 * 120;

    /* size of the blacklist */
    private static final int BLACKLIST_MAX_SIZE = 100;

    /* blacklist of addresses of SOCKS5 proxies */
    private static final Cache<String, Integer> ADDRESS_BLACKLIST = new Cache<String, Integer>(
                    BLACKLIST_MAX_SIZE, BLACKLIST_LIFETIME);

    /*
     * The number of connection failures it takes for a particular SOCKS5 proxy to be blacklisted.
     * When a proxy is blacklisted no more connection attempts will be made to it for a period of 2
     * hours.
     */
    private static int CONNECTION_FAILURE_THRESHOLD = 2;

    /* the bytestream initialization request */
    private Bytestream bytestreamRequest;

    /* SOCKS5 Bytestream manager containing the XMPP connection and helper methods */
    private Socks5BytestreamManager manager;

    /* timeout to connect to all SOCKS5 proxies */
    private int totalConnectTimeout = 10000;

    /* minimum timeout to connect to one SOCKS5 proxy */
    private int minimumConnectTimeout = 2000;

    /**
     * Returns the number of connection failures it takes for a particular SOCKS5 proxy to be
     * blacklisted. When a proxy is blacklisted no more connection attempts will be made to it for a
     * period of 2 hours. Default is 2.
     * 
     * @return the number of connection failures it takes for a particular SOCKS5 proxy to be
     *         blacklisted
     */
    public static int getConnectFailureThreshold() {
        return CONNECTION_FAILURE_THRESHOLD;
    }

    /**
     * Sets the number of connection failures it takes for a particular SOCKS5 proxy to be
     * blacklisted. When a proxy is blacklisted no more connection attempts will be made to it for a
     * period of 2 hours. Default is 2.
     * <p>
     * Setting the connection failure threshold to zero disables the blacklisting.
     * 
     * @param connectFailureThreshold the number of connection failures it takes for a particular
     *        SOCKS5 proxy to be blacklisted
     */
    public static void setConnectFailureThreshold(int connectFailureThreshold) {
        CONNECTION_FAILURE_THRESHOLD = connectFailureThreshold;
    }

    /**
     * Creates a new Socks5BytestreamRequest.
     * 
     * @param manager the SOCKS5 Bytestream manager
     * @param bytestreamRequest the SOCKS5 Bytestream initialization packet
     */
    protected Socks5BytestreamRequest(Socks5BytestreamManager manager, Bytestream bytestreamRequest) {
        this.manager = manager;
        this.bytestreamRequest = bytestreamRequest;
    }

    /**
     * Returns the maximum timeout to connect to SOCKS5 proxies. Default is 10000ms.
     * <p>
     * When accepting a SOCKS5 Bytestream request Smack tries to connect to all SOCKS5 proxies given
     * by the initiator until a connection is established. This timeout divided by the number of
     * SOCKS5 proxies determines the timeout for every connection attempt.
     * <p>
     * You can set the minimum timeout for establishing a connection to one SOCKS5 proxy by invoking
     * {@link #setMinimumConnectTimeout(int)}.
     * 
     * @return the maximum timeout to connect to SOCKS5 proxies
     */
    public int getTotalConnectTimeout() {
        if (this.totalConnectTimeout <= 0) {
            return 10000;
        }
        return this.totalConnectTimeout;
    }

    /**
     * Sets the maximum timeout to connect to SOCKS5 proxies. Default is 10000ms.
     * <p>
     * When accepting a SOCKS5 Bytestream request Smack tries to connect to all SOCKS5 proxies given
     * by the initiator until a connection is established. This timeout divided by the number of
     * SOCKS5 proxies determines the timeout for every connection attempt.
     * <p>
     * You can set the minimum timeout for establishing a connection to one SOCKS5 proxy by invoking
     * {@link #setMinimumConnectTimeout(int)}.
     * 
     * @param totalConnectTimeout the maximum timeout to connect to SOCKS5 proxies
     */
    public void setTotalConnectTimeout(int totalConnectTimeout) {
        this.totalConnectTimeout = totalConnectTimeout;
    }

    /**
     * Returns the timeout to connect to one SOCKS5 proxy while accepting the SOCKS5 Bytestream
     * request. Default is 2000ms.
     * 
     * @return the timeout to connect to one SOCKS5 proxy
     */
    public int getMinimumConnectTimeout() {
        if (this.minimumConnectTimeout <= 0) {
            return 2000;
        }
        return this.minimumConnectTimeout;
    }

    /**
     * Sets the timeout to connect to one SOCKS5 proxy while accepting the SOCKS5 Bytestream
     * request. Default is 2000ms.
     * 
     * @param minimumConnectTimeout the timeout to connect to one SOCKS5 proxy
     */
    public void setMinimumConnectTimeout(int minimumConnectTimeout) {
        this.minimumConnectTimeout = minimumConnectTimeout;
    }

    /**
     * Returns the sender of the SOCKS5 Bytestream initialization request.
     * 
     * @return the sender of the SOCKS5 Bytestream initialization request.
     */
    public String getFrom() {
        return this.bytestreamRequest.getFrom();
    }

    /**
     * Returns the session ID of the SOCKS5 Bytestream initialization request.
     * 
     * @return the session ID of the SOCKS5 Bytestream initialization request.
     */
    public String getSessionID() {
        return this.bytestreamRequest.getSessionID();
    }

    /**
     * Accepts the SOCKS5 Bytestream initialization request and returns the socket to send/receive
     * data.
     * <p>
     * Before accepting the SOCKS5 Bytestream request you can set timeouts by invoking
     * {@link #setTotalConnectTimeout(int)} and {@link #setMinimumConnectTimeout(int)}.
     * 
     * @return the socket to send/receive data
     * @throws XMPPException if connection to all SOCKS5 proxies failed or if stream is invalid.
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public Socks5BytestreamSession accept() throws XMPPException, InterruptedException {
        Collection<StreamHost> streamHosts = this.bytestreamRequest.getStreamHosts();

        // throw exceptions if request contains no stream hosts
        if (streamHosts.size() == 0) {
            cancelRequest();
        }

        StreamHost selectedHost = null;
        Socket socket = null;

        String digest = Socks5Utils.createDigest(this.bytestreamRequest.getSessionID(),
                        this.bytestreamRequest.getFrom(), this.manager.getConnection().getUser());

        /*
         * determine timeout for each connection attempt; each SOCKS5 proxy has the same amount of
         * time so that the first does not consume the whole timeout
         */
        int timeout = Math.max(getTotalConnectTimeout() / streamHosts.size(),
                        getMinimumConnectTimeout());

        for (StreamHost streamHost : streamHosts) {
            String address = streamHost.getAddress() + ":" + streamHost.getPort();

            // check to see if this address has been blacklisted
            int failures = getConnectionFailures(address);
            if (CONNECTION_FAILURE_THRESHOLD > 0 && failures >= CONNECTION_FAILURE_THRESHOLD) {
                continue;
            }

            // establish socket
            try {

                // build SOCKS5 client
                final Socks5Client socks5Client = new Socks5Client(streamHost, digest);

                // connect to SOCKS5 proxy with a timeout
                socket = socks5Client.getSocket(timeout);

                // set selected host
                selectedHost = streamHost;
                break;

            }
            catch (TimeoutException e) {
                incrementConnectionFailures(address);
            }
            catch (IOException e) {
                incrementConnectionFailures(address);
            }
            catch (XMPPException e) {
                incrementConnectionFailures(address);
            }

        }

        // throw exception if connecting to all SOCKS5 proxies failed
        if (selectedHost == null || socket == null) {
            cancelRequest();
        }

        // send used-host confirmation
        Bytestream response = createUsedHostResponse(selectedHost);
        this.manager.getConnection().sendPacket(response);

        return new Socks5BytestreamSession(socket, selectedHost.getJID().equals(
                        this.bytestreamRequest.getFrom()));

    }

    /**
     * Rejects the SOCKS5 Bytestream request by sending a reject error to the initiator.
     */
    public void reject() {
        this.manager.replyRejectPacket(this.bytestreamRequest);
    }

    /**
     * Cancels the SOCKS5 Bytestream request by sending an error to the initiator and building a
     * XMPP exception.
     * 
     * @throws XMPPException XMPP exception containing the XMPP error
     */
    private void cancelRequest() throws XMPPException {
        String errorMessage = "Could not establish socket with any provided host";
        XMPPError error = new XMPPError(XMPPError.Condition.item_not_found, errorMessage);
        IQ errorIQ = IQ.createErrorResponse(this.bytestreamRequest, error);
        this.manager.getConnection().sendPacket(errorIQ);
        throw new XMPPException(errorMessage, error);
    }

    /**
     * Returns the response to the SOCKS5 Bytestream request containing the SOCKS5 proxy used.
     * 
     * @param selectedHost the used SOCKS5 proxy
     * @return the response to the SOCKS5 Bytestream request
     */
    private Bytestream createUsedHostResponse(StreamHost selectedHost) {
        Bytestream response = new Bytestream(this.bytestreamRequest.getSessionID());
        response.setTo(this.bytestreamRequest.getFrom());
        response.setType(IQ.Type.RESULT);
        response.setPacketID(this.bytestreamRequest.getPacketID());
        response.setUsedHost(selectedHost.getJID());
        return response;
    }

    /**
     * Increments the connection failure counter by one for the given address.
     * 
     * @param address the address the connection failure counter should be increased
     */
    private void incrementConnectionFailures(String address) {
        Integer count = ADDRESS_BLACKLIST.get(address);
        ADDRESS_BLACKLIST.put(address, count == null ? 1 : count + 1);
    }

    /**
     * Returns how often the connection to the given address failed.
     * 
     * @param address the address
     * @return number of connection failures
     */
    private int getConnectionFailures(String address) {
        Integer count = ADDRESS_BLACKLIST.get(address);
        return count != null ? count : 0;
    }

}
