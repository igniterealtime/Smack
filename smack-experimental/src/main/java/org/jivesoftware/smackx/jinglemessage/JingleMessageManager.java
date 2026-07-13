/*
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jinglemessage;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.chat2.ChatManager;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromTypeFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle_rtp.element.RtpDescription;
import org.jivesoftware.smackx.jingle_rtp.provider.JingleRTPDescriptionProvider;
import org.jivesoftware.smackx.jinglemessage.element.JingleMessage;
import org.jivesoftware.smackx.jinglemessage.provider.JingleMessageProvider;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

/**
 * A JingleMessage manager in media call setup.
 *
 * @author Eng Chong Meng
 */
public final class JingleMessageManager extends Manager {
    private static final Logger LOGGER = Logger.getLogger(JingleMessageManager.class.getName());

    /**
     * The value of {@link JingleMessage#NAMESPACE}.
     */
    public static final String NAMESPACE = JingleMessage.NAMESPACE;

    private static final Map<XMPPConnection, JingleMessageManager> INSTANCES = new WeakHashMap<>();

    private final Set<JingleMessageListener> jingleMessageListeners = new CopyOnWriteArraySet<>();

    private final AsyncButOrdered<Chat> asyncButOrdered = new AsyncButOrdered<>();

    /**
     * Message filter to listen for message sent from DomainJid i.e.
     * server with normal or has xmlElement <code>JingleMessage</code>
     */
    private static final StanzaFilter MESSAGE_FILTER = new AndFilter(
            MessageTypeFilter.NORMAL_OR_CHAT, new StanzaExtensionFilter(JingleMessage.NAMESPACE));

    private static final StanzaFilter INCOMING_JINGLE_MESSAGE_FILTER
            = new AndFilter(MESSAGE_FILTER, FromTypeFilter.ENTITY_FULL_JID);

    public static synchronized JingleMessageManager getInstanceFor(XMPPConnection connection) {
        JingleMessageManager jingleMessageManager = INSTANCES.get(connection);

        if (jingleMessageManager == null) {
            jingleMessageManager = new JingleMessageManager(connection);
            INSTANCES.put(connection, jingleMessageManager);
        }
        return jingleMessageManager;
    }

    private JingleMessageManager(final XMPPConnection connection) {
        super(connection);

        connection.addSyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) {
                final Message message = (Message) stanza;
                final JingleMessage jingleMessage;
                StandardExtensionElement extElement = (StandardExtensionElement) message.getExtension(JingleMessage.NAMESPACE);
                try {
                    jingleMessage = JingleMessageProvider.parse(extElement);
                }
                catch (XmlPullParserException | IOException | SmackParsingException e) {
                    throw new RuntimeException(e);
                }

                // Ignore any delayed Jingle Messages.
                if (stanza.hasExtension(DelayInformation.ELEMENT, DelayInformation.NAMESPACE)) {
                    LOGGER.log(Level.WARNING, "Ignore delayed JingleMessage received: " + jingleMessage.getAction());
                    return;
                }

                final EntityFullJid fullFrom = message.getFrom().asEntityFullJidIfPossible();
                final EntityBareJid bareFrom = fullFrom.asEntityBareJid();
                final Chat chat = ChatManager.getInstanceFor(connection()).chatWith(bareFrom);

                asyncButOrdered.performAsyncButOrdered(chat, new Runnable() {
                    @Override
                    public void run() {
                        for (JingleMessageListener listener : jingleMessageListeners) {
                            listener.handleJmSession(jingleMessage, message);
                        }
                    }
                });

            }
        }, INCOMING_JINGLE_MESSAGE_FILTER);

        ProviderManager.addExtensionProvider(RtpDescription.ELEMENT, RtpDescription.NAMESPACE, new JingleRTPDescriptionProvider());
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        serviceDiscoveryManager.addFeature(NAMESPACE);
    }

    /**
     * Check if any of the contact jid resources support JingleMessage.
     *
     * @param jid the contact to check with.
     *
     * @return true if any of the contact resources support JM.
     */
    public boolean contactSupportsJingleMessage(Jid jid) {
        Roster roster = Roster.getInstanceFor(connection());
        List<Presence> presences = roster.getAvailablePresences(jid.asBareJid());
        for (Presence presence : presences) {
            DiscoverInfo featureInfo = EntityCapsManager.getDiscoverInfoByUser(presence.getFrom());
            if (featureInfo != null && featureInfo.containsFeature(NAMESPACE)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a new listener for incoming jingle messages.
     *
     * @param listener the listener to add.
     */
    public void addIncomingListener(JingleMessageListener listener) {
        jingleMessageListeners.add(listener);
    }

    /**
     * Remove an incoming jingle message listener.
     *
     * @param listener the listener to remove.
     */
    public void removeIncomingListener(JingleMessageListener listener) {
        jingleMessageListeners.remove(listener);
    }

    // ==================== Outgoing Call processes ==================== //

    /**
     * Prepare and send the Jingle Message propose.
     *
     * @param recipient to which the call propose is sent
     * @param sid JingleMessage call UUID
     * @param rtpDescription RtpDescription xmlElement
     */
    public void sendJingleMessagePropose(BareJid recipient, String sid, List<XmlElement> rtpDescription) {
        JingleMessage msgPropose = new JingleMessage(JingleMessage.ACTION_PROPOSE, sid);
        msgPropose.addElements(rtpDescription);
        sendJingleMessage(recipient, msgPropose);
    }

    /**
     * Prepare and send Jingle Message Retract to the recipient callee, when call is retracted by initiator.
     *
     * @param recipient to which the call retract is sent
     * @param sid JingleMessage call UUID
     * @param reason JingleReason element
     * @param element may contain a TieBreakElement
     */
    public void sendJingleMessageRetract(Jid recipient, String sid, JingleReason reason, NamedElement element) {
        JingleMessage msgRetract = new JingleMessage(JingleMessage.ACTION_RETRACT, sid, reason, element);
        sendJingleMessage(recipient, msgRetract);
    }

    // ==================== Incoming Call processes ==================== //

    /**
     * Prepare Jingle Message Ringing and send it to the recipient callee on call received.
     *
     * @param recipient the callee FullJid
     * @param sid JingleMessage call UUID
     */
    public void sendJingleMessageRinging(FullJid recipient, String sid) {
        JingleMessage msgRinging = new JingleMessage(JingleMessage.ACTION_RINGING, sid);
        sendJingleMessage(recipient, msgRinging);
    }

    /**
     * Responder has accepted the call and send JM proceed to:
     * a. Responder's: all other resources to end call.
     * b. Initiator to initial the call.
     *
     * @param recipient the JM Proceed recipient: Initiator(FullJid) or Responder(BareJid)
     * @param sid JingleMessage call UUID
     */
    public void sendJingleMessageProceed(Jid recipient, String sid) {
        JingleMessage msgProceed = new JingleMessage(JingleMessage.ACTION_PROCEED, sid);
        sendJingleMessage(recipient, msgProceed);
    }

    /**
     * Responder has rejected the call; prepare Jingle Message reject and send it to:
     * a. Responder's: all other resources to end call.
     * b. Initiator to end call.
     *
     * @param recipient the JM Reject recipient: Initiator(FullJid) or Responder(BareJid)
     * @param sid the intended Jingle Message call id
     * @param reason the JingleReason xmlElement to be included
     * @param element may contain a NamedElement
     */
    public void sendJingleMessageReject(Jid recipient, String sid, JingleReason reason, NamedElement element) {
        JingleMessage msgReject = new JingleMessage(JingleMessage.ACTION_REJECT, sid, reason, element);
        sendJingleMessage(recipient, msgReject);
    }

    // ==================== common utilities ==================== //

    /**
     * Send Jingle Message finish on end call successful.
     *
     * @param recipient the recipient callee Jid
     * @param sid the intended Jingle Message call id
     * @param reason the JingleReason xmlElement to be included
     * @param element may contain e.g. Success Element
     */
    public void sendJingleMessageFinish(Jid recipient, String sid, JingleReason reason, NamedElement element) {
        JingleMessage msgFinish = new JingleMessage(JingleMessage.ACTION_FINISH, sid, reason, element);
        sendJingleMessage(recipient, msgFinish);
    }

    /**
     * Build the chat message and add Jingle Message attachment before sending.
     *
     * @param recipient the recipient message recipient
     * @param jingleMessage the extension element to be sent
     */
    private void sendJingleMessage(Jid recipient, JingleMessage jingleMessage) {
        String msgId = "jm-" + jingleMessage.getId();
        Jid user = connection().getUser();
        Message message = StanzaBuilder.buildMessage(msgId)
                .ofType(Message.Type.chat)
                .from(user)
                .to(recipient)
                .setLanguage("us")
                .addExtension(jingleMessage)
                .build();

        try {
            connection().sendStanza(message);
        }
        catch (SmackException.NotConnectedException | InterruptedException e) {
            LOGGER.log(Level.WARNING, "Error in sending jingle message: \n" +
                    e.getMessage() + "\n" +
                    jingleMessage.toXML());
        }
    }
}
