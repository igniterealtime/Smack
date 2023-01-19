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

import java.util.concurrent.TimeUnit;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.OutgoingQueueFullException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaFactory;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.util.Consumer;
import org.jivesoftware.smack.util.Predicate;
import org.jivesoftware.smack.util.XmppElementUtil;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;

/**
 * The XMPPConnection interface provides an interface for connections from a client to an XMPP server and
 * implements shared methods which are used by the different types of connections (e.g.
 * {@link org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection} or <code>XMPPTCPConnection</code>). To create a connection to an XMPP server
 * a simple usage of this API might look like the following:
 *
 * <pre>{@code
 * // Create the configuration for this new connection
 * XMPPTCPConnectionConfiguration.Builder configBuilder = XMPPTCPConnectionConfiguration.builder();
 * configBuilder.setUsernameAndPassword("username", "password");
 * configBuilder.setXmppDomain("jabber.org");
 *
 * AbstractXMPPConnection connection = new XMPPTCPConnection(configBuilder.build());
 * connection.connect();
 * connection.login();
 *
 * Message message = connection.getStanzaFactory().buildMessageStanza()
 *     .to("mark@example.org)
 *     .setBody("Hi, how are you?")
 *     .build();
 * connection.sendStanza(message);
 *
 * connection.disconnect();
 * }</pre>
 * <p>
 * Note that the XMPPConnection interface does intentionally not declare any methods that manipulate
 * the connection state, e.g. <code>connect()</code>, <code>disconnect()</code>. You should use the
 * most-generic superclass connection type that is able to provide the methods you require. In most cases
 * this should be {@link AbstractXMPPConnection}. And use or hand out instances of the
 * XMPPConnection interface when you don't need to manipulate the connection state.
 * </p>
 * <p>
 * XMPPConnections can be reused between connections. This means that an Connection may be connected,
 * disconnected and then connected again. Listeners of the XMPPConnection will be retained across
 * connections.
 * </p>
 * <h2>Processing Incoming Stanzas</h2>
 * Smack provides a flexible framework for processing incoming stanzas using two constructs:
 * <ul>
 *  <li>{@link StanzaCollector}: lets you synchronously wait for new stanzas</li>
 *  <li>{@link StanzaListener}: an interface for asynchronously notifying you of incoming stanzas</li>
 * </ul>
 *
 * <h2>Incoming Stanza Listeners</h2>
 * Most callbacks (listeners, handlers, …) than you can add to a connection come in three different variants:
 * <ul>
 * <li>standard</li>
 * <li>async (asynchronous)</li>
 * <li>sync (synchronous)</li>
 * </ul>
 * <p>
 * Standard callbacks are invoked concurrently, but it is ensured that the same callback is never run concurrently.
 * The callback's identity is used as key for that. The events delivered to the callback preserve the order of the
 * causing events of the connection.
 * </p>
 * <p>
 * Asynchronous callbacks are run decoupled from the connections main event loop. Hence a callback triggered by
 * stanza B may (appear to) invoked before a callback triggered by stanza A, even though stanza A arrived before B.
 * </p>
 * <p>
 * Synchronous callbacks are run synchronous to the main event loop of a connection. Hence they are invoked in the
 * exact order of how events happen there, most importantly the arrival order of incoming stanzas. You should only
 * use synchronous callbacks in rare situations.
 * </p>
 * <h2>Stanza Filters</h2>
 * Stanza filters allow you to define the predicates for which listeners or collectors should be invoked. For more
 * information about stanza filters, see {@link org.jivesoftware.smack.filter}.
 * <h2>Provider Architecture</h2>
 * XMPP is an extensible protocol. Smack allows for this extensible with its provider architecture that allows to
 * plug-in providers that are able to parse the various XML extension elements used for XMPP's extensibility. For
 * more information see {@link org.jivesoftware.smack.provider}.
 * <h2>Debugging</h2>
 * See {@link org.jivesoftware.smack.debugger} for Smack's API to debug XMPP connections.
 * <h2>Modular Connection Architecture</h2>
 * Smack's new modular connection architecture will one day replace the monolithic architecture. Its main entry
 * point {@link org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection} has more information.
 *
 * @author Matt Tucker
 * @author Guenther Niess
 * @author Florian Schmaus
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
     * <code>null</code> if not logged in yet. An XMPP address is in the form
     * username@server/resource.
     *
     * @return the full XMPP address of the user logged in.
     */
    EntityFullJid getUser();

    /**
     * Returns the stream ID for this connection, which is the value set by the server
     * when opening an XMPP stream. This value will be <code>null</code> if not connected to the server.
     *
     * @return the ID of this connection returned from the XMPP server or <code>null</code> if
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

    StanzaFactory getStanzaFactory();

    /**
     * Sends the specified stanza to the server.
     *
     * @param stanza the stanza to send.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * */
    void sendStanza(Stanza stanza) throws NotConnectedException, InterruptedException;

    void sendStanzaNonBlocking(Stanza stanza) throws NotConnectedException, OutgoingQueueFullException;

    /**
     * Try to send the given stanza. Returns {@code true} if the stanza was successfully put into the outgoing stanza
     * queue, otherwise, if {@code false} is returned, the stanza could not be scheduled for sending (for example
     * because the outgoing element queue is full). Note that this means that the stanza possibly was not put onto the
     * wire, even if {@code true} is returned, it just has been successfully scheduled for sending.
     * <p>
     * <b>Note:</b> Implementations are not required to provide that functionality. In that case this method is mapped
     * to {@link #sendStanza(Stanza)} and will possibly block until the stanza could be scheduled for sending.
     * </p>
     *
     * @param stanza the stanza to send.
     * @return {@code true} if the stanza was successfully scheduled to be send, {@code false} otherwise.
     * @throws NotConnectedException if the connection is not connected.
     * @since 4.4.0
     * @deprecated use {@link #sendStanzaNonBlocking(Stanza)} instead.
     */
    // TODO: Remove in Smack 4.7.
    @Deprecated
    boolean trySendStanza(Stanza stanza) throws NotConnectedException;

    /**
     * Try to send the given stanza. Returns {@code true} if the stanza was successfully put into the outgoing stanza
     * queue within the given timeout period, otherwise, if {@code false} is returned, the stanza could not be scheduled
     * for sending (for example because the outgoing element queue is full). Note that this means that the stanza
     * possibly was not put onto the wire, even if {@code true} is returned, it just has been successfully scheduled for
     * sending.
     * <p>
     * <b>Note:</b> Implementations are not required to provide that functionality. In that case this method is mapped
     * to {@link #sendStanza(Stanza)} and will possibly block until the stanza could be scheduled for sending.
     * </p>
     *
     * @param stanza the stanza to send.
     * @param timeout how long to wait before giving up, in units of {@code unit}.
     * @param unit a {@code TimeUnit} determining how to interpret the {@code timeout} parameter.
     * @return {@code true} if the stanza was successfully scheduled to be send, {@code false} otherwise.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @since 4.4.0
     * @deprecated use {@link #sendStanzaNonBlocking(Stanza)} instead.
     */
    // TODO: Remove in Smack 4.7.
    @Deprecated
    boolean trySendStanza(Stanza stanza, long timeout, TimeUnit unit)  throws NotConnectedException, InterruptedException;

    /**
     * Send a Nonza.
     * <p>
     * <b>This method is not meant for end-user usage!</b> It allows sending plain stream elements, which should not be
     * done by a user manually. <b>Doing so may result in a unstable or unusable connection.</b> Certain Smack APIs use
     * this method to send plain stream elements.
     * </p>
     *
     * @param nonza the Nonza to send.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    void sendNonza(Nonza nonza) throws NotConnectedException, InterruptedException;

    void sendNonzaNonBlocking(Nonza stanza) throws NotConnectedException, OutgoingQueueFullException;

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
     * @param <I> the type of the expected result IQ.
     * @return an IQ with type 'result'
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if the XMPP connection is not connected.
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
     * Registers a stanza listener with this connection. The listener will be invoked when a (matching) incoming stanza
     * is received. The stanza filter determines which stanzas will be delivered to the listener. It is guaranteed that
     * the same listener will not be invoked concurrently and the order of invocation will reflect the order in
     * which the stanzas have been received. If the same stanza listener is added again with a different filter, only
     * the new filter will be used.
     *
     * @param stanzaListener the stanza listener to notify of new received stanzas.
     * @param stanzaFilter the stanza filter to use.
     * @since 4.4.0
     */
    void addStanzaListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter);

    /**
     * Removes a stanza listener for received stanzas from this connection.
     *
     * @param stanzaListener the stanza listener to remove.
     * @return true if the stanza listener was removed.
     * @since 4.4.0
     */
    boolean removeStanzaListener(StanzaListener stanzaListener);

    /**
     *  Registers a <b>synchronous</b> stanza listener with this connection. A stanza listener will be invoked only when
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
     */
    void addStanzaSendingListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter);

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
     * @deprecated use {@link #addMessageInterceptor(Consumer, Predicate)} or {@link #addPresenceInterceptor(Consumer, Predicate)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    void addStanzaInterceptor(StanzaListener stanzaInterceptor, StanzaFilter stanzaFilter);

    /**
     * Removes a stanza interceptor.
     *
     * @param stanzaInterceptor the stanza interceptor to remove.
     * @deprecated use {@link #removeMessageInterceptor(Consumer)} or {@link #removePresenceInterceptor(Consumer)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    void removeStanzaInterceptor(StanzaListener stanzaInterceptor);

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
     * @param messageInterceptor the stanza interceptor to notify of stanzas about to be sent.
     * @param messageFilter      the stanza filter to use.
     */
    void addMessageInterceptor(Consumer<MessageBuilder> messageInterceptor, Predicate<Message> messageFilter);

    /**
     * Removes a message interceptor.
     *
     * @param messageInterceptor the message interceptor to remove.
     */
    void removeMessageInterceptor(Consumer<MessageBuilder> messageInterceptor);

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
     * @param presenceInterceptor the stanza interceptor to notify of stanzas about to be sent.
     * @param presenceFilter      the stanza filter to use.
     */
    void addPresenceInterceptor(Consumer<PresenceBuilder> presenceInterceptor, Predicate<Presence> presenceFilter);

    /**
     * Removes a presence interceptor.
     *
     * @param presenceInterceptor the stanza interceptor to remove.
     */
    void removePresenceInterceptor(Consumer<PresenceBuilder> presenceInterceptor);
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
     * <code>2*Integer.MAX_VALUE</code> instances as the counter could wrap.
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
     * @param fromMode TODO javadoc me please
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
     * @param element TODO javadoc me please
     * @param namespace TODO javadoc me please
     * @return a stanza extensions of the feature or <code>null</code>
     * @deprecated use {@link #getFeature(Class)} instead.
     */
    // TODO: Remove in Smack 4.5.
    @Deprecated
    default <F extends XmlElement> F getFeature(String element, String namespace) {
        QName qname = new QName(namespace, element);
        return getFeature(qname);
    }

    /**
     * Get the feature stanza extensions for a given stream feature of the
     * server, or <code>null</code> if the server doesn't support that feature.
     *
     * @param <F> {@link ExtensionElement} type of the feature.
     * @param qname the qualified name of the XML element of feature.
     * @return a stanza extensions of the feature or <code>null</code>
     * @since 4.4
     */
    <F extends XmlElement> F getFeature(QName qname);

    /**
     * Get the feature stanza extensions for a given stream feature of the
     * server, or <code>null</code> if the server doesn't support that feature.
     *
     * @param <F> {@link ExtensionElement} type of the feature.
     * @param featureClass the class of the feature.
     * @return a stanza extensions of the feature or <code>null</code>
     * @since 4.4
     */
    default <F extends XmlElement> F getFeature(Class<F> featureClass) {
        QName qname = XmppElementUtil.getQNameFor(featureClass);
        return getFeature(qname);
    }

    /**
     * Return true if the server supports the given stream feature.
     *
     * @param element TODO javadoc me please
     * @param namespace TODO javadoc me please
     * @return true if the server supports the stream feature.
     */
    default boolean hasFeature(String element, String namespace) {
        QName qname = new QName(namespace, element);
        return hasFeature(qname);
    }

    /**
     * Return true if the server supports the given stream feature.
     *
     * @param qname the qualified name of the XML element of feature.
     * @return true if the server supports the stream feature.
     */
    boolean hasFeature(QName qname);

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
     * @param <S> the type of the stanza to send.
     * @return a SmackFuture for the response.
     */
    <S extends Stanza> SmackFuture<S, Exception> sendAsync(S stanza, StanzaFilter replyFilter);

    /**
     * Send a stanza asynchronously, waiting for exactly one response stanza using the given reply filter.
     *
     * @param stanza the stanza to send.
     * @param replyFilter the filter used for the response stanza.
     * @param timeout the reply timeout in milliseconds.
     * @param <S> the type of the stanza to send.
     * @return a SmackFuture for the response.
     */
    <S extends Stanza> SmackFuture<S, Exception> sendAsync(S stanza, StanzaFilter replyFilter, long timeout);

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
     * @param iqRequestHandler TODO javadoc me please
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
