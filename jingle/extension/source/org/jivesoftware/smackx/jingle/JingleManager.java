/**
 * $RCSfile: JingleManager.java,v $
 * $Revision: 1.4 $
 * $Date: 2007/07/17 22:13:16 $
 *
 * Copyright 2003-2005 Jive Software.
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

package org.jivesoftware.smackx.jingle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.RosterListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.listeners.CreatedJingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionRequestListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.BasicTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.nat.TransportResolver;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.provider.JingleProvider;

/**
 * Jingle is a session establishment protocol defined in (XEP-0166).
 * It defines a framework for negotiating and managing out-of-band ( data that is send and receive through other connection than XMPP connection) data sessions over XMPP.
 * With this protocol you can setup VOIP Calls, Video Streaming, File transfers and whatever out-of-band session based transmission.
 * <p/>
 * To create a Jingle Session you need a Transport method and a Payload type.
 * <p/>
 * A transport method is how it will trasmit and receive network packets. Transport MUST have one or more candidates.
 * A transport candidate is an IP Address with a defined port, that other party must send data to.
 * <p/>
 * A supported payload type, is the data encoding format that the jmf will be transmitted.
 * For instance an Audio Payload "GSM".
 * <p/>
 * A Jingle session negociates a payload type and a pair of transport candidates.
 * Which means that when a Jingle Session is establhished you will have two defined transport candidates with addresses
 * and a defined Payload type.
 * In other words, you will have two IP address with their respective ports, and a Codec type defined.
 * <p/>
 * The JingleManager is a facade built upon Jabber Jingle (XEP-166) to allow the
 * use of Jingle. This implementation allows the user to simply
 * use this class for setting the Jingle parameters, create and receive Jingle Sessions.
 * <p/>
 * In order to use the Jingle, the user must provide a
 * TransportManager that will handle the resolution of potential IP addresses taht can be used to transport the streaming (jmf).
 * This TransportManager can be initialized with several default resolvers,
 * including a fixed solver that can be used when the address and port are know
 * in advance.
 * This API have ready to use Transport Managers, for instance: BasicTransportManager, STUNTransportManager, BridgedTransportManager.
 * <p/>
 * You should also especify a JingleMediaManager if you want that JingleManager assume Media control
 * Using a JingleMediaManager implementation is the easier way to implement a Jingle Application.
 * <p/>
 * Otherwise before creating an outgoing connection, the user must create jingle session
 * listeners that will be called when different events happen. The most
 * important event is <i>sessionEstablished()</i>, that will be called when all
 * the negotiations are finished, providing the payload type for the
 * transmission as well as the remote and local addresses and ports for the
 * communication. See JingleSessionListener for a complete list of events that can be
 * observed.
 * <p/>
 * This is an example of how to use the JingleManager:
 * <i>This example implements a Jingle VOIP Call between two users.</i>
 * <p/>
 * <pre>
 * <p/>
 *                               To wait for an Incoming Jingle Session:
 * <p/>
 *                               try {
 * <p/>
 *                                           // Connect to a XMPP Server
 *                                           Connection x1 = new XMPPConnection("xmpp.com");
 *                                           x1.connect();
 *                                           x1.login("juliet", "juliet");
 * <p/>
 *                                           // Create a JingleManager using a BasicResolver
 *                                           final JingleManager jm1 = new JingleManager(
 *                                                   x1, new BasicTransportManager());
 * <p/>
 *                                           // Create a JingleMediaManager. In this case using Jingle Audio Media API
 *                                           JingleMediaManager jingleMediaManager = new AudioMediaManager();
 * <p/>
 *                                           // Set the JingleMediaManager
 *                                           jm1.setMediaManager(jingleMediaManager);
 * <p/>
 *                                           // Listen for incoming calls
 *                                           jm1.addJingleSessionRequestListener(new JingleSessionRequestListener() {
 *                                               public void sessionRequested(JingleSessionRequest request) {
 * <p/>
 *                                                   try {
 *                                                      // Accept the call
 *                                                      IncomingJingleSession session = request.accept();
 * <p/>
 * <p/>
 *                                                       // Start the call
 *                                                       session.start();
 *                                                   } catch (XMPPException e) {
 *                                                       e.printStackTrace();
 *                                                   }
 * <p/>
 *                                               }
 *                                           });
 * <p/>
 *                                       Thread.sleep(15000);
 * <p/>
 *                                       } catch (Exception e) {
 *                                           e.printStackTrace();
 *                                       }
 * <p/>
 *                               To create an Outgoing Jingle Session:
 * <p/>
 *                                     try {
 * <p/>
 *                                           // Connect to a XMPP Server
 *                                           Connection x0 = new XMPPConnection("xmpp.com");
 *                                           x0.connect();
 *                                           x0.login("romeo", "romeo");
 * <p/>
 *                                           // Create a JingleManager using a BasicResolver
 *                                           final JingleManager jm0 = new JingleManager(
 *                                                   x0, new BasicTransportManager());
 * <p/>
 *                                           // Create a JingleMediaManager. In this case using Jingle Audio Media API
 *                                           JingleMediaManager jingleMediaManager = new AudioMediaManager(); // Using Jingle Media API
 * <p/>
 *                                           // Set the JingleMediaManager
 *                                           jm0.setMediaManager(jingleMediaManager);
 * <p/>
 *                                           // Create a new Jingle Call with a full JID
 *                                           OutgoingJingleSession js0 = jm0.createOutgoingJingleSession("juliet@xmpp.com/Smack");
 * <p/>
 *                                           // Start the call
 *                                           js0.start();
 * <p/>
 *                                           Thread.sleep(10000);
 *                                           js0.terminate();
 * <p/>
 *                                           Thread.sleep(3000);
 * <p/>
 *                                       } catch (Exception e) {
 *                                           e.printStackTrace();
 *                                       }
 *                               </pre>
 *
 * @author Thiago Camargo
 * @author Alvaro Saurin
 * @author Jeff Williams
 * @see JingleListener
 * @see TransportResolver
 * @see JingleSession
 * @see JingleSession
 * @see JingleMediaManager
 * @see BasicTransportManager , STUNTransportManager, BridgedTransportManager, TransportResolver, BridgedResolver, ICEResolver, STUNResolver and BasicResolver.
 */
public class JingleManager implements JingleSessionListener {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(JingleManager.class);
	
	// non-static

    final List<JingleSession> jingleSessions = new ArrayList<JingleSession>();

    // Listeners for manager events (ie, session requests...)
    private List<JingleSessionRequestListener> jingleSessionRequestListeners;

    // Listeners for created JingleSessions
    private List<CreatedJingleSessionListener> creationListeners = new ArrayList<CreatedJingleSessionListener>();

    // The XMPP connection
    private Connection connection;

    // The Media Managers
    private List<JingleMediaManager> jingleMediaManagers;

     /**
     * Default constructor with a defined Connection, Transport Resolver and a Media Manager
     * If a fully implemented JingleMediaSession is entered, JingleManager manage Jingle signalling and jmf
     *
     * @param connection             XMPP Connection to be used
     * @param jingleMediaManager     an implemeted JingleMediaManager to be used.
     */
    public JingleManager(Connection connection, List<JingleMediaManager> jingleMediaManagers) {
        this.connection = connection;
        this.jingleMediaManagers = jingleMediaManagers;

        connection.getRoster().addRosterListener(new RosterListener() {

            public void entriesAdded(Collection<String> addresses) {
            }

            public void entriesUpdated(Collection<String> addresses) {
            }

            public void entriesDeleted(Collection<String> addresses) {
            }

            public void presenceChanged(Presence presence) {
                if (!presence.isAvailable()) {
                    String xmppAddress = presence.getFrom();
                    JingleSession aux = null;
                    for (JingleSession jingleSession : jingleSessions) {
                        if (jingleSession.getInitiator().equals(xmppAddress) || jingleSession.getResponder().equals(xmppAddress)) {
                            aux = jingleSession;
                        }
                    }
                    if (aux != null)
                        try {
                            aux.terminate();
                        } catch (XMPPException e) {
                            e.printStackTrace();
                        }
                }
            }
        });

    }

    
    /**
     * Setup the jingle system to let the remote clients know we support Jingle.
     * (This used to be a static part of construction.  The problem is a remote client might
     * attempt a Jingle connection to us after we've created a Connection, but before we've
     * setup an instance of a JingleManager.  We will appear to not support Jingle.  With the new
     * method you just call it once and all new connections will report Jingle support.)
     */
    public static void setJingleServiceEnabled() {
        ProviderManager providerManager = ProviderManager.getInstance();
        providerManager.addIQProvider("jingle", "urn:xmpp:tmp:jingle", new JingleProvider());

        // Enable the Jingle support on every established connection
        // The ServiceDiscoveryManager class should have been already
        // initialized
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                JingleManager.setServiceEnabled(connection, true);
            }
        });
    }

    /**
     * Enables or disables the Jingle support on a given connection.
     * <p/>
     * <p/>
     * Before starting any Jingle jmf session, check that the user can handle
     * it. Enable the Jingle support to indicate that this client handles Jingle
     * messages.
     *
     * @param connection the connection where the service will be enabled or
     *                   disabled
     * @param enabled    indicates if the service will be enabled or disabled
     */
    public synchronized static void setServiceEnabled(Connection connection, boolean enabled) {
        if (isServiceEnabled(connection) == enabled) {
            return;
        }

        if (enabled) {
            ServiceDiscoveryManager.getInstanceFor(connection).addFeature(Jingle.NAMESPACE);
        } else {
            ServiceDiscoveryManager.getInstanceFor(connection).removeFeature(Jingle.NAMESPACE);
        }
    }

    /**
     * Returns true if the Jingle support is enabled for the given connection.
     *
     * @param connection the connection to look for Jingle support
     * @return a boolean indicating if the Jingle support is enabled for the
     *         given connection
     */
    public static boolean isServiceEnabled(Connection connection) {
        return ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(Jingle.NAMESPACE);
    }

    /**
     * Returns true if the specified user handles Jingle messages.
     *
     * @param connection the connection to use to perform the service discovery
     * @param userID     the user to check. A fully qualified xmpp ID, e.g.
     *                   jdoe@example.com
     * @return a boolean indicating whether the specified user handles Jingle
     *         messages
     */
    public static boolean isServiceEnabled(Connection connection, String userID) {
        try {
            DiscoverInfo result = ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(userID);
            return result.containsFeature(Jingle.NAMESPACE);
        } catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the Media Managers of this Jingle Manager
     *
     * @return
     */
    public List<JingleMediaManager> getMediaManagers() {
        return jingleMediaManagers;
    }

    /**
     * Set the Media Managers of this Jingle Manager
     *
     * @param jingleMediaManager JingleMediaManager to be used for open, close, start and stop jmf streamings
     */
    public void setMediaManagers(List<JingleMediaManager> jingleMediaManagers) {
        this.jingleMediaManagers = jingleMediaManagers;
    }

    /**
    * Add a Jingle session request listenerJingle to listen to incoming session
    * requests.
    *
    * @param jingleSessionRequestListener an implemented JingleSessionRequestListener
    * @see #removeJingleSessionRequestListener(JingleSessionRequestListener)
    * @see JingleListener
    */
    public synchronized void addJingleSessionRequestListener(final JingleSessionRequestListener jingleSessionRequestListener) {
        if (jingleSessionRequestListener != null) {
            if (jingleSessionRequestListeners == null) {
                initJingleSessionRequestListeners();
            }
            synchronized (jingleSessionRequestListeners) {
                jingleSessionRequestListeners.add(jingleSessionRequestListener);
            }
        }
    }

    /**
     * Removes a Jingle session listenerJingle.
     *
     * @param jingleSessionRequestListener The jingle session jingleSessionRequestListener to be removed
     * @see #addJingleSessionRequestListener(JingleSessionRequestListener)
     * @see JingleListener
     */
    public void removeJingleSessionRequestListener(JingleSessionRequestListener jingleSessionRequestListener) {
        if (jingleSessionRequestListeners == null) {
            return;
        }
        synchronized (jingleSessionRequestListeners) {
            jingleSessionRequestListeners.remove(jingleSessionRequestListener);
        }
    }

    /**
     * Adds a CreatedJingleSessionListener.
     * This listener will be called when a session is created by the JingleManager instance.
     *
     * @param createdJingleSessionListener
     */
    public void addCreationListener(CreatedJingleSessionListener createdJingleSessionListener) {
        this.creationListeners.add(createdJingleSessionListener);
    }

    /**
     * Removes a CreatedJingleSessionListener.
     * This listener will be called when a session is created by the JingleManager instance.
     *
     * @param createdJingleSessionListener
     */
    public void removeCreationListener(CreatedJingleSessionListener createdJingleSessionListener) {
        this.creationListeners.remove(createdJingleSessionListener);
    }

    /**
     * Trigger CreatedJingleSessionListeners that a session was created.
     *
     * @param jingleSession
     */
    public void triggerSessionCreated(JingleSession jingleSession) {
        jingleSessions.add(jingleSession);
        jingleSession.addListener(this);
        for (CreatedJingleSessionListener createdJingleSessionListener : creationListeners) {
            try {
                createdJingleSessionListener.sessionCreated(jingleSession);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
    }

    public void sessionDeclined(String reason, JingleSession jingleSession) {
        jingleSession.removeListener(this);
        jingleSessions.remove(jingleSession);
        jingleSession.close();
        LOGGER.error("Declined:" + reason);
    }

    public void sessionRedirected(String redirection, JingleSession jingleSession) {
        jingleSession.removeListener(this);
        jingleSessions.remove(jingleSession);
    }

    public void sessionClosed(String reason, JingleSession jingleSession) {
        jingleSession.removeListener(this);
        jingleSessions.remove(jingleSession);
    }

    public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
        jingleSession.removeListener(this);
        jingleSessions.remove(jingleSession);
    }

    public void sessionMediaReceived(JingleSession jingleSession, String participant) {
        // Do Nothing
    }

    /**
     * Register the listenerJingles, waiting for a Jingle packet that tries to
     * establish a new session.
     */
    private void initJingleSessionRequestListeners() {
        PacketFilter initRequestFilter = new PacketFilter() {
            // Return true if we accept this packet
            public boolean accept(Packet pin) {
                if (pin instanceof IQ) {
                    IQ iq = (IQ) pin;
                    if (iq.getType().equals(IQ.Type.SET)) {
                        if (iq instanceof Jingle) {
                            Jingle jin = (Jingle) pin;
                            if (jin.getAction().equals(JingleActionEnum.SESSION_INITIATE)) {
                                return true;
                            }
                        }
                    }
                }
                return false;
            }
        };

        jingleSessionRequestListeners = new ArrayList<JingleSessionRequestListener>();

        // Start a packet listener for session initiation requests
        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                triggerSessionRequested((Jingle) packet);
            }
        }, initRequestFilter);
    }

    /**
     * Disconnect all Jingle Sessions
     */
    public void disconnectAllSessions() {

        List<JingleSession> sessions = jingleSessions.subList(0, jingleSessions.size());

        for (JingleSession jingleSession : sessions)
            try {
                jingleSession.terminate();
            } catch (XMPPException e) {
                e.printStackTrace();
            }

        sessions.clear();
    }

    /**
     * Activates the listenerJingles on a Jingle session request.
     *
     * @param initJin the packet that must be passed to the jingleSessionRequestListener.
     */
    void triggerSessionRequested(Jingle initJin) {

        JingleSessionRequestListener[] jingleSessionRequestListeners = null;

        // Make a synchronized copy of the listenerJingles
        synchronized (this.jingleSessionRequestListeners) {
            jingleSessionRequestListeners = new JingleSessionRequestListener[this.jingleSessionRequestListeners.size()];
            this.jingleSessionRequestListeners.toArray(jingleSessionRequestListeners);
        }

        // ... and let them know of the event
        JingleSessionRequest request = new JingleSessionRequest(this, initJin);
        for (int i = 0; i < jingleSessionRequestListeners.length; i++) {
            jingleSessionRequestListeners[i].sessionRequested(request);
        }
    }

    // Session creation

    /**
     * Creates an Jingle session to start a communication with another user.
     *
     * @param responder    the fully qualified jabber ID with resource of the other
     *                     user.
     * @param payloadTypes list of supported payload types
     * @return The session on which the negotiation can be run.
     */
    public JingleSession createOutgoingJingleSession(String responder) throws XMPPException {

        if (responder == null || StringUtils.parseName(responder).length() <= 0 || StringUtils.parseServer(responder).length() <= 0
                || StringUtils.parseResource(responder).length() <= 0) {
            throw new IllegalArgumentException("The provided user id was not fully qualified");
        }

        JingleSession session = new JingleSession(connection, (JingleSessionRequest) null, connection.getUser(), responder, jingleMediaManagers);

        triggerSessionCreated(session);

        return session;
    }

    /**
     * Creates an Jingle session to start a communication with another user.
     *
     * @param responder the fully qualified jabber ID with resource of the other
     *                  user.
     * @return the session on which the negotiation can be run.
     */
    //    public OutgoingJingleSession createOutgoingJingleSession(String responder) throws XMPPException {
    //        if (this.getMediaManagers() == null) return null;
    //        return createOutgoingJingleSession(responder, this.getMediaManagers());
    //    }
    /**
     * When the session request is acceptable, this method should be invoked. It
     * will create an JingleSession which allows the negotiation to procede.
     *
     * @param request      the remote request that is being accepted.
     * @param payloadTypes the list of supported Payload types that can be accepted
     * @return the session which manages the rest of the negotiation.
     */
    public JingleSession createIncomingJingleSession(JingleSessionRequest request) throws XMPPException {
        if (request == null) {
            throw new NullPointerException("Received request cannot be null");
        }

        JingleSession session = new JingleSession(connection, request, request.getFrom(), connection.getUser(), jingleMediaManagers);

        triggerSessionCreated(session);

        return session;
    }

    /**
     * When the session request is acceptable, this method should be invoked. It
     * will create an JingleSession which allows the negotiation to procede.
     * This method use JingleMediaManager to select the supported Payload types.
     *
     * @param request the remote request that is being accepted.
     * @return the session which manages the rest of the negotiation.
     */
    //    IncomingJingleSession createIncomingJingleSession(JingleSessionRequest request) throws XMPPException {
    //        if (request == null) {
    //            throw new NullPointerException("JingleMediaManager is not defined");
    //        }
    //        if (jingleMediaManager != null)
    //            return createIncomingJingleSession(request, jingleMediaManager.getPayloads());
    //
    //        return createIncomingJingleSession(request, null);
    //    }
    /**
     * Get a session with the informed JID. If no session is found, return null.
     *
     * @param jid
     * @return
     */
    public JingleSession getSession(String jid) {
        for (JingleSession jingleSession : jingleSessions) {
            if (jingleSession.getResponder().equals(jid)) {
                return jingleSession;
            }
        }
        return null;
    }
}