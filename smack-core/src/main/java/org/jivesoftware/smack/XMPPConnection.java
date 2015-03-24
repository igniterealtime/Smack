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
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.PlainStreamElement;

/**
 * The XMPPConnection interface provides an interface for connections to an XMPP server and
 * implements shared methods which are used by the different types of connections (e.g.
 * {@link XMPPTCPConnection} or {@link XMPPBOSHConnection}). To create a connection to an XMPP server
 * a simple usage of this API might look like the following:
 * <p>
 * 
 * <pre>
 * // Create a connection to the igniterealtime.org XMPP server.
 * XMPPTCPConnection con = new XMPPTCPConnection("igniterealtime.org");
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
 * </p>
 * <p>
 * Note that the XMPPConnection interface does intentionally not declare any methods that manipulate
 * the connection state, e.g. <code>connect()</code>, <code>disconnect()</code>. You should use the
 * most specific connection type, e.g. <code>XMPPTCPConnection</code> as declared type and use the
 * XMPPConnection interface when you don't need to manipulate the connection state.
 * </p>
 * <p>
 * XMPPConnections can be reused between connections. This means that an Connection may be connected,
 * disconnected and then connected again. Listeners of the XMPPConnection will be retained across
 * connections.
 * </p>
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
     * Returns the stream ID for this connection, which is the value set by the server
     * when opening an XMPP stream. This value will be <tt>null</tt> if not connected to the server.
     * 
     * @return the ID of this connection returned from the XMPP server or <tt>null</tt> if
     *      not connected to the server.
     * @see <a href="http://xmpp.org/rfcs/rfc6120.html#streams-attr-id">RFC 6120 § 4.7.3. id</a>
     */
    public String getStreamId();

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
     * Sends the specified stanza(/packet) to the server.
     * 
     * @param packet the stanza(/packet) to send.
     * @throws NotConnectedException 
     * @deprecated use {@link #sendStanza(Stanza)} instead.
     */
    @Deprecated
    public void sendPacket(Stanza packet) throws NotConnectedException;

    /**
     * Sends the specified stanza to the server.
     *
     * @param stanza the stanza to send.
     * @throws NotConnectedException if the connection is not connected.
     */
    public void sendStanza(Stanza stanza) throws NotConnectedException;

    /**
     * Send a PlainStreamElement.
     * <p>
     * <b>This method is not meant for end-user usage!</b> It allows sending plain stream elements, which should not be
     * done by a user manually. <b>Doing so may result in a unstable or unusable connection.</b> Certain Smack APIs use
     * this method to send plain stream elements.
     * </p>
     *
     * @param element
     * @throws NotConnectedException
     */
    public void send(PlainStreamElement element) throws NotConnectedException;

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
     * Creates a new stanza(/packet) collector collecting packets that are replies to <code>packet</code>.
     * Does also send <code>packet</code>. The stanza(/packet) filter for the collector is an
     * {@link IQReplyFilter}, guaranteeing that stanza(/packet) id and JID in the 'from' address have
     * expected values.
     *
     * @param packet the stanza(/packet) to filter responses from
     * @return a new stanza(/packet) collector.
     * @throws NotConnectedException 
     */
    public PacketCollector createPacketCollectorAndSend(IQ packet) throws NotConnectedException;

    /**
     * Creates a new stanza(/packet) collector for this connection. A stanza(/packet) filter determines
     * which packets will be accumulated by the collector. A PacketCollector is
     * more suitable to use than a {@link StanzaListener} when you need to wait for
     * a specific result.
     * 
     * @param packetFilter the stanza(/packet) filter to use.
     * @param packet the stanza(/packet) to send right after the collector got created
     * @return a new stanza(/packet) collector.
     */
    public PacketCollector createPacketCollectorAndSend(StanzaFilter packetFilter, Stanza packet)
                    throws NotConnectedException;

    /**
     * Creates a new stanza(/packet) collector for this connection. A stanza(/packet) filter
     * determines which packets will be accumulated by the collector. A
     * PacketCollector is more suitable to use than a {@link StanzaListener}
     * when you need to wait for a specific result.
     * <p>
     * <b>Note:</b> If you send a Stanza(/Packet) right after using this method, then
     * consider using
     * {@link #createPacketCollectorAndSend(StanzaFilter, Stanza)} instead.
     * Otherwise make sure cancel the PacketCollector in every case, e.g. even
     * if an exception is thrown, or otherwise you may leak the PacketCollector.
     * </p>
     * 
     * @param packetFilter the stanza(/packet) filter to use.
     * @return a new stanza(/packet) collector.
     */
    public PacketCollector createPacketCollector(StanzaFilter packetFilter);

    /**
     * Create a new stanza(/packet) collector with the given stanza(/packet) collector configuration.
     * <p>
     * Please make sure to cancel the collector when it is no longer required. See also
     * {@link #createPacketCollector(StanzaFilter)}.
     * </p>
     * 
     * @param configuration the stanza(/packet) collector configuration.
     * @return a new stanza(/packet) collector.
     * @since 4.1
     */
    public PacketCollector createPacketCollector(PacketCollector.Configuration configuration);

    /**
     * Remove a stanza(/packet) collector of this connection.
     * 
     * @param collector a stanza(/packet) collectors which was created for this connection.
     */
    public void removePacketCollector(PacketCollector collector);

    /**
     * Registers a stanza(/packet) listener with this connection.
     * <p>
     * This method has been deprecated. It is important to differentiate between using an asynchronous stanza(/packet) listener
     * (preferred where possible) and a synchronous stanza(/packet) lister. Refer
     * {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)} and
     * {@link #addSyncStanzaListener(StanzaListener, StanzaFilter)} for more information.
     * </p>
     *
     * @param packetListener the stanza(/packet) listener to notify of new received packets.
     * @param packetFilter the stanza(/packet) filter to use.
     * @deprecated use {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)} or
     *             {@link #addSyncStanzaListener(StanzaListener, StanzaFilter)}.
     */
    @Deprecated
    public void addPacketListener(StanzaListener packetListener, StanzaFilter packetFilter);

    /**
     * Removes a stanza(/packet) listener for received packets from this connection.
     * 
     * @param packetListener the stanza(/packet) listener to remove.
     * @return true if the stanza(/packet) listener was removed
     * @deprecated use {@link #removeAsyncStanzaListener(StanzaListener)} or {@link #removeSyncStanzaListener(StanzaListener)}.
     */
    @Deprecated
    public boolean removePacketListener(StanzaListener packetListener);

    /**
     * Registers a <b>synchronous</b> stanza(/packet) listener with this connection. A stanza(/packet) listener will be invoked only when
     * an incoming stanza(/packet) is received. A stanza(/packet) filter determines which packets will be delivered to the listener. If
     * the same stanza(/packet) listener is added again with a different filter, only the new filter will be used.
     * <p>
     * <b>Important:</b> This stanza(/packet) listeners will be called in the same <i>single</i> thread that processes all
     * incoming stanzas. Only use this kind of stanza(/packet) filter if it does not perform any XMPP activity that waits for a
     * response. Consider using {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)} when possible, i.e. when
     * the invocation order doesn't have to be the same as the order of the arriving packets. If the order of the
     * arriving packets, consider using a {@link PacketCollector} when possible.
     * </p>
     *
     * @param packetListener the stanza(/packet) listener to notify of new received packets.
     * @param packetFilter the stanza(/packet) filter to use.
     * @see #addPacketInterceptor(StanzaListener, StanzaFilter)
     * @since 4.1
     */
    public void addSyncStanzaListener(StanzaListener packetListener, StanzaFilter packetFilter);

    /**
     * Removes a stanza(/packet) listener for received packets from this connection.
     *
     * @param packetListener the stanza(/packet) listener to remove.
     * @return true if the stanza(/packet) listener was removed
     * @since 4.1
     */
    public boolean removeSyncStanzaListener(StanzaListener packetListener);

    /**
     * Registers an <b>asynchronous</b> stanza(/packet) listener with this connection. A stanza(/packet) listener will be invoked only
     * when an incoming stanza(/packet) is received. A stanza(/packet) filter determines which packets will be delivered to the listener.
     * If the same stanza(/packet) listener is added again with a different filter, only the new filter will be used.
     * <p>
     * Unlike {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)} stanza(/packet) listeners added with this method will be
     * invoked asynchronously in their own thread. Use this method if the order of the stanza(/packet) listeners must not depend
     * on the order how the stanzas where received.
     * </p>
     * 
     * @param packetListener the stanza(/packet) listener to notify of new received packets.
     * @param packetFilter the stanza(/packet) filter to use.
     * @see #addPacketInterceptor(StanzaListener, StanzaFilter)
     * @since 4.1
    */
    public void addAsyncStanzaListener(StanzaListener packetListener, StanzaFilter packetFilter);

    /**
     * Removes an <b>asynchronous</b> stanza(/packet) listener for received packets from this connection.
     * 
     * @param packetListener the stanza(/packet) listener to remove.
     * @return true if the stanza(/packet) listener was removed
     * @since 4.1
     */
    public boolean removeAsyncStanzaListener(StanzaListener packetListener);

    /**
     * Registers a stanza(/packet) listener with this connection. The listener will be
     * notified of every stanza(/packet) that this connection sends. A stanza(/packet) filter determines
     * which packets will be delivered to the listener. Note that the thread
     * that writes packets will be used to invoke the listeners. Therefore, each
     * stanza(/packet) listener should complete all operations quickly or use a different
     * thread for processing.
     * 
     * @param packetListener the stanza(/packet) listener to notify of sent packets.
     * @param packetFilter   the stanza(/packet) filter to use.
     */
    public void addPacketSendingListener(StanzaListener packetListener, StanzaFilter packetFilter);

    /**
     * Removes a stanza(/packet) listener for sending packets from this connection.
     * 
     * @param packetListener the stanza(/packet) listener to remove.
     */
    public void removePacketSendingListener(StanzaListener packetListener);

    /**
     * Registers a stanza(/packet) interceptor with this connection. The interceptor will be
     * invoked every time a stanza(/packet) is about to be sent by this connection. Interceptors
     * may modify the stanza(/packet) to be sent. A stanza(/packet) filter determines which packets
     * will be delivered to the interceptor.
     * 
     * <p>
     * NOTE: For a similar functionality on incoming packets, see {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)}.
     *
     * @param packetInterceptor the stanza(/packet) interceptor to notify of packets about to be sent.
     * @param packetFilter      the stanza(/packet) filter to use.
     */
    public void addPacketInterceptor(StanzaListener packetInterceptor, StanzaFilter packetFilter);
 
    /**
     * Removes a stanza(/packet) interceptor.
     *
     * @param packetInterceptor the stanza(/packet) interceptor to remove.
     */
    public void removePacketInterceptor(StanzaListener packetInterceptor);

    /**
     * Returns the current value of the reply timeout in milliseconds for request for this
     * XMPPConnection instance.
     *
     * @return the stanza(/packet) reply timeout in milliseconds
     */
    public long getPacketReplyTimeout();

    /**
     * Set the stanza(/packet) reply timeout in milliseconds. In most cases, Smack will throw a
     * {@link NoResponseException} if no reply to a request was received within the timeout period.
     *
     * @param timeout the stanza(/packet) reply timeout in milliseconds
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
     * Get the feature stanza(/packet) extensions for a given stream feature of the
     * server, or <code>null</code> if the server doesn't support that feature.
     * 
     * @param element
     * @param namespace
     * @return a stanza(/packet) extensions of the feature or <code>null</code>
     */
    public <F extends ExtensionElement> F getFeature(String element, String namespace);

    /**
     * Return true if the server supports the given stream feature.
     * 
     * @param element
     * @param namespace
     * @return true if the server supports the stream feature.
     */
    public boolean hasFeature(String element, String namespace);

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
    public void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter,
                    StanzaListener callback) throws NotConnectedException;

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
    public void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter, StanzaListener callback,
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
    public void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter,
                    final StanzaListener callback, final ExceptionCallback exceptionCallback,
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
    public void sendIqWithResponseCallback(IQ iqRequest, StanzaListener callback) throws NotConnectedException;

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
    public void sendIqWithResponseCallback(IQ iqRequest, StanzaListener callback,
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
    public void sendIqWithResponseCallback(IQ iqRequest, final StanzaListener callback,
                    final ExceptionCallback exceptionCallback, long timeout)
                    throws NotConnectedException;

    /**
     * Add a callback that is called exactly once and synchronously with the incoming stanza that matches the given
     * stanza(/packet) filter.
     * 
     * @param callback the callback invoked once the stanza(/packet) filter matches a stanza.
     * @param packetFilter the filter to match stanzas or null to match all.
     */
    public void addOneTimeSyncCallback(StanzaListener callback, StanzaFilter packetFilter);

    /**
     * Register an IQ request handler with this connection.
     * <p>
     * IQ request handler process incoming IQ requests, i.e. incoming IQ stanzas of type 'get' or 'set', and return a result.
     * </p>
     * @param iqRequestHandler the IQ request handler to register.
     * @return the previously registered IQ request handler or null.
     */
    public IQRequestHandler registerIQRequestHandler(IQRequestHandler iqRequestHandler);

    /**
     * Convenience method for {@link #unregisterIQRequestHandler(String, String, org.jivesoftware.smack.packet.IQ.Type)}.
     *
     * @param iqRequestHandler
     * @return the previously registered IQ request handler or null.
     */
    public IQRequestHandler unregisterIQRequestHandler(IQRequestHandler iqRequestHandler);

    /**
     * Unregister an IQ request handler with this connection.
     * 
     * @param element the IQ element the IQ request handler is responsible for.
     * @param namespace the IQ namespace the IQ request handler is responsible for.
     * @param type the IQ type the IQ request handler is responsible for.
     * @return the previously registered IQ request handler or null.
     */
    public IQRequestHandler unregisterIQRequestHandler(String element, String namespace, IQ.Type type);

    /**
     * Returns the timestamp in milliseconds when the last stanza was received.
     * 
     * @return the timestamp in milliseconds
     */
    public long getLastStanzaReceived();

}
