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


import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;

/**
 * The abstract XMPPConnection class provides an interface for connections to a XMPP server and
 * implements shared methods which are used by the different types of connections (e.g.
 * {@link XMPPTCPConnection} or {@link XMPPBOSHConnection}). To create a connection to a XMPP server
 * a simple usage of this API might look like the following:
 * <p>
 * 
 * <pre>
 * // Create a connection to the igniterealtime.org XMPP server.
 * XMPPConnection con = new XMPPTCPConnection("igniterealtime.org");
 * // Connect to the server
 * con.connect();
 * // Most servers require you to login before performing other tasks.
 * con.login("jsmith", "mypass");
 * // Start a new conversation with John Doe and send him a message.
 * Chat chat = ChatManager.getInstanceFor(con).createChat(<font color="green">"jdoe@igniterealtime.org"</font>, new MessageListener() {
 *     public void processMessage(Chat chat, Message message) {
 *         // Print out any messages we get back to standard out.
 *         System.out.println(<font color="green">"Received message: "</font> + message);
 *     }
 * });
 * chat.sendMessage(<font color="green">"Howdy!"</font>);
 * // Disconnect from the server
 * con.disconnect();
 * </pre>
 * <p>
 * Connections can be reused between connections. This means that an Connection may be connected,
 * disconnected and then connected again. Listeners of the Connection will be retained across
 * connections.
 * 
 * @author Matt Tucker
 * @author Guenther Niess
 */
@SuppressWarnings("javadoc")
public interface XMPPConnection {

    /**
     * Returns the name of the service provided by the XMPP server for this connection.
     * This is also called XMPP domain of the connected server. After
     * authenticating with the server the returned value may be different.
     * 
     * @return the name of the service provided by the XMPP server.
     */
    public String getServiceName();

    /**
     * Returns the host name of the server where the XMPP server is running. This would be the
     * IP address of the server or a name that may be resolved by a DNS server.
     * 
     * @return the host name of the server where the XMPP server is running or null if not yet connected.
     */
    public String getHost();

    /**
     * Returns the port number of the XMPP server for this connection. The default port
     * for normal connections is 5222.
     * 
     * @return the port number of the XMPP server or 0 if not yet connected.
     */
    public int getPort();

    /**
     * Returns the full XMPP address of the user that is logged in to the connection or
     * <tt>null</tt> if not logged in yet. An XMPP address is in the form
     * username@server/resource.
     * 
     * @return the full XMPP address of the user logged in.
     */
    public String getUser();

    /**
     * Returns the connection ID for this connection, which is the value set by the server
     * when opening a XMPP stream. If the server does not set a connection ID, this value
     * will be null. This value will be <tt>null</tt> if not connected to the server.
     * 
     * @return the ID of this connection returned from the XMPP server or <tt>null</tt> if
     *      not connected to the server.
     */
    public String getConnectionID();

    /**
     * Returns true if currently connected to the XMPP server.
     * 
     * @return true if connected.
     */
    public boolean isConnected();

    /**
     * Returns true if currently authenticated by successfully calling the login method.
     * 
     * @return true if authenticated.
     */
    public boolean isAuthenticated();

    /**
     * Returns true if currently authenticated anonymously.
     * 
     * @return true if authenticated anonymously.
     */
    public boolean isAnonymous();

    /**
     * Returns true if the connection to the server has successfully negotiated encryption. 
     * 
     * @return true if a secure connection to the server.
     */
    public boolean isSecureConnection();

    /**
     * Returns true if network traffic is being compressed. When using stream compression network
     * traffic can be reduced up to 90%. Therefore, stream compression is ideal when using a slow
     * speed network connection. However, the server will need to use more CPU time in order to
     * un/compress network data so under high load the server performance might be affected.
     * 
     * @return true if network traffic is being compressed.
     */
    public boolean isUsingCompression();

    /**
     * Sends the specified packet to the server.
     * 
     * @param packet the packet to send.
     * @throws NotConnectedException 
     */
    public void sendPacket(Packet packet) throws NotConnectedException;

    /**
     * Returns the roster for the user.
     * <p>
     * This method will never return <code>null</code>, instead if the user has not yet logged into
     * the server or is logged in anonymously all modifying methods of the returned roster object
     * like {@link Roster#createEntry(String, String, String[])},
     * {@link Roster#removeEntry(RosterEntry)} , etc. except adding or removing
     * {@link RosterListener}s will throw an IllegalStateException.
     * 
     * @return the user's roster.
     * @throws IllegalStateException if the connection is anonymous
     */
    public Roster getRoster();

    /**
     * Adds a connection listener to this connection that will be notified when
     * the connection closes or fails.
     * 
     * @param connectionListener a connection listener.
     */
    public void addConnectionListener(ConnectionListener connectionListener);

    /**
     * Removes a connection listener from this connection.
     * 
     * @param connectionListener a connection listener.
     */
    public void removeConnectionListener(ConnectionListener connectionListener);

    /**
     * Creates a new packet collector collecting packets that are replies to <code>packet</code>.
     * Does also send <code>packet</code>. The packet filter for the collector is an
     * {@link IQReplyFilter}, guaranteeing that packet id and JID in the 'from' address have
     * expected values.
     *
     * @param packet the packet to filter responses from
     * @return a new packet collector.
     * @throws NotConnectedException 
     */
    public PacketCollector createPacketCollectorAndSend(IQ packet) throws NotConnectedException;

    /**
     * Creates a new packet collector for this connection. A packet filter determines
     * which packets will be accumulated by the collector. A PacketCollector is
     * more suitable to use than a {@link PacketListener} when you need to wait for
     * a specific result.
     * 
     * @param packetFilter the packet filter to use.
     * @return a new packet collector.
     */
    public PacketCollector createPacketCollector(PacketFilter packetFilter);

    /**
     * Remove a packet collector of this connection.
     * 
     * @param collector a packet collectors which was created for this connection.
     */
    public void removePacketCollector(PacketCollector collector);

    /**
     * Registers a packet listener with this connection. A packet listener will be invoked only
     * when an incoming packet is received. A packet filter determines
     * which packets will be delivered to the listener. If the same packet listener
     * is added again with a different filter, only the new filter will be used.
     * 
     * NOTE: If you want get a similar callback for outgoing packets, see {@link #addPacketInterceptor(PacketInterceptor, PacketFilter)}.
     * 
     * @param packetListener the packet listener to notify of new received packets.
     * @param packetFilter   the packet filter to use.
     */
    public void addPacketListener(PacketListener packetListener, PacketFilter packetFilter);

    /**
     * Removes a packet listener for received packets from this connection.
     * 
     * @param packetListener the packet listener to remove.
     * @return true if the packet listener was removed
     */
    public boolean removePacketListener(PacketListener packetListener);

    /**
     * Registers a packet listener with this connection. The listener will be
     * notified of every packet that this connection sends. A packet filter determines
     * which packets will be delivered to the listener. Note that the thread
     * that writes packets will be used to invoke the listeners. Therefore, each
     * packet listener should complete all operations quickly or use a different
     * thread for processing.
     * 
     * @param packetListener the packet listener to notify of sent packets.
     * @param packetFilter   the packet filter to use.
     */
    public void addPacketSendingListener(PacketListener packetListener, PacketFilter packetFilter);

    /**
     * Removes a packet listener for sending packets from this connection.
     * 
     * @param packetListener the packet listener to remove.
     */
    public void removePacketSendingListener(PacketListener packetListener);

    /**
     * Registers a packet interceptor with this connection. The interceptor will be
     * invoked every time a packet is about to be sent by this connection. Interceptors
     * may modify the packet to be sent. A packet filter determines which packets
     * will be delivered to the interceptor.
     * 
     * <p>
     * NOTE: For a similar functionality on incoming packets, see {@link #addPacketListener(PacketListener, PacketFilter)}.
     *
     * @param packetInterceptor the packet interceptor to notify of packets about to be sent.
     * @param packetFilter      the packet filter to use.
     */
    public void addPacketInterceptor(PacketInterceptor packetInterceptor, PacketFilter packetFilter);
 
    /**
     * Removes a packet interceptor.
     *
     * @param packetInterceptor the packet interceptor to remove.
     */
    public void removePacketInterceptor(PacketInterceptor packetInterceptor);

    /**
     * Retrieve the servers Entity Caps node
     * 
     * XMPPConnection holds this information in order to avoid a dependency to
     * smackx where EntityCapsManager lives from smack.
     * 
     * @return the servers entity caps node
     */
    public String getServiceCapsNode();

    /**
     * Returns true if the server supports roster versioning as defined in XEP-0237.
     *
     * @return true if the server supports roster versioning
     */
    public boolean isRosterVersioningSupported();

    /**
     * Returns the current value of the reply timeout in milliseconds for request for this
     * XMPPConnection instance.
     *
     * @return the packet reply timeout in milliseconds
     */
    public long getPacketReplyTimeout();

    /**
     * Set the packet reply timeout in milliseconds. In most cases, Smack will throw a
     * {@link NoResponseException} if no reply to a request was received within the timeout period.
     *
     * @param timeout the packet reply timeout in milliseconds
     */
    public void setPacketReplyTimeout(long timeout);

    /**
     * Get the connection counter of this XMPPConnection instance. Those can be used as ID to
     * identify the connection, but beware that the ID may not be unique if you create more then
     * <tt>2*Integer.MAX_VALUE</tt> instances as the counter could wrap.
     *
     * @return the connection counter of this XMPPConnection
     */
    public int getConnectionCounter();

    public static enum FromMode {
        /**
         * Leave the 'from' attribute unchanged. This is the behavior of Smack < 4.0
         */
        UNCHANGED,
        /**
         * Omit the 'from' attribute. According to RFC 6120 8.1.2.1 1. XMPP servers "MUST (...)
         * override the 'from' attribute specified by the client". It is therefore safe to specify
         * FromMode.OMITTED here.
         */
        OMITTED,
        /**
         * Set the from to the clients full JID. This is usually not required.
         */
        USER
    }

    /**
     * Set the FromMode for this connection instance. Defines how the 'from' attribute of outgoing
     * stanzas should be populated by Smack.
     * 
     * @param fromMode
     */
    public void setFromMode(FromMode fromMode);

    /**
     * Get the currently active FromMode.
     *
     * @return the currently active {@link FromMode}
     */
    public FromMode getFromMode();

    /**
     * Get the permanent roster store.
     * @return the permanent roster store or null
     */
    public RosterStore getRosterStore();

    /**
     * Returns true if the roster will be loaded from the server when logging in. This
     * is the common behaviour for clients but sometimes clients may want to differ this
     * or just never do it if not interested in rosters.
     *
     * @return true if the roster will be loaded from the server when logging in.
     */
    public boolean isRosterLoadedAtLogin();

    /**
     * Send a stanza and wait asynchronously for a response by using <code>replyFilter</code>.
     * <p>
     * If there is a response, then <code>callback</code> will be invoked. The callback will be
     * invoked at most once and it will be not invoked after the connections default reply timeout
     * has been elapsed.
     * </p>
     * 
     * @param stanza the stanza to send (required)
     * @param replyFilter the filter used to determine response stanza (required)
     * @param callback the callback invoked if there is a response (required)
     * @throws NotConnectedException
     */
    public void sendStanzaWithResponseCallback(Packet stanza, PacketFilter replyFilter,
                    PacketListener callback) throws NotConnectedException;

    /**
     * Send a stanza and wait asynchronously for a response by using <code>replyFilter</code>.
     * <p>
     * If there is a response, then <code>callback</code> will be invoked. If there is no response
     * after the connections default reply timeout, then <code>exceptionCallback</code> will be invoked
     * with a {@link SmackException.NoResponseException}. The callback will be invoked at most once.
     * </p>
     * 
     * @param stanza the stanza to send (required)
     * @param replyFilter the filter used to determine response stanza (required)
     * @param callback the callback invoked if there is a response (required)
     * @param exceptionCallback the callback invoked if there is an exception (optional)
     * @throws NotConnectedException
     */
    public void sendStanzaWithResponseCallback(Packet stanza, PacketFilter replyFilter, PacketListener callback,
                    ExceptionCallback exceptionCallback) throws NotConnectedException;

    /**
     * Send a stanza and wait asynchronously for a response by using <code>replyFilter</code>.
     * <p>
     * If there is a response, then <code>callback</code> will be invoked. If there is no response
     * after <code>timeout</code> milliseconds, then <code>exceptionCallback</code> will be invoked
     * with a {@link SmackException.NoResponseException}. The callback will be invoked at most once.
     * </p>
     * 
     * @param stanza the stanza to send (required)
     * @param replyFilter the filter used to determine response stanza (required)
     * @param callback the callback invoked if there is a response (required)
     * @param exceptionCallback the callback invoked if there is an exception (optional)
     * @param timeout the timeout in milliseconds to wait for a response
     * @throws NotConnectedException
     */
    public void sendStanzaWithResponseCallback(Packet stanza, PacketFilter replyFilter,
                    final PacketListener callback, final ExceptionCallback exceptionCallback,
                    long timeout) throws NotConnectedException;

    /**
     * Send a IQ stanza and invoke <code>callback</code> if there is a result of
     * {@link org.jivesoftware.smack.packet.IQ.Type#result} with that result IQ. The callback will
     * not be invoked after the connections default reply timeout has been elapsed.
     * 
     * @param iqRequest the IQ stanza to send (required)
     * @param callback the callback invoked if there is result response (required)
     * @throws NotConnectedException
     */
    public void sendIqWithResponseCallback(IQ iqRequest, PacketListener callback) throws NotConnectedException;

    /**
     * Send a IQ stanza and invoke <code>callback</code> if there is a result of
     * {@link org.jivesoftware.smack.packet.IQ.Type#result} with that result IQ. If there is an
     * error response <code>exceptionCallback</code> will be invoked, if not null, with the received
     * error as {@link XMPPException.XMPPErrorException}. If there is no response after the
     * connections default reply timeout, then <code>exceptionCallback</code> will be invoked with a
     * {@link SmackException.NoResponseException}.
     * 
     * @param iqRequest the IQ stanza to send (required)
     * @param callback the callback invoked if there is result response (required)
     * @param exceptionCallback the callback invoked if there is an Exception optional
     * @throws NotConnectedException
     */
    public void sendIqWithResponseCallback(IQ iqRequest, PacketListener callback,
                    ExceptionCallback exceptionCallback) throws NotConnectedException;

    /**
     * Send a IQ stanza and invoke <code>callback</code> if there is a result of
     * {@link org.jivesoftware.smack.packet.IQ.Type#result} with that result IQ. If there is an
     * error response <code>exceptionCallback</code> will be invoked, if not null, with the received
     * error as {@link XMPPException.XMPPErrorException}. If there is no response after
     * <code>timeout</code>, then <code>exceptionCallback</code> will be invoked with a
     * {@link SmackException.NoResponseException}.
     * 
     * @param iqRequest the IQ stanza to send (required)
     * @param callback the callback invoked if there is result response (required)
     * @param exceptionCallback the callback invoked if there is an Exception optional
     * @param timeout the timeout in milliseconds to wait for a response
     * @throws NotConnectedException
     */
    public void sendIqWithResponseCallback(IQ iqRequest, final PacketListener callback,
                    final ExceptionCallback exceptionCallback, long timeout)
                    throws NotConnectedException;
}
