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
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Stanza;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;

/**
 * The XMPPConnection interface provides an interface for connections to an XMPP server and
 * implements shared methods which are used by the different types of connections (e.g.
 * <code>XMPPTCPConnection</code> or <code>XMPPBOSHConnection</code>). To create a connection to an XMPP server
 * a simple usage of this API might look like the following:
 *
 * <pre>
 * // Create a connection to the igniterealtime.org XMPP server.
 * XMPPTCPConnection con = new XMPPTCPConnection("igniterealtime.org");
 * // Connect to the server
 * con.connect();
 * // Most servers require you to login before performing other tasks.
 * con.login("jsmith", "mypass");
 * // Start a new conversation with John Doe and send him a message.
 * ChatManager chatManager = ChatManager.getInstanceFor(con);
 * chatManager.addIncomingListener(new IncomingChatMessageListener() {
 *     public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
 *         // Print out any messages we get back to standard out.
 *         System.out.println("Received message: " + message);
 *     }
 * });
 * Chat chat = chatManager.chatWith("jdoe@igniterealtime.org");
 * chat.send("Howdy!");
 * // Disconnect from the server
 * con.disconnect();
 * </pre>
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
public interface XMPPConnection {

    /**
     * Returns the XMPP Domain of the service provided by the XMPP server and used for this connection. After
     * authenticating with the server the returned value may be different.
     *
     * @return the XMPP domain of this XMPP session.
     */
    DomainBareJid getXMPPServiceDomain();

    /**
     * Returns the host name of the server where the XMPP server is running. This would be the
     * IP address of the server or a name that may be resolved by a DNS server.
     *
     * @return the host name of the server where the XMPP server is running or null if not yet connected.
     */
    String getHost();

    /**
     * Returns the port number of the XMPP server for this connection. The default port
     * for normal connections is 5222.
     *
     * @return the port number of the XMPP server or 0 if not yet connected.
     */
    int getPort();

    /**
     * Returns the full XMPP address of the user that is logged in to the connection or
     * <tt>null</tt> if not logged in yet. An XMPP address is in the form
     * username@server/resource.
     *
     * @return the full XMPP address of the user logged in.
     */
    EntityFullJid getUser();

    /**
     * Returns the stream ID for this connection, which is the value set by the server
     * when opening an XMPP stream. This value will be <tt>null</tt> if not connected to the server.
     *
     * @return the ID of this connection returned from the XMPP server or <tt>null</tt> if
     *      not connected to the server.
     * @see <a href="http://xmpp.org/rfcs/rfc6120.html#streams-attr-id">RFC 6120 § 4.7.3. id</a>
     */
    String getStreamId();

    /**
     * Returns true if currently connected to the XMPP server.
     *
     * @return true if connected.
     */
    boolean isConnected();

    /**
     * Returns true if currently authenticated by successfully calling the login method.
     *
     * @return true if authenticated.
     */
    boolean isAuthenticated();

    /**
     * Returns true if currently authenticated anonymously.
     *
     * @return true if authenticated anonymously.
     */
    boolean isAnonymous();

    /**
     * Returns true if the connection to the server has successfully negotiated encryption.
     *
     * @return true if a secure connection to the server.
     */
    boolean isSecureConnection();

    /**
     * Returns true if network traffic is being compressed. When using stream compression network
     * traffic can be reduced up to 90%. Therefore, stream compression is ideal when using a slow
     * speed network connection. However, the server will need to use more CPU time in order to
     * un/compress network data so under high load the server performance might be affected.
     *
     * @return true if network traffic is being compressed.
     */
    boolean isUsingCompression();

    /**
     * Sends the specified stanza to the server.
     *
     * @param stanza the stanza to send.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException
     * */
    void sendStanza(Stanza stanza) throws NotConnectedException, InterruptedException;

    /**
     * Send a Nonza.
     * <p>
     * <b>This method is not meant for end-user usage!</b> It allows sending plain stream elements, which should not be
     * done by a user manually. <b>Doing so may result in a unstable or unusable connection.</b> Certain Smack APIs use
     * this method to send plain stream elements.
     * </p>
     *
     * @param nonza the Nonza to send.
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    void sendNonza(Nonza nonza) throws NotConnectedException, InterruptedException;

    /**
     * Adds a connection listener to this connection that will be notified when
     * the connection closes or fails.
     *
     * @param connectionListener a connection listener.
     */
    void addConnectionListener(ConnectionListener connectionListener);

    /**
     * Removes a connection listener from this connection.
     *
     * @param connectionListener a connection listener.
     */
    void removeConnectionListener(ConnectionListener connectionListener);

    /**
     * Send an IQ request and wait for the response.
     *
     * @param request the IQ request
     * @return an IQ with type 'result'
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @since 4.3
     */
    <I extends IQ> I sendIqRequestAndWaitForResponse(IQ request)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException;

    /**
     * Creates a new stanza collector collecting IQ responses that are replies to the IQ <code>request</code>.
     * Does also send the <code>request</code> IQ. The stanza filter for the collector is an
     * {@link IQReplyFilter}, guaranteeing that stanza id and JID in the 'from' address have
     * expected values.
     *
     * @param request the IQ request to filter responses from
     * @return a new stanza collector.
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    StanzaCollector createStanzaCollectorAndSend(IQ request) throws NotConnectedException, InterruptedException;

    /**
     * Creates a new stanza collector for this connection. A stanza filter determines
     * which stanzas will be accumulated by the collector. A StanzaCollector is
     * more suitable to use than a {@link StanzaListener} when you need to wait for
     * a specific result.
     *
     * @param stanzaFilter the stanza filter to use.
     * @param stanza the stanza to send right after the collector got created
     * @return a new stanza collector.
     * @throws InterruptedException
     * @throws NotConnectedException
     */
    StanzaCollector createStanzaCollectorAndSend(StanzaFilter stanzaFilter, Stanza stanza)
                    throws NotConnectedException, InterruptedException;

    /**
     * Creates a new stanza collector for this connection. A stanza filter
     * determines which stanzas will be accumulated by the collector. A
     * StanzaCollector is more suitable to use than a {@link StanzaListener}
     * when you need to wait for a specific result.
     * <p>
     * <b>Note:</b> If you send a Stanza right after using this method, then
     * consider using
     * {@link #createStanzaCollectorAndSend(StanzaFilter, Stanza)} instead.
     * Otherwise make sure cancel the StanzaCollector in every case, e.g. even
     * if an exception is thrown, or otherwise you may leak the StanzaCollector.
     * </p>
     *
     * @param stanzaFilter the stanza filter to use.
     * @return a new stanza collector.
     */
    StanzaCollector createStanzaCollector(StanzaFilter stanzaFilter);

    /**
     * Create a new stanza collector with the given stanza collector configuration.
     * <p>
     * Please make sure to cancel the collector when it is no longer required. See also
     * {@link #createStanzaCollector(StanzaFilter)}.
     * </p>
     *
     * @param configuration the stanza collector configuration.
     * @return a new stanza collector.
     * @since 4.1
     */
    StanzaCollector createStanzaCollector(StanzaCollector.Configuration configuration);

    /**
     * Remove a stanza collector of this connection.
     *
     * @param collector a stanza collectors which was created for this connection.
     */
    void removeStanzaCollector(StanzaCollector collector);

    /**
     * Registers a <b>synchronous</b> stanza listener with this connection. A stanza listener will be invoked only when
     * an incoming stanza is received. A stanza filter determines which stanzas will be delivered to the listener. If
     * the same stanza listener is added again with a different filter, only the new filter will be used.
     * <p>
     * <b>Important:</b> This stanza listeners will be called in the same <i>single</i> thread that processes all
     * incoming stanzas. Only use this kind of stanza filter if it does not perform any XMPP activity that waits for a
     * response. Consider using {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)} when possible, i.e. when
     * the invocation order doesn't have to be the same as the order of the arriving stanzas. If the order of the
     * arriving stanzas, consider using a {@link StanzaCollector} when possible.
     * </p>
     *
     * @param stanzaListener the stanza listener to notify of new received stanzas.
     * @param stanzaFilter the stanza filter to use.
     * @see #addStanzaInterceptor(StanzaListener, StanzaFilter)
     * @since 4.1
     */
    void addSyncStanzaListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter);

    /**
     * Removes a stanza listener for received stanzas from this connection.
     *
     * @param stanzaListener the stanza listener to remove.
     * @return true if the stanza listener was removed
     * @since 4.1
     */
    boolean removeSyncStanzaListener(StanzaListener stanzaListener);

    /**
     * Registers an <b>asynchronous</b> stanza listener with this connection. A stanza listener will be invoked only
     * when an incoming stanza is received. A stanza filter determines which stanzas will be delivered to the listener.
     * If the same stanza listener is added again with a different filter, only the new filter will be used.
     * <p>
     * Unlike {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)} stanza listeners added with this method will be
     * invoked asynchronously in their own thread. Use this method if the order of the stanza listeners must not depend
     * on the order how the stanzas where received.
     * </p>
     *
     * @param stanzaListener the stanza listener to notify of new received stanzas.
     * @param stanzaFilter the stanza filter to use.
     * @see #addStanzaInterceptor(StanzaListener, StanzaFilter)
     * @since 4.1
    */
    void addAsyncStanzaListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter);

    /**
     * Removes an <b>asynchronous</b> stanza listener for received stanzas from this connection.
     *
     * @param stanzaListener the stanza listener to remove.
     * @return true if the stanza listener was removed
     * @since 4.1
     */
    boolean removeAsyncStanzaListener(StanzaListener stanzaListener);

    /**
     * Registers a stanza listener with this connection. The listener will be
     * notified of every stanza that this connection sends. A stanza filter determines
     * which stanzas will be delivered to the listener. Note that the thread
     * that writes stanzas will be used to invoke the listeners. Therefore, each
     * stanza listener should complete all operations quickly or use a different
     * thread for processing.
     *
     * @param stanzaListener the stanza listener to notify of sent stanzas.
     * @param stanzaFilter   the stanza filter to use.
     * @deprecated use {@link #addStanzaSendingListener} instead
     */
    // TODO Remove in Smack 4.4
    @Deprecated
    void addPacketSendingListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter);

    /**
     * Registers a stanza listener with this connection. The listener will be
     * notified of every stanza that this connection sends. A stanza filter determines
     * which stanzas will be delivered to the listener. Note that the thread
     * that writes stanzas will be used to invoke the listeners. Therefore, each
     * stanza listener should complete all operations quickly or use a different
     * thread for processing.
     *
     * @param stanzaListener the stanza listener to notify of sent stanzas.
     * @param stanzaFilter   the stanza filter to use.
     */
    void addStanzaSendingListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter);

    /**
     * Removes a stanza listener for sending stanzas from this connection.
     *
     * @param stanzaListener the stanza listener to remove.
     * @deprecated use {@link #removeStanzaSendingListener} instead
     */
    // TODO Remove in Smack 4.4
    @Deprecated
    void removePacketSendingListener(StanzaListener stanzaListener);

    /**
     * Removes a stanza listener for sending stanzas from this connection.
     *
     * @param stanzaListener the stanza listener to remove.
     */
    void removeStanzaSendingListener(StanzaListener stanzaListener);

    /**
     * Registers a stanza interceptor with this connection. The interceptor will be
     * invoked every time a stanza is about to be sent by this connection. Interceptors
     * may modify the stanza to be sent. A stanza filter determines which stanzas
     * will be delivered to the interceptor.
     *
     * <p>
     * NOTE: For a similar functionality on incoming stanzas, see {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)}.
     * </p>
     *
     * @param stanzaInterceptor the stanza interceptor to notify of stanzas about to be sent.
     * @param stanzaFilter      the stanza filter to use.
     * @deprecated use {@link #addStanzaInterceptor} instead
     */
    // TODO Remove in Smack 4.4
    @Deprecated
    void addPacketInterceptor(StanzaListener stanzaInterceptor, StanzaFilter stanzaFilter);

    /**
     * Registers a stanza interceptor with this connection. The interceptor will be
     * invoked every time a stanza is about to be sent by this connection. Interceptors
     * may modify the stanza to be sent. A stanza filter determines which stanzas
     * will be delivered to the interceptor.
     *
     * <p>
     * NOTE: For a similar functionality on incoming stanzas, see {@link #addAsyncStanzaListener(StanzaListener, StanzaFilter)}.
     * </p>
     *
     * @param stanzaInterceptor the stanza interceptor to notify of stanzas about to be sent.
     * @param stanzaFilter      the stanza filter to use.
     */
    void addStanzaInterceptor(StanzaListener stanzaInterceptor, StanzaFilter stanzaFilter);

    /**
     * Removes a stanza interceptor.
     *
     * @param stanzaInterceptor the stanza interceptor to remove.
     * @deprecated user {@link #removeStanzaInterceptor} instead
     */
    // TODO Remove in Smack 4.4
    @Deprecated
    void removePacketInterceptor(StanzaListener stanzaInterceptor);

    /**
     * Removes a stanza interceptor.
     *
     * @param stanzaInterceptor the stanza interceptor to remove.
     */
    void removeStanzaInterceptor(StanzaListener stanzaInterceptor);

    /**
     * Returns the current value of the reply timeout in milliseconds for request for this
     * XMPPConnection instance.
     *
     * @return the reply timeout in milliseconds
     */
    long getReplyTimeout();

    /**
     * Set the stanza reply timeout in milliseconds. In most cases, Smack will throw a
     * {@link NoResponseException} if no reply to a request was received within the timeout period.
     *
     * @param timeout for a reply in milliseconds
     */
    void setReplyTimeout(long timeout);

    /**
     * Get the connection counter of this XMPPConnection instance. Those can be used as ID to
     * identify the connection, but beware that the ID may not be unique if you create more then
     * <tt>2*Integer.MAX_VALUE</tt> instances as the counter could wrap.
     *
     * @return the connection counter of this XMPPConnection
     */
    int getConnectionCounter();

    enum FromMode {
        /**
         * Leave the 'from' attribute unchanged. This is the behavior of Smack &lt; 4.0
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
    void setFromMode(FromMode fromMode);

    /**
     * Get the currently active FromMode.
     *
     * @return the currently active {@link FromMode}
     */
    FromMode getFromMode();

    /**
     * Get the feature stanza extensions for a given stream feature of the
     * server, or <code>null</code> if the server doesn't support that feature.
     *
     * @param <F> {@link ExtensionElement} type of the feature.
     * @param element
     * @param namespace
     * @return a stanza extensions of the feature or <code>null</code>
     */
    <F extends ExtensionElement> F getFeature(String element, String namespace);

    /**
     * Return true if the server supports the given stream feature.
     *
     * @param element
     * @param namespace
     * @return true if the server supports the stream feature.
     */
    boolean hasFeature(String element, String namespace);

    /**
     * Send an IQ request asynchronously. The connection's default reply timeout will be used.
     *
     * @param request the IQ request to send.
     * @return a SmackFuture for the response.
     */
    SmackFuture<IQ, Exception> sendIqRequestAsync(IQ request);

    /**
     * Send an IQ request asynchronously.
     *
     * @param request the IQ request to send.
     * @param timeout the reply timeout in milliseconds.
     * @return a SmackFuture for the response.
     */
    SmackFuture<IQ, Exception> sendIqRequestAsync(IQ request, long timeout);

    /**
     * Send a stanza asynchronously, waiting for exactly one response stanza using the given reply filter. The
     * connection's default reply timeout will be used.
     *
     * @param stanza the stanza to send.
     * @param replyFilter the filter used for the response stanza.
     * @return a SmackFuture for the response.
     */
    <S extends Stanza> SmackFuture<S, Exception> sendAsync(S stanza, StanzaFilter replyFilter);

    /**
     * Send a stanza asynchronously, waiting for exactly one response stanza using the given reply filter.
     *
     * @param stanza the stanza to send.
     * @param replyFilter the filter used for the response stanza.
     * @param timeout the reply timeout in milliseconds.
     * @return a SmackFuture for the response.
     */
    <S extends Stanza> SmackFuture<S, Exception> sendAsync(S stanza, StanzaFilter replyFilter, long timeout);

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
     * @throws InterruptedException
     * @deprecated use {@link #sendAsync(Stanza, StanzaFilter)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.4.
    void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter,
                    StanzaListener callback) throws NotConnectedException, InterruptedException;

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
     * @throws InterruptedException
     * @deprecated use {@link #sendAsync(Stanza, StanzaFilter)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.4.
    void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter, StanzaListener callback,
                    @SuppressWarnings("deprecation") ExceptionCallback exceptionCallback) throws NotConnectedException, InterruptedException;

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
     * @throws InterruptedException
     * @deprecated use {@link #sendAsync(Stanza, StanzaFilter, long)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.4.
    void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter,
                    StanzaListener callback, @SuppressWarnings("deprecation") ExceptionCallback exceptionCallback,
                    long timeout) throws NotConnectedException, InterruptedException;

    /**
     * Send a IQ stanza and invoke <code>callback</code> if there is a result of
     * {@link org.jivesoftware.smack.packet.IQ.Type#result} with that result IQ. The callback will
     * not be invoked after the connections default reply timeout has been elapsed.
     *
     * @param iqRequest the IQ stanza to send (required)
     * @param callback the callback invoked if there is result response (required)
     * @throws NotConnectedException
     * @throws InterruptedException
     * @deprecated use {@link #sendIqRequestAsync(IQ)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.4.
    void sendIqWithResponseCallback(IQ iqRequest, StanzaListener callback) throws NotConnectedException, InterruptedException;

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
     * @throws InterruptedException
     * @deprecated use {@link #sendIqRequestAsync(IQ)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.4.
    void sendIqWithResponseCallback(IQ iqRequest, StanzaListener callback,
                    @SuppressWarnings("deprecation") ExceptionCallback exceptionCallback) throws NotConnectedException, InterruptedException;

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
     * @throws InterruptedException
     * @deprecated use {@link #sendIqRequestAsync(IQ, long)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.4.
    void sendIqWithResponseCallback(IQ iqRequest, StanzaListener callback,
                    @SuppressWarnings("deprecation") ExceptionCallback exceptionCallback, long timeout)
                    throws NotConnectedException, InterruptedException;

    /**
     * Add a callback that is called exactly once and synchronously with the incoming stanza that matches the given
     * stanza filter.
     *
     * @param callback the callback invoked once the stanza filter matches a stanza.
     * @param stanzaFilter the filter to match stanzas or null to match all.
     */
    void addOneTimeSyncCallback(StanzaListener callback, StanzaFilter stanzaFilter);

    /**
     * Register an IQ request handler with this connection.
     * <p>
     * IQ request handler process incoming IQ requests, i.e. incoming IQ stanzas of type 'get' or 'set', and return a result.
     * </p>
     * @param iqRequestHandler the IQ request handler to register.
     * @return the previously registered IQ request handler or null.
     */
    IQRequestHandler registerIQRequestHandler(IQRequestHandler iqRequestHandler);

    /**
     * Convenience method for {@link #unregisterIQRequestHandler(String, String, org.jivesoftware.smack.packet.IQ.Type)}.
     *
     * @param iqRequestHandler
     * @return the previously registered IQ request handler or null.
     */
    IQRequestHandler unregisterIQRequestHandler(IQRequestHandler iqRequestHandler);

    /**
     * Unregister an IQ request handler with this connection.
     *
     * @param element the IQ element the IQ request handler is responsible for.
     * @param namespace the IQ namespace the IQ request handler is responsible for.
     * @param type the IQ type the IQ request handler is responsible for.
     * @return the previously registered IQ request handler or null.
     */
    IQRequestHandler unregisterIQRequestHandler(String element, String namespace, IQ.Type type);

    /**
     * Returns the timestamp in milliseconds when the last stanza was received.
     *
     * @return the timestamp in milliseconds
     */
    long getLastStanzaReceived();

}
