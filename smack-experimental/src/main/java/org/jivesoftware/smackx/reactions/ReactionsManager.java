/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reactions;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.ExtensionElementFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.reactions.element.Reaction;
import org.jivesoftware.smackx.reactions.element.ReactionsElement;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextMultiFormField;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

/**
 * Manages reactions in the XMPP protocol. This class allows adding, removing, and listening for reactions
 * on messages, as well as managing restrictions on the number of reactions per user and allowed emojis.
 * It also allows propagating these restrictions to other clients via XMPP service discovery.
 *
 * This class is based on the XEP-0444 extension protocol for reactions.
 *
 * @author Ismael Nunes Campos
 *
 * @see <a href="https://xmpp.org/extensions/xep-0444.html">XEP-0444 Message Reactions</a>
 * @see ReactionsElement
 * @see Reaction
 */
public final class ReactionsManager extends Manager {

    private static final Map<XMPPConnection, ReactionsManager> INSTANCES = new WeakHashMap<>();

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                if (ENABLED_BY_DEFAULT) {
                    getInstanceFor(connection).announceSupport();
                }
            }
        });
    }

    private static final String REACTIONS_RESTRICTIONS_NAMESPACE = "urn:xmpp:reactions:0:restrictions";

    private static boolean ENABLED_BY_DEFAULT = false;

    private final Set<ReactionsListener> listeners = new CopyOnWriteArraySet<>();

    private final AsyncButOrdered<BareJid> asyncButOrdered = new AsyncButOrdered<>();

    private final StanzaListener stanzaListener;

    private boolean isStanzaListenerActive = false;

    private final XMPPConnection connection;

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private final StanzaFilter REACTIONS_FILTER = new AndFilter(StanzaTypeFilter.MESSAGE,
                    new ExtensionElementFilter<ReactionsElement>(ReactionsElement.class));

    /**
     * Retrieves the instance of the ReactionsManager for the given XMPP connection.
     *
     * @param connection The XMPP connection.
     * @return The ReactionsManager instance for the connection.
     */
    public static synchronized ReactionsManager getInstanceFor(XMPPConnection connection) {
        ReactionsManager reactionsManager = INSTANCES.get(connection);

        if (reactionsManager == null) {
            reactionsManager = new ReactionsManager(connection);
            INSTANCES.put(connection, reactionsManager);
        }
        return reactionsManager;
    }

    /**
     * Constructs an instance of the reactions' manager.
     *
     * @param connection The XMPP connection used by the manager.
     */
    public ReactionsManager(XMPPConnection connection) {
        super(connection);
        this.connection = connection;

        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

        stanzaListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet)
                            throws SmackException.NotConnectedException, InterruptedException,
                            SmackException.NotLoggedInException {
                final Message message = (Message) packet;
                final ReactionsElement reactionsElement = ReactionsElement.fromMessage(message);
                final Set<Reaction> reactionsSet = reactionsElement.getReactions();
                final Set<String> reactions = new LinkedHashSet<>();

                for (Reaction reaction : reactionsSet) {
                    reactions.add(reaction.getEmoji());
                }

                asyncButOrdered.performAsyncButOrdered(message.getFrom().asBareJid(), () -> {
                    for (ReactionsListener l : listeners) {
                        l.onReactionReceived(message.getStanzaId(), reactions, reactionsElement, message);
                    }
                });
            }
        };

    }

    /**
     * Enable or disable auto-announcing support for Message Reactions.
     * Default is disabled.
     *
     * @param enabled enabled
     */
    public static synchronized void setEnabledByDefault(boolean enabled) {
        ENABLED_BY_DEFAULT = enabled;
    }

    /**
     * Announce support for Message Reactions to the server.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0444.html#disco">XEP-0444: Message Reactions: §2. Discovering Support</a>
     */
    public void announceSupport() {
        serviceDiscoveryManager.addFeature(ReactionsElement.NAMESPACE);
    }

    /**
     * Stop announcing support for Message Reactions.
     */
    public void stopAnnouncingSupport() {
        serviceDiscoveryManager.removeFeature(ReactionsElement.NAMESPACE);
    }

    /**
     * Checks whether the user supports reactions.
     *
     * @param jid The JID of the user.
     * @return {@code true} if the user supports reactions, otherwise {@code false}.
     * @throws XMPPException.XMPPErrorException If an XMPP error occurs.
     * @throws SmackException.NotConnectedException If the connection is not established.
     * @throws InterruptedException If the operation is interrupted.
     * @throws SmackException.NoResponseException If no response is received from the server.
     */
    public boolean userSupportsReactions(Jid jid) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
                    InterruptedException, SmackException.NoResponseException {
        return serviceDiscoveryManager.supportsFeature(jid, ReactionsElement.NAMESPACE);
    }

    /**
     * Checks whether the server supports reactions.
     *
     * @return {@code true} if the server supports reactions, otherwise {@code false}.
     * @throws XMPPException.XMPPErrorException If an XMPP error occurs.
     * @throws SmackException.NotConnectedException If the connection is not established.
     * @throws InterruptedException If the operation is interrupted.
     * @throws SmackException.NoResponseException If no response is received from the server.
     */
    public boolean serverSupportsReactions()
                    throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
                    SmackException.NoResponseException {
        return serviceDiscoveryManager.serverSupportsFeature(ReactionsElement.NAMESPACE);
    }

    /**
     * Creates a new message builder that contains reactions to the original message identified by its ID.
     * The reactions are added to the new message, ensuring they meet the specified restrictions.
     *
     * <p>This method generates a new message builder that will create a message containing the list of reactions (emojis)
     * to the original message identified by its ID. It ensures that the number of reactions and the emojis used comply
     * with the given restrictions.</p>
     *
     * @param emojis              The list of emojis to be added as reactions.
     * @param originalMessageId   The ID of the original message being reacted to. This ID will be used for the {@link ReactionsElement}.
     * @param restrictions        The reaction restrictions, such as the maximum number of reactions per user and allowed emojis.
     * @return                   A message builder containing the reactions to the original message.
     * @throws IllegalArgumentException If the number of reactions exceeds the allowed limit or if any emoji is not allowed.
     */
    public static MessageBuilder createMessageWithReactions(Set<String> emojis, String originalMessageId, ReactionRestrictions restrictions) {
        if (restrictions != null) {
            if (emojis.size() > restrictions.getMaxReactionsPerUser()) {
                throw new IllegalArgumentException("Exceeded maximum number of reactions per user");
            }


            for (String emoji : emojis) {
                if (!restrictions.getAllowedEmojis().contains(emoji)) {
                    throw new IllegalArgumentException("Emoji " + emoji + " is not allowed");
                }
            }
        }

        Set<Reaction> reactions = new LinkedHashSet<>();
        for (String emoji : emojis) {
            reactions.add(new Reaction(emoji));
        }

        ReactionsElement reactionsElement = new ReactionsElement(reactions, originalMessageId);

        MessageBuilder newMessage = StanzaBuilder.buildMessage();

        newMessage.addExtension(reactionsElement);

        return newMessage;
    }

    /**
     * Adds a reaction listener. If this is the first listener, the StanzaListener is added to the connection.
     *
     * @param listener The listener to be added.
     * @return true if the listener was successfully added, false otherwise.
     */
    public synchronized boolean addReactionsListener(ReactionsListener listener) {
        boolean added = listeners.add(listener);
        if (added && listeners.size() == 1 && !isStanzaListenerActive) {
            connection.addAsyncStanzaListener(stanzaListener, REACTIONS_FILTER);
            isStanzaListenerActive = true;
        }
        return added;
    }

    /**
     * Removes a reaction listener. If this is the last listener, the StanzaListener is removed from the connection.
     *
     * @param listener The listener to be removed.
     * @return true if the listener was successfully removed, false otherwise.
     */
    public synchronized boolean removeReactionsListener(ReactionsListener listener) {
        boolean removed = listeners.remove(listener);
        if (removed && listeners.isEmpty() && isStanzaListenerActive) {
            connection.removeAsyncStanzaListener(stanzaListener);
            isStanzaListenerActive = false;
        }
        return removed;
    }

    /**
     * Creates a form for reaction restrictions, including the max number of reactions per user
     * and the list of allowed emojis.
     *
     * @param maxReactionsPerUser The maximum number of reactions allowed per user.
     * @param allowedEmojis The list of allowed emojis.
     * @return The reaction restrictions form.
     */
    public static DataForm createReactionRestrictionsForm(int maxReactionsPerUser, Set<String> allowedEmojis) {

        DataForm.Builder builder = DataForm.builder(DataForm.Type.result);
        builder.setFormType(REACTIONS_RESTRICTIONS_NAMESPACE);

        builder.addField(
                        FormField.builder("max_reactions_per_user")
                                        .setValue(maxReactionsPerUser)
                                        .build()
        );

        TextMultiFormField.Builder allowlistFieldBuilder = FormField.textMultiBuilder("allowlist");

        for (String emoji : allowedEmojis) {
            allowlistFieldBuilder.addValue(emoji);
        }

        builder.addField(allowlistFieldBuilder.build());

        return builder.build();
    }

    /**
     * Advertises reaction restrictions to a given XMPP server.
     *
     * @param connection The XMPP connection.
     * @param maxReactionsPerUser The maximum number of reactions allowed per user.
     * @param allowedEmojis The list of allowed emojis.
     */
    public void advertiseReactionRestrictions(XMPPConnection connection, int maxReactionsPerUser, Set<String> allowedEmojis) {
        DataForm restrictionsForm = createReactionRestrictionsForm(maxReactionsPerUser, allowedEmojis);
        serviceDiscoveryManager.addExtendedInfo(restrictionsForm);
    }

    /**
     * Retrieves the reaction restrictions for a given entity.
     *
     * @param jid The JID of the user.
     * @return The reaction restrictions for the user.
     * @throws XMPPException.XMPPErrorException If an XMPP error occurs.
     * @throws SmackException.NotConnectedException If the connection is not established.
     * @throws InterruptedException If the operation is interrupted.
     * @throws SmackException.NoResponseException If no response is received from the server.
     */
    public ReactionRestrictions getReactionRestrictions(Jid jid)
                    throws XMPPException.XMPPErrorException,
                    SmackException.NotConnectedException,
                    InterruptedException, SmackException.NoResponseException {

        DiscoverInfo discoverInfo = serviceDiscoveryManager.discoverInfo(jid);

        for (DataForm extension : discoverInfo.getExtensions(DataForm.class)) {

            FormField formTypeField = extension.getField("FORM_TYPE");

            if (formTypeField != null && formTypeField.getValues().stream().anyMatch(v -> v.toString().equals(REACTIONS_RESTRICTIONS_NAMESPACE))) {

                Form form = new Form(extension);
                int maxReactionsPerUser = Integer.parseInt(form.getField("max_reactions_per_user").getFirstValue());

                Set<String> allowedEmojis = form.getField("allowlist")
                               .getValues()
                               .stream()
                               .map(CharSequence::toString)
                               .collect(Collectors.toSet());

                    return new ReactionRestrictions(maxReactionsPerUser, allowedEmojis);
            }

        }
        return null;
    }

    /**
     * Represents the reaction restrictions for a user or XMPP server.
     */
    public static class ReactionRestrictions {
        private final int maxReactionsPerUser;
        private final Set<String> allowedEmojis;

        /**
         * Constructs the reaction restrictions.
         *
         * @param maxReactionsPerUser The maximum number of reactions allowed per user.
         * @param allowedEmojis The list of allowed emojis.
         */
        public ReactionRestrictions(int maxReactionsPerUser, Set<String> allowedEmojis) {
            this.maxReactionsPerUser = maxReactionsPerUser;
            this.allowedEmojis = allowedEmojis;
        }

        /**
         * Retrieves the maximum number of reactions allowed per user.
         *
         * @return The maximum number of reactions.
         */
        public int getMaxReactionsPerUser() {
            return maxReactionsPerUser;
        }

        /**
         * Retrieves the list of allowed emojis.
         *
         * @return The list of allowed emojis.
         */
        public Set<String> getAllowedEmojis() {
            return allowedEmojis;
        }
    }
}
