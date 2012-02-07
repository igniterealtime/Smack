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
package org.jivesoftware.smackx.bytestreams.ibb;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.SyncPacketSend;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;

/**
 * The InBandBytestreamManager class handles establishing In-Band Bytestreams as specified in the <a
 * href="http://xmpp.org/extensions/xep-0047.html">XEP-0047</a>.
 * <p>
 * The In-Band Bytestreams (IBB) enables two entities to establish a virtual bytestream over which
 * they can exchange Base64-encoded chunks of data over XMPP itself. It is the fall-back mechanism
 * in case the Socks5 bytestream method of transferring data is not available.
 * <p>
 * There are two ways to send data over an In-Band Bytestream. It could either use IQ stanzas to
 * send data packets or message stanzas. If IQ stanzas are used every data packet is acknowledged by
 * the receiver. This is the recommended way to avoid possible rate-limiting penalties. Message
 * stanzas are not acknowledged because most XMPP server implementation don't support stanza
 * flow-control method like <a href="http://xmpp.org/extensions/xep-0079.html">Advanced Message
 * Processing</a>. To set the stanza that should be used invoke {@link #setStanza(StanzaType)}.
 * <p>
 * To establish an In-Band Bytestream invoke the {@link #establishSession(String)} method. This will
 * negotiate an in-band bytestream with the given target JID and return a session.
 * <p>
 * If a session ID for the In-Band Bytestream was already negotiated (e.g. while negotiating a file
 * transfer) invoke {@link #establishSession(String, String)}.
 * <p>
 * To handle incoming In-Band Bytestream requests add an {@link InBandBytestreamListener} to the
 * manager. There are two ways to add this listener. If you want to be informed about incoming
 * In-Band Bytestreams from a specific user add the listener by invoking
 * {@link #addIncomingBytestreamListener(BytestreamListener, String)}. If the listener should
 * respond to all In-Band Bytestream requests invoke
 * {@link #addIncomingBytestreamListener(BytestreamListener)}.
 * <p>
 * Note that the registered {@link InBandBytestreamListener} will NOT be notified on incoming
 * In-Band bytestream requests sent in the context of <a
 * href="http://xmpp.org/extensions/xep-0096.html">XEP-0096</a> file transfer. (See
 * {@link FileTransferManager})
 * <p>
 * If no {@link InBandBytestreamListener}s are registered, all incoming In-Band bytestream requests
 * will be rejected by returning a &lt;not-acceptable/&gt; error to the initiator.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamManager implements BytestreamManager {

    /**
     * Stanzas that can be used to encapsulate In-Band Bytestream data packets.
     */
    public enum StanzaType {

        /**
         * IQ stanza.
         */
        IQ,

        /**
         * Message stanza.
         */
        MESSAGE
    }

    /*
     * create a new InBandBytestreamManager and register its shutdown listener on every established
     * connection
     */
    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                final InBandBytestreamManager manager;
                manager = InBandBytestreamManager.getByteStreamManager(connection);

                // register shutdown listener
                connection.addConnectionListener(new AbstractConnectionListener() {

                    public void connectionClosed() {
                        manager.disableService();
                    }

                });

            }
        });
    }

    /**
     * The XMPP namespace of the In-Band Bytestream
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/ibb";

    /**
     * Maximum block size that is allowed for In-Band Bytestreams
     */
    public static final int MAXIMUM_BLOCK_SIZE = 65535;

    /* prefix used to generate session IDs */
    private static final String SESSION_ID_PREFIX = "jibb_";

    /* random generator to create session IDs */
    private final static Random randomGenerator = new Random();

    /* stores one InBandBytestreamManager for each XMPP connection */
    private final static Map<Connection, InBandBytestreamManager> managers = new HashMap<Connection, InBandBytestreamManager>();

    /* XMPP connection */
    private final Connection connection;

    /*
     * assigns a user to a listener that is informed if an In-Band Bytestream request for this user
     * is received
     */
    private final Map<String, BytestreamListener> userListeners = new ConcurrentHashMap<String, BytestreamListener>();

    /*
     * list of listeners that respond to all In-Band Bytestream requests if there are no user
     * specific listeners for that request
     */
    private final List<BytestreamListener> allRequestListeners = Collections.synchronizedList(new LinkedList<BytestreamListener>());

    /* listener that handles all incoming In-Band Bytestream requests */
    private final InitiationListener initiationListener;

    /* listener that handles all incoming In-Band Bytestream IQ data packets */
    private final DataListener dataListener;

    /* listener that handles all incoming In-Band Bytestream close requests */
    private final CloseListener closeListener;

    /* assigns a session ID to the In-Band Bytestream session */
    private final Map<String, InBandBytestreamSession> sessions = new ConcurrentHashMap<String, InBandBytestreamSession>();

    /* block size used for new In-Band Bytestreams */
    private int defaultBlockSize = 4096;

    /* maximum block size allowed for this connection */
    private int maximumBlockSize = MAXIMUM_BLOCK_SIZE;

    /* the stanza used to send data packets */
    private StanzaType stanza = StanzaType.IQ;

    /*
     * list containing session IDs of In-Band Bytestream open packets that should be ignored by the
     * InitiationListener
     */
    private List<String> ignoredBytestreamRequests = Collections.synchronizedList(new LinkedList<String>());

    /**
     * Returns the InBandBytestreamManager to handle In-Band Bytestreams for a given
     * {@link Connection}.
     * 
     * @param connection the XMPP connection
     * @return the InBandBytestreamManager for the given XMPP connection
     */
    public static synchronized InBandBytestreamManager getByteStreamManager(Connection connection) {
        if (connection == null)
            return null;
        InBandBytestreamManager manager = managers.get(connection);
        if (manager == null) {
            manager = new InBandBytestreamManager(connection);
            managers.put(connection, manager);
        }
        return manager;
    }

    /**
     * Constructor.
     * 
     * @param connection the XMPP connection
     */
    private InBandBytestreamManager(Connection connection) {
        this.connection = connection;

        // register bytestream open packet listener
        this.initiationListener = new InitiationListener(this);
        this.connection.addPacketListener(this.initiationListener,
                        this.initiationListener.getFilter());

        // register bytestream data packet listener
        this.dataListener = new DataListener(this);
        this.connection.addPacketListener(this.dataListener, this.dataListener.getFilter());

        // register bytestream close packet listener
        this.closeListener = new CloseListener(this);
        this.connection.addPacketListener(this.closeListener, this.closeListener.getFilter());

    }

    /**
     * Adds InBandBytestreamListener that is called for every incoming in-band bytestream request
     * unless there is a user specific InBandBytestreamListener registered.
     * <p>
     * If no listeners are registered all In-Band Bytestream request are rejected with a
     * &lt;not-acceptable/&gt; error.
     * <p>
     * Note that the registered {@link InBandBytestreamListener} will NOT be notified on incoming
     * Socks5 bytestream requests sent in the context of <a
     * href="http://xmpp.org/extensions/xep-0096.html">XEP-0096</a> file transfer. (See
     * {@link FileTransferManager})
     * 
     * @param listener the listener to register
     */
    public void addIncomingBytestreamListener(BytestreamListener listener) {
        this.allRequestListeners.add(listener);
    }

    /**
     * Removes the given listener from the list of listeners for all incoming In-Band Bytestream
     * requests.
     * 
     * @param listener the listener to remove
     */
    public void removeIncomingBytestreamListener(BytestreamListener listener) {
        this.allRequestListeners.remove(listener);
    }

    /**
     * Adds InBandBytestreamListener that is called for every incoming in-band bytestream request
     * from the given user.
     * <p>
     * Use this method if you are awaiting an incoming Socks5 bytestream request from a specific
     * user.
     * <p>
     * If no listeners are registered all In-Band Bytestream request are rejected with a
     * &lt;not-acceptable/&gt; error.
     * <p>
     * Note that the registered {@link InBandBytestreamListener} will NOT be notified on incoming
     * Socks5 bytestream requests sent in the context of <a
     * href="http://xmpp.org/extensions/xep-0096.html">XEP-0096</a> file transfer. (See
     * {@link FileTransferManager})
     * 
     * @param listener the listener to register
     * @param initiatorJID the JID of the user that wants to establish an In-Band Bytestream
     */
    public void addIncomingBytestreamListener(BytestreamListener listener, String initiatorJID) {
        this.userListeners.put(initiatorJID, listener);
    }

    /**
     * Removes the listener for the given user.
     * 
     * @param initiatorJID the JID of the user the listener should be removed
     */
    public void removeIncomingBytestreamListener(String initiatorJID) {
        this.userListeners.remove(initiatorJID);
    }

    /**
     * Use this method to ignore the next incoming In-Band Bytestream request containing the given
     * session ID. No listeners will be notified for this request and and no error will be returned
     * to the initiator.
     * <p>
     * This method should be used if you are awaiting an In-Band Bytestream request as a reply to
     * another packet (e.g. file transfer).
     * 
     * @param sessionID to be ignored
     */
    public void ignoreBytestreamRequestOnce(String sessionID) {
        this.ignoredBytestreamRequests.add(sessionID);
    }

    /**
     * Returns the default block size that is used for all outgoing in-band bytestreams for this
     * connection.
     * <p>
     * The recommended default block size is 4096 bytes. See <a
     * href="http://xmpp.org/extensions/xep-0047.html#usage">XEP-0047</a> Section 5.
     * 
     * @return the default block size
     */
    public int getDefaultBlockSize() {
        return defaultBlockSize;
    }

    /**
     * Sets the default block size that is used for all outgoing in-band bytestreams for this
     * connection.
     * <p>
     * The default block size must be between 1 and 65535 bytes. The recommended default block size
     * is 4096 bytes. See <a href="http://xmpp.org/extensions/xep-0047.html#usage">XEP-0047</a>
     * Section 5.
     * 
     * @param defaultBlockSize the default block size to set
     */
    public void setDefaultBlockSize(int defaultBlockSize) {
        if (defaultBlockSize <= 0 || defaultBlockSize > MAXIMUM_BLOCK_SIZE) {
            throw new IllegalArgumentException("Default block size must be between 1 and "
                            + MAXIMUM_BLOCK_SIZE);
        }
        this.defaultBlockSize = defaultBlockSize;
    }

    /**
     * Returns the maximum block size that is allowed for In-Band Bytestreams for this connection.
     * <p>
     * Incoming In-Band Bytestream open request will be rejected with an
     * &lt;resource-constraint/&gt; error if the block size is greater then the maximum allowed
     * block size.
     * <p>
     * The default maximum block size is 65535 bytes.
     * 
     * @return the maximum block size
     */
    public int getMaximumBlockSize() {
        return maximumBlockSize;
    }

    /**
     * Sets the maximum block size that is allowed for In-Band Bytestreams for this connection.
     * <p>
     * The maximum block size must be between 1 and 65535 bytes.
     * <p>
     * Incoming In-Band Bytestream open request will be rejected with an
     * &lt;resource-constraint/&gt; error if the block size is greater then the maximum allowed
     * block size.
     * 
     * @param maximumBlockSize the maximum block size to set
     */
    public void setMaximumBlockSize(int maximumBlockSize) {
        if (maximumBlockSize <= 0 || maximumBlockSize > MAXIMUM_BLOCK_SIZE) {
            throw new IllegalArgumentException("Maximum block size must be between 1 and "
                            + MAXIMUM_BLOCK_SIZE);
        }
        this.maximumBlockSize = maximumBlockSize;
    }

    /**
     * Returns the stanza used to send data packets.
     * <p>
     * Default is {@link StanzaType#IQ}. See <a
     * href="http://xmpp.org/extensions/xep-0047.html#message">XEP-0047</a> Section 4.
     * 
     * @return the stanza used to send data packets
     */
    public StanzaType getStanza() {
        return stanza;
    }

    /**
     * Sets the stanza used to send data packets.
     * <p>
     * The use of {@link StanzaType#IQ} is recommended. See <a
     * href="http://xmpp.org/extensions/xep-0047.html#message">XEP-0047</a> Section 4.
     * 
     * @param stanza the stanza to set
     */
    public void setStanza(StanzaType stanza) {
        this.stanza = stanza;
    }

    /**
     * Establishes an In-Band Bytestream with the given user and returns the session to send/receive
     * data to/from the user.
     * <p>
     * Use this method to establish In-Band Bytestreams to users accepting all incoming In-Band
     * Bytestream requests since this method doesn't provide a way to tell the user something about
     * the data to be sent.
     * <p>
     * To establish an In-Band Bytestream after negotiation the kind of data to be sent (e.g. file
     * transfer) use {@link #establishSession(String, String)}.
     * 
     * @param targetJID the JID of the user an In-Band Bytestream should be established
     * @return the session to send/receive data to/from the user
     * @throws XMPPException if the user doesn't support or accept in-band bytestreams, or if the
     *         user prefers smaller block sizes
     */
    public InBandBytestreamSession establishSession(String targetJID) throws XMPPException {
        String sessionID = getNextSessionID();
        return establishSession(targetJID, sessionID);
    }

    /**
     * Establishes an In-Band Bytestream with the given user using the given session ID and returns
     * the session to send/receive data to/from the user.
     * 
     * @param targetJID the JID of the user an In-Band Bytestream should be established
     * @param sessionID the session ID for the In-Band Bytestream request
     * @return the session to send/receive data to/from the user
     * @throws XMPPException if the user doesn't support or accept in-band bytestreams, or if the
     *         user prefers smaller block sizes
     */
    public InBandBytestreamSession establishSession(String targetJID, String sessionID)
                    throws XMPPException {
        Open byteStreamRequest = new Open(sessionID, this.defaultBlockSize, this.stanza);
        byteStreamRequest.setTo(targetJID);

        // sending packet will throw exception on timeout or error reply
        SyncPacketSend.getReply(this.connection, byteStreamRequest);

        InBandBytestreamSession inBandBytestreamSession = new InBandBytestreamSession(
                        this.connection, byteStreamRequest, targetJID);
        this.sessions.put(sessionID, inBandBytestreamSession);

        return inBandBytestreamSession;
    }

    /**
     * Responses to the given IQ packet's sender with an XMPP error that an In-Band Bytestream is
     * not accepted.
     * 
     * @param request IQ packet that should be answered with a not-acceptable error
     */
    protected void replyRejectPacket(IQ request) {
        XMPPError xmppError = new XMPPError(XMPPError.Condition.no_acceptable);
        IQ error = IQ.createErrorResponse(request, xmppError);
        this.connection.sendPacket(error);
    }

    /**
     * Responses to the given IQ packet's sender with an XMPP error that an In-Band Bytestream open
     * request is rejected because its block size is greater than the maximum allowed block size.
     * 
     * @param request IQ packet that should be answered with a resource-constraint error
     */
    protected void replyResourceConstraintPacket(IQ request) {
        XMPPError xmppError = new XMPPError(XMPPError.Condition.resource_constraint);
        IQ error = IQ.createErrorResponse(request, xmppError);
        this.connection.sendPacket(error);
    }

    /**
     * Responses to the given IQ packet's sender with an XMPP error that an In-Band Bytestream
     * session could not be found.
     * 
     * @param request IQ packet that should be answered with a item-not-found error
     */
    protected void replyItemNotFoundPacket(IQ request) {
        XMPPError xmppError = new XMPPError(XMPPError.Condition.item_not_found);
        IQ error = IQ.createErrorResponse(request, xmppError);
        this.connection.sendPacket(error);
    }

    /**
     * Returns a new unique session ID.
     * 
     * @return a new unique session ID
     */
    private String getNextSessionID() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(SESSION_ID_PREFIX);
        buffer.append(Math.abs(randomGenerator.nextLong()));
        return buffer.toString();
    }

    /**
     * Returns the XMPP connection.
     * 
     * @return the XMPP connection
     */
    protected Connection getConnection() {
        return this.connection;
    }

    /**
     * Returns the {@link InBandBytestreamListener} that should be informed if a In-Band Bytestream
     * request from the given initiator JID is received.
     * 
     * @param initiator the initiator's JID
     * @return the listener
     */
    protected BytestreamListener getUserListener(String initiator) {
        return this.userListeners.get(initiator);
    }

    /**
     * Returns a list of {@link InBandBytestreamListener} that are informed if there are no
     * listeners for a specific initiator.
     * 
     * @return list of listeners
     */
    protected List<BytestreamListener> getAllRequestListeners() {
        return this.allRequestListeners;
    }

    /**
     * Returns the sessions map.
     * 
     * @return the sessions map
     */
    protected Map<String, InBandBytestreamSession> getSessions() {
        return sessions;
    }

    /**
     * Returns the list of session IDs that should be ignored by the InitialtionListener
     * 
     * @return list of session IDs
     */
    protected List<String> getIgnoredBytestreamRequests() {
        return ignoredBytestreamRequests;
    }

    /**
     * Disables the InBandBytestreamManager by removing its packet listeners and resetting its
     * internal status.
     */
    private void disableService() {

        // remove manager from static managers map
        managers.remove(connection);

        // remove all listeners registered by this manager
        this.connection.removePacketListener(this.initiationListener);
        this.connection.removePacketListener(this.dataListener);
        this.connection.removePacketListener(this.closeListener);

        // shutdown threads
        this.initiationListener.shutdown();

        // reset internal status
        this.userListeners.clear();
        this.allRequestListeners.clear();
        this.sessions.clear();
        this.ignoredBytestreamRequests.clear();

    }

}
