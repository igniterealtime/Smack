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

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.reactions.element.Reaction;
import org.jivesoftware.smackx.reactions.element.ReactionsElement;
import org.jivesoftware.smackx.reactions.filter.ReactionsFilter;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.TextSingleFormField;
import org.jivesoftware.smackx.xdata.form.Form;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.stream.Collectors;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;

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
                getInstanceFor(connection);
            }
        });
    }

    private static final String REACTIONS_RESTRICTIONS_NAMESPACE = "urn:xmpp:reactions:0:restrictions";
    private final Set<ReactionsListener> listeners = new CopyOnWriteArraySet<>();
    private final AsyncButOrdered<BareJid> asyncButOrdered = new AsyncButOrdered<>();
    private final  StanzaFilter reactionsElementFilter = new AndFilter(StanzaTypeFilter.MESSAGE,ReactionsFilter.INSTANCE);


    /**
     * Constructs an instance of the reactions manager and add ReactionsElement to disco features.
     *
     * @param connection The XMPP connection used by the manager.
     */
    public ReactionsManager(XMPPConnection connection) {
        super(connection);
        connection.addAsyncStanzaListener(this::reactionsElementListener,reactionsElementFilter);
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        sdm.addFeature(ReactionsElement.NAMESPACE);
    }

    /**
     * Listener method for reactions elements in XMPP messages. This method is invoked when a new
     * stanza (message) is received and attempts to extract a {@link ReactionsElement} from the message.
     * If the element is found, it notifies the registered reaction listeners.
     *
     * @param packet The received XMPP stanza (message).
     */
    public  void reactionsElementListener(Stanza packet){
        Message message = (Message) packet;
        ReactionsElement reactionsElement = ReactionsElement.fromMessage(message);

        if (reactionsElement != null){
            notifyReactionListeners(message,reactionsElement);
        }

    }

    /**
     * Notifies all registered reaction listeners that a new reaction has been received. This method
     * performs the notification in an ordered, asynchronous manner to ensure listeners are notified in
     * the order that they were added.
     *
     * @param message The XMPP message that contains the reactions.
     * @param reactionsElement The {@link ReactionsElement} containing the reactions.
     */
    public  void notifyReactionListeners(Message message, ReactionsElement reactionsElement) {
        for (ReactionsListener listener : listeners) {
            asyncButOrdered.performAsyncButOrdered(message.getFrom().asBareJid(), () -> {
                listener.onReactionReceived(message, reactionsElement);
            });
        }
    }


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
     * Checks whether the user supports reactions.
     *
     * @param jid The JID of the user.
     * @return {@code true} if the user supports reactions, otherwise {@code false}.
     * @throws XMPPException.XMPPErrorException If an XMPP error occurs.
     * @throws SmackException.NotConnectedException If the connection is not established.
     * @throws InterruptedException If the operation is interrupted.
     * @throws SmackException.NoResponseException If no response is received from the server.
     */
    public boolean userSupportsReactions(EntityBareJid jid) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
                    InterruptedException, SmackException.NoResponseException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(jid,ReactionsElement.NAMESPACE);
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
        return ServiceDiscoveryManager.getInstanceFor(connection())
                        .serverSupportsFeature(ReactionsElement.NAMESPACE);
    }

    /**
     * Adds reactions to a message.
     *
     * @param message           The message builder where the reactions will be added.
     * @param emojis            The list of emojis to be added as reactions.
     * @param originalMessageId The ID of the original message being reacted to.
     * @param restrictions      The reaction restrictions such as max reactions per user and allowed emojis.
     * @throws IllegalArgumentException If the reactions exceed the allowed limit or if any emoji is not allowed.
     */
    public static void addReactionsToMessage(Message message, List<String> emojis,
                    String originalMessageId, ReactionRestrictions restrictions){
        List<Reaction> reactions = new ArrayList<>();

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


        for (String emoji : emojis) {
            Reaction reaction = new Reaction(emoji);
            reactions.add(reaction);
        }

        ReactionsElement reactionsElement = new ReactionsElement(reactions, originalMessageId);

        message.addExtension(reactionsElement);

    }

    /**
     * Adds a reactions' listener.
     *
     * @param listener The reactions listener to be added.
     */
    public synchronized void addReactionsListener(ReactionsListener listener){
        listeners.add(listener);
    }

    /**
     * Removes a reactions listener.
     *
     * @param listener The reactions listener to be removed.
     */
    public synchronized void removeReactionsListener(ReactionsListener listener){
        listeners.remove(listener);
    }


    /**
     * Creates a form for reaction restrictions, including the max number of reactions per user
     * and the list of allowed emojis.
     *
     * @param maxReactionsPerUser The maximum number of reactions allowed per user.
     * @param allowedEmojis The list of allowed emojis.
     * @return The reaction restrictions form.
     */
    public static DataForm createReactionRestrictionsForm(int maxReactionsPerUser, List<String> allowedEmojis) {

        DataForm.Builder builder = DataForm.builder();
        builder.setFormType(String.valueOf(DataForm.Type.result));
        builder.addField(
                        FormField.buildHiddenFormType("urn:xmpp:reactions:0:restrictions")
        );
        builder.addField(
                        FormField.builder("max_reactions_per_user").setValue(String.valueOf(maxReactionsPerUser))
                                        .build()
        );

        FormField.Builder<TextSingleFormField, TextSingleFormField.Builder> allowlistFieldBuilder = FormField.builder("allowlist");
        for (String emoji : allowedEmojis) {
            Reaction reaction = new Reaction(emoji);
            FormField.builder("value").setValue((CharSequence) reaction);
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
    public void advertiseReactionRestrictions(XMPPConnection connection, int maxReactionsPerUser, List<String> allowedEmojis) {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        DataForm restrictionsForm = createReactionRestrictionsForm(maxReactionsPerUser, allowedEmojis);
        sdm.addExtendedInfo(restrictionsForm);

        sdm.addFeature(ReactionsElement.NAMESPACE);
    }

    /**
     * Retrieves the reaction restrictions for a given user.
     *
     * @param jid The JID of the user.
     * @return The reaction restrictions for the user.
     * @throws XMPPException.XMPPErrorException If an XMPP error occurs.
     * @throws SmackException.NotConnectedException If the connection is not established.
     * @throws InterruptedException If the operation is interrupted.
     * @throws SmackException.NoResponseException If no response is received from the server.
     */
    public ReactionRestrictions getReactionRestrictions(EntityBareJid jid) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException,
                    InterruptedException, SmackException.NoResponseException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        DiscoverInfo discoverInfo = sdm.discoverInfo(jid);


        for (XmlElement extension : discoverInfo.getExtensions()) {
            if (extension instanceof DataForm) {
                DataForm dataForm = (DataForm) extension;
                FormField formTypeField = dataForm.getField("FORM_TYPE");
                if (formTypeField != null && formTypeField.getValues().stream().anyMatch(v -> v.toString().equals(REACTIONS_RESTRICTIONS_NAMESPACE))) {
                    Form form = new Form(dataForm);
                    int maxReactionsPerUser = Integer.parseInt(form.getField("max_reactions_per_user").getFirstValue());

                    // Converts List<? extends CharSequence> to List<String>
                    List<String> allowedEmojis = form.getField("allowlist")
                                    .getValues()
                                    .stream()
                                    .map(CharSequence::toString)
                                    .collect(Collectors.toList());

                    return new ReactionRestrictions(maxReactionsPerUser, allowedEmojis);
                }
            }
        }
        return null;
    }

    /**
     * Represents the reaction restrictions for a user or XMPP server.
     */
    public static class ReactionRestrictions {
        private final int maxReactionsPerUser;
        private final List<String> allowedEmojis;

        /**
         * Constructs the reaction restrictions.
         *
         * @param maxReactionsPerUser The maximum number of reactions allowed per user.
         * @param allowedEmojis The list of allowed emojis.
         */
        public ReactionRestrictions(int maxReactionsPerUser, List<String> allowedEmojis) {
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
        public List<String> getAllowedEmojis() {
            return allowedEmojis;
        }
    }

}
