/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

import org.jivesoftware.smack.*;
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
import org.jivesoftware.smackx.jingle.nat.BasicResolver;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.nat.TransportResolver;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.Jingle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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
 *                                           XMPPConnection x1 = new XMPPConnection("xmpp.com");
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
 *                                           XMPPConnection x0 = new XMPPConnection("xmpp.com");
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
 * @see JingleListener
 * @see TransportResolver
 * @see org.jivesoftware.smackx.jingle.nat.JingleTransportManager
 * @see OutgoingJingleSession
 * @see IncomingJingleSession
 * @see JingleMediaManager
 * @see org.jivesoftware.smackx.jingle.nat.BasicTransportManager , STUNTransportManager, BridgedTransportManager, TransportResolver, BridgedResolver, ICEResolver, STUNResolver and BasicResolver.
 */
public class JingleManager implements JingleSessionListener {

    // non-static

    final List<JingleSession> jingleSessions = new ArrayList<JingleSession>();

    // Listeners for manager events (ie, session requests...)
    private List<JingleSessionRequestListener> jingleSessionRequestListeners;

    // Listeners for created JingleSessions
    private List<CreatedJingleSessionListener> creationListeners = new ArrayList<CreatedJingleSessionListener>();

    // The XMPP connection
    private XMPPConnection connection;

    // The Media Manager
    private JingleMediaManager jingleMediaManager;

    // The Jingle transport manager
    private final JingleTransportManager jingleTransportManager;

    static {

        ProviderManager providerManager = ProviderManager.getInstance();

        providerManager.addIQProvider("jingle", "http://jabber.org/protocol/jingle",
                new org.jivesoftware.smackx.provider.JingleProvider());

        providerManager.addExtensionProvider("description", "http://jabber.org/protocol/jingle/description/audio",
                new org.jivesoftware.smackx.provider.JingleContentDescriptionProvider.Audio());

        providerManager.addExtensionProvider("description", "http://jabber.org/protocol/jingle/description/audio",
                new org.jivesoftware.smackx.provider.JingleContentDescriptionProvider.Audio());

        providerManager.addExtensionProvider("transport", "http://jabber.org/protocol/jingle/transport/ice",
                new org.jivesoftware.smackx.provider.JingleTransportProvider.Ice());
        providerManager.addExtensionProvider("transport", "http://jabber.org/protocol/jingle/transport/raw-udp",
                new org.jivesoftware.smackx.provider.JingleTransportProvider.RawUdp());

        providerManager.addExtensionProvider("busy", "http://jabber.org/protocol/jingle/info/audio",
                new org.jivesoftware.smackx.provider.JingleContentInfoProvider.Audio.Busy());
        providerManager.addExtensionProvider("hold", "http://jabber.org/protocol/jingle/info/audio",
                new org.jivesoftware.smackx.provider.JingleContentInfoProvider.Audio.Hold());
        providerManager.addExtensionProvider("mute", "http://jabber.org/protocol/jingle/info/audio",
                new org.jivesoftware.smackx.provider.JingleContentInfoProvider.Audio.Mute());
        providerManager.addExtensionProvider("queued", "http://jabber.org/protocol/jingle/info/audio",
                new org.jivesoftware.smackx.provider.JingleContentInfoProvider.Audio.Queued());
        providerManager.addExtensionProvider("ringing", "http://jabber.org/protocol/jingle/info/audio",
                new org.jivesoftware.smackx.provider.JingleContentInfoProvider.Audio.Ringing());

        // Enable the Jingle support on every established connection
        // The ServiceDiscoveryManager class should have been already
        // initialized
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                JingleManager.setServiceEnabled(connection, true);
            }
        });
    }

    /**
     * Default constructor with a defined XMPPConnection, Transport Resolver and a Media Manager
     * If a fully implemented JingleMediaSession is entered, JingleManager manage Jingle signalling and jmf
     *
     * @param connection             XMPP Connection to be used
     * @param jingleTransportManager transport resolver to be used
     * @param jingleMediaManager     an implemeted JingleMediaManager to be used.
     */
    public JingleManager(XMPPConnection connection, JingleTransportManager jingleTransportManager, JingleMediaManager jingleMediaManager) {
        this.connection = connection;
        this.jingleTransportManager = jingleTransportManager;
        this.jingleMediaManager = jingleMediaManager;

        connection.getRoster().addRosterListener(new RosterListener() {

            public void entriesAdded(Collection addresses) {
            }

            public void entriesUpdated(Collection addresses) {
            }

            public void entriesDeleted(Collection addresses) {
            }

            public void presenceChanged(Presence presence) {
                String xmppAddress = presence.getFrom();
                JingleSession aux = null;
                for (JingleSession jingleSession : jingleSessions) {
                    if (jingleSession.getInitiator().equals(xmppAddress) ||
                            jingleSession.getResponder().equals(xmppAddress))
                    {
                        aux = jingleSession;
                    }
                }
                if (aux != null)
                    try {
                        aux.terminate();
                    }
                    catch (XMPPException e) {
                        e.printStackTrace();
                    }
            }
        });

    }

    /**
     * Default constructor with a defined XMPPConnection and a Transport Resolver
     *
     * @param connection             XMPP Connection to be used
     * @param jingleTransportManager transport resolver to be used
     */
    public JingleManager(XMPPConnection connection, JingleTransportManager jingleTransportManager) {
        this(connection, jingleTransportManager, null);
    }

    /**
     * Default constructor with a defined XMPPConnection.
     * A default JingleTransportmanager based on BasicResolver will be used in this JingleManager transport.
     *
     * @param connection XMPP Connection to be used
     */
    public JingleManager(XMPPConnection connection) {
        this(connection, new JingleTransportManager() {
            protected TransportResolver createResolver() {
                return new BasicResolver();
            }
        });
    }

    /**
     * Default constructor with a defined XMPPConnection and a defined Resolver.
     * A default JingleTransportmanager based on BasicResolver will be used in this JingleManager transport.
     *
     * @param connection XMPP Connection to be used
     */
    public JingleManager(XMPPConnection connection, final TransportResolver resolver) {
        this(connection, new JingleTransportManager() {
            protected TransportResolver createResolver() {
                return resolver;
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
    public synchronized static void setServiceEnabled(XMPPConnection connection,
            boolean enabled) {
        if (isServiceEnabled(connection) == enabled) {
            return;
        }

        if (enabled) {
            ServiceDiscoveryManager.getInstanceFor(connection).addFeature(
                    Jingle.NAMESPACE);
        }
        else {
            ServiceDiscoveryManager.getInstanceFor(connection).removeFeature(
                    Jingle.NAMESPACE);
        }
    }

    /**
     * Returns true if the Jingle support is enabled for the given connection.
     *
     * @param connection the connection to look for Jingle support
     * @return a boolean indicating if the Jingle support is enabled for the
     *         given connection
     */
    public static boolean isServiceEnabled(XMPPConnection connection) {
        return ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(
                Jingle.NAMESPACE);
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
    public static boolean isServiceEnabled(XMPPConnection connection, String userID) {
        try {
            DiscoverInfo result = ServiceDiscoveryManager.getInstanceFor(connection)
                    .discoverInfo(userID);
            return result.containsFeature(Jingle.NAMESPACE);
        }
        catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Get the JingleTransportManager of this JingleManager
     *
     * @return
     */
    public JingleTransportManager getJingleTransportManager() {
        return jingleTransportManager;
    }

    /**
     * Get the Media Manager of this Jingle Manager
     *
     * @return
     */
    public JingleMediaManager getMediaManager() {
        return jingleMediaManager;
    }

    /**
     * Set the Media Manager of this Jingle Manager
     *
     * @param jingleMediaManager JingleMediaManager to be used for open, close, start and stop jmf streamings
     */
    public void setMediaManager(JingleMediaManager jingleMediaManager) {
        this.jingleMediaManager = jingleMediaManager;
    }

    /**
     * Add a Jingle session request listenerJingle to listen to incoming session
     * requests.
     *
     * @param jingleSessionRequestListener an implemented JingleSessionRequestListener
     * @see #removeJingleSessionRequestListener(JingleSessionRequestListener)
     * @see JingleListener
     */
    public synchronized void addJingleSessionRequestListener(
            final JingleSessionRequestListener jingleSessionRequestListener) {
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
            }
            catch (Exception e) {
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
        System.err.println("Declined");
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
                            if (jin.getAction().equals(Jingle.Action.SESSIONINITIATE)) {
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
            }
            catch (XMPPException e) {
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
    public OutgoingJingleSession createOutgoingJingleSession(String responder,
            List<PayloadType> payloadTypes) throws XMPPException {

        if (responder == null || StringUtils.parseName(responder).length() <= 0
                || StringUtils.parseServer(responder).length() <= 0
                || StringUtils.parseResource(responder).length() <= 0) {
            throw new IllegalArgumentException(
                    "The provided user id was not fully qualified");
        }

        OutgoingJingleSession session;

        TransportResolver resolver = jingleTransportManager.getResolver();

        if (jingleMediaManager != null)
            session = new OutgoingJingleSession(connection, responder, payloadTypes, resolver, jingleMediaManager);
        else
            session = new OutgoingJingleSession(connection, responder, payloadTypes, jingleTransportManager.getResolver());

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
    public OutgoingJingleSession createOutgoingJingleSession(String responder) throws XMPPException {
        if (this.getMediaManager() == null) return null;
        return createOutgoingJingleSession(responder, this.getMediaManager().getPayloads());
    }

    /**
     * When the session request is acceptable, this method should be invoked. It
     * will create an JingleSession which allows the negotiation to procede.
     *
     * @param request      the remote request that is being accepted.
     * @param payloadTypes the list of supported Payload types that can be accepted
     * @return the session which manages the rest of the negotiation.
     */
    IncomingJingleSession createIncomingJingleSession(
            JingleSessionRequest request, List<PayloadType> payloadTypes) throws XMPPException {
        if (request == null) {
            throw new NullPointerException("Received request cannot be null");
        }

        IncomingJingleSession session;

        TransportResolver resolver = jingleTransportManager.getResolver();

        if (jingleMediaManager != null)
            session = new IncomingJingleSession(connection, request
                    .getFrom(), payloadTypes, resolver, jingleMediaManager);
        else
            session = new IncomingJingleSession(connection, request
                    .getFrom(), payloadTypes, resolver);

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
    IncomingJingleSession createIncomingJingleSession(JingleSessionRequest request) throws XMPPException {
        if (request == null) {
            throw new NullPointerException("JingleMediaManager is not defined");
        }
        if (jingleMediaManager != null)
            return createIncomingJingleSession(request, jingleMediaManager.getPayloads());

        return createIncomingJingleSession(request,null);
    }

    /**
     * Get a session with the informed JID. If no session is found, return null.
     *
     * @param jid
     * @return
     */
    public JingleSession getSession(String jid) {
        for (JingleSession jingleSession : jingleSessions) {
            if (jingleSession instanceof OutgoingJingleSession) {
                if (jingleSession.getResponder().equals(jid)) {
                    return jingleSession;
                }
            }
            else if (jingleSession instanceof IncomingJingleSession) {
                if (jingleSession.getInitiator().equals(jid)) {
                    return jingleSession;
                }
            }
        }
        return null;
    }

    /**
     * Reject the session. If we don't want to accept the new session, send an
     * appropriate error packet.
     *
     * @param request the request to be rejected.
     */
    protected void rejectIncomingJingleSession(JingleSessionRequest request) {
        Jingle initiation = request.getJingle();

        IQ rejection = JingleSession.createError(initiation.getPacketID(), initiation
                .getFrom(), initiation.getTo(), 403, "Declined");
        connection.sendPacket(rejection);
    }
}