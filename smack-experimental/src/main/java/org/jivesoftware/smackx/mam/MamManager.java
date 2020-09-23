/**
 *
 * Copyright © 2017-2020 Florian Schmaus, 2016-2017 Fernando Ramirez
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
package org.jivesoftware.smackx.mam;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.commands.AdHocCommandManager;
import org.jivesoftware.smackx.commands.RemoteCommand;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamElements.MamResultExtension;
import org.jivesoftware.smackx.mam.element.MamFinIQ;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ.DefaultBehavior;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.mam.filter.MamResultFilter;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;

/**
 * A Manager for Message Archive Management (MAM, <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313</a>).
 *
 * <h2>Get an instance of a manager for a message archive</h2>
 *
 * In order to work with {@link MamManager} you need to obtain an instance for a particular archive.
 * To get the instance for the default archive on the user's server, use the {@link #getInstanceFor(XMPPConnection)} method.
 *
 * <pre>
 * {@code
 * XMPPConnection connection = ...
 * MamManager mamManager = MamManager.getInstanceFor(connection);
 * }
 * </pre>
 *
 * If you want to retrieve a manager for a different archive use {@link #getInstanceFor(XMPPConnection, Jid)}, which takes the archive's XMPP address as second argument.
 *
 * <h2>Check if MAM is supported</h2>
 *
 * After you got your manager instance, you probably first want to check if MAM is supported.
 * Simply use {@link #isSupported()} to check if there is a MAM archive available.
 *
 * <pre>
 * {@code
 * boolean isSupported = mamManager.isSupported();
 * }
 * </pre>
 *
 * <h2>Message Archive Preferences</h2>
 *
 * After you have verified that the MAM is supported, you probably want to configure the archive first before using it.
 * One of the most important preference is to enable MAM for your account.
 * Some servers set up new accounts with MAM disabled by default.
 * You can do so by calling {@link #enableMamForAllMessages()}.
 *
 * <h3>Retrieve current preferences</h3>
 *
 * The archive's preferences can be retrieved using {@link #retrieveArchivingPreferences()}.
 *
 * <h3>Update preferences</h3>
 *
 * Use {@link MamPrefsResult#asMamPrefs()} to get a modifiable {@link MamPrefs} instance.
 * After performing the desired changes, use {@link #updateArchivingPreferences(MamPrefs)} to update the preferences.
 *
 * <h2>Query the message archive</h2>
 *
 * Querying a message archive involves a two step process. First you need specify the query's arguments, for example a date range.
 * The query arguments of a particular query are represented by a {@link MamQueryArgs} instance, which can be build using {@link MamQueryArgs.Builder}.
 *
 * After you have build such an instance, use {@link #queryArchive(MamQueryArgs)} to issue the query.
 *
 * <pre>
 * {@code
 * MamQueryArgs mamQueryArgs = MamQueryArgs.builder()
 *                                 .limitResultsToJid(jid)
 *                                 .setResultPageSizeTo(10)
 *                                 .queryLastPage()
 *                                 .build();
 * MamQuery mamQuery = mamManager.queryArchive(mamQueryArgs);
 * }
 * </pre>
 *
 * On success {@link #queryArchive(MamQueryArgs)} returns a {@link MamQuery} instance.
 * The instance will hold one page of the queries result set.
 * Use {@link MamQuery#getMessages()} to retrieve the messages of the archive belonging to the page.
 *
 * You can get the whole page including all metadata using {@link MamQuery#getPage()}.
 *
 * <h2>Paging through the results</h2>
 *
 * Because the matching result set could be potentially very big, a MAM service will probably not return all matching messages.
 * Instead the results are possibly send in multiple pages.
 * To check if the result was complete or if there are further pages, use {@link MamQuery#isComplete()}.
 * If this method returns {@code false}, then you may want to page through the archive.
 *
 * {@link MamQuery} provides convince methods to do so: {@link MamQuery#pageNext(int)} and {@link MamQuery#pagePrevious(int)}.
 *
 * <pre>
 * {@code
 * MamQuery nextPageMamQuery = mamQuery.pageNext(10);
 * }
 * </pre>
 *
 * <h2>Get the supported form fields</h2>
 *
 * You can use {@link #retrieveFormFields()} to retrieve a list of the supported additional form fields by this archive.
 * Those fields can be used for further restrict a query.
 *
 *
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Florian Schmaus
 * @author Fernando Ramirez
 *
 */
public final class MamManager extends Manager {

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    private static final String FORM_FIELD_WITH = "with";
    private static final String FORM_FIELD_START = "start";
    private static final String FORM_FIELD_END = "end";

    private static final Map<XMPPConnection, Map<Jid, MamManager>> INSTANCES = new WeakHashMap<>();

    private static final String ADVANCED_CONFIG_NODE = "urn:xmpp:mam#configure";

    /**
     * Get a MamManager for the MAM archive of the local entity (the "user") of the given connection.
     *
     * @param connection the XMPP connection to get the archive for.
     * @return the instance of MamManager.
     */
    // CHECKSTYLE:OFF:RegexpSingleline
    public static MamManager getInstanceFor(XMPPConnection connection) {
    // CHECKSTYLE:ON:RegexpSingleline
        return getInstanceFor(connection, (Jid) null);
    }

    /**
     * Get a MamManager for the MAM archive of the given {@code MultiUserChat}. Note that not all MUCs support MAM,
     * hence it is recommended to use {@link #isSupported()} to check if MAM is supported by the MUC.
     *
     * @param multiUserChat the MultiUserChat to retrieve the MamManager for.
     * @return the MamManager for the given MultiUserChat.
     * @since 4.3.0
     */
    public static MamManager getInstanceFor(MultiUserChat multiUserChat) {
        XMPPConnection connection = multiUserChat.getXmppConnection();
        Jid archiveAddress = multiUserChat.getRoom();
        return getInstanceFor(connection, archiveAddress);
    }

    public static synchronized MamManager getInstanceFor(XMPPConnection connection, Jid archiveAddress) {
        Map<Jid, MamManager> managers = INSTANCES.get(connection);
        if (managers == null) {
            managers = new HashMap<>();
            INSTANCES.put(connection, managers);
        }
        MamManager mamManager = managers.get(archiveAddress);
        if (mamManager == null) {
            mamManager = new MamManager(connection, archiveAddress);
            managers.put(archiveAddress, mamManager);
        }
        return mamManager;
    }

    private final Jid archiveAddress;

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private final AdHocCommandManager adHocCommandManager;

    private MamManager(XMPPConnection connection, Jid archiveAddress) {
        super(connection);
        this.archiveAddress = archiveAddress;
        serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        adHocCommandManager = AdHocCommandManager.getAddHocCommandsManager(connection);
    }

    /**
     * The the XMPP address of this MAM archive. Note that this method may return {@code null} if this MamManager
     * handles the local entity's archive and if the connection has never been authenticated at least once.
     *
     * @return the XMPP address of this MAM archive or {@code null}.
     * @since 4.3.0
     */
    public Jid getArchiveAddress() {
        if (archiveAddress == null) {
            EntityFullJid localJid = connection().getUser();
            if (localJid == null) {
                return null;
            }
            return localJid.asBareJid();
        }
        return archiveAddress;
    }

    public static final class MamQueryArgs {
        private final String node;

        private final Map<String, FormField> formFields;

        private final Integer maxResults;

        private final String afterUid;

        private final String beforeUid;

        private MamQueryArgs(Builder builder) {
            node = builder.node;
            formFields = builder.formFields;
            if (builder.maxResults > 0) {
                maxResults = builder.maxResults;
            } else {
                maxResults = null;
            }
            afterUid = builder.afterUid;
            beforeUid = builder.beforeUid;
        }

        private DataForm dataForm;

        DataForm getDataForm() {
            if (dataForm != null) {
                return dataForm;
            }
            DataForm.Builder dataFormBuilder = getNewMamForm();
            dataFormBuilder.addFields(formFields.values());
            dataForm = dataFormBuilder.build();
            return dataForm;
        }

        void maybeAddRsmSet(MamQueryIQ mamQueryIQ) {
            if (maxResults == null && afterUid == null && beforeUid == null) {
                return;
            }

            int max;
            if (maxResults != null) {
                max = maxResults;
            } else {
                max = -1;
            }

            RSMSet rsmSet = new RSMSet(afterUid, beforeUid, -1, -1, null, max, null, -1);
            mamQueryIQ.addExtension(rsmSet);
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String node;

            private final Map<String, FormField> formFields = new LinkedHashMap<>(8);

            private int maxResults = -1;

            private String afterUid;

            private String beforeUid;

            public Builder queryNode(String node) {
                if (node == null) {
                    return this;
                }

                this.node = node;

                return this;
            }

            public Builder limitResultsToJid(Jid withJid) {
                if (withJid == null) {
                    return this;
                }

                FormField formField = getWithFormField(withJid);
                formFields.put(formField.getFieldName(), formField);

                return this;
            }

            public Builder limitResultsSince(Date start) {
                if (start == null) {
                    return this;
                }

                FormField formField = FormField.builder(FORM_FIELD_START)
                                .setValue(start)
                                .build();
                formFields.put(formField.getFieldName(), formField);

                FormField endFormField = formFields.get(FORM_FIELD_END);
                if (endFormField != null) {
                    Date end;
                    try {
                        end = endFormField.getFirstValueAsDate();
                    }
                    catch (ParseException e) {
                        throw new IllegalStateException(e);
                    }
                    if (end.getTime() <= start.getTime()) {
                        throw new IllegalArgumentException("Given start date (" + start
                                        + ") is after the existing end date (" + end + ')');
                    }
                }

                return this;
            }

            public Builder limitResultsBefore(Date end) {
                if (end == null) {
                    return this;
                }

                FormField formField = FormField.builder(FORM_FIELD_END)
                    .setValue(end)
                    .build();
                formFields.put(formField.getFieldName(), formField);

                FormField startFormField = formFields.get(FORM_FIELD_START);
                if (startFormField != null) {
                    Date start;
                    try {
                        start = startFormField.getFirstValueAsDate();
                    } catch (ParseException e) {
                        throw new IllegalStateException(e);
                    }
                    if (end.getTime() <= start.getTime()) {
                        throw new IllegalArgumentException("Given end date (" + end
                                        + ") is before the existing start date (" + start + ')');
                    }
                }

                return this;
            }

            public Builder setResultPageSize(Integer max) {
                if (max == null) {
                    maxResults = -1;
                    return this;
                }
                return setResultPageSizeTo(max.intValue());
            }

            public Builder setResultPageSizeTo(int max) {
                if (max < 0) {
                    throw new IllegalArgumentException();
                }
                this.maxResults = max;
                return this;
            }

            /**
             * Only return the count of messages the query yields, not the actual messages. Note that not all services
             * return a correct count, some return an approximate count.
             *
             * @return an reference to this builder.
             * @see <a href="https://xmpp.org/extensions/xep-0059.html#count">XEP-0059 § 2.7</a>
             */
            public Builder onlyReturnMessageCount() {
                return setResultPageSizeTo(0);
            }

            public Builder withAdditionalFormField(FormField formField) {
                formFields.put(formField.getFieldName(), formField);
                return this;
            }

            public Builder withAdditionalFormFields(List<FormField> additionalFields) {
                for (FormField formField : additionalFields) {
                    withAdditionalFormField(formField);
                }
                return this;
            }

            public Builder afterUid(String afterUid) {
                this.afterUid = StringUtils.requireNullOrNotEmpty(afterUid, "afterUid must not be empty");
                return this;
            }

            /**
             * Specifies a message UID as 'before' anchor for the query. Note that unlike {@link #afterUid(String)} this
             * method also accepts the empty String to query the last page of an archive (c.f. XEP-0059 § 2.5).
             *
             * @param beforeUid a message UID acting as 'before' query anchor.
             * @return an instance to this builder.
             */
            public Builder beforeUid(String beforeUid) {
                // We don't perform any argument validation, since every possible argument (null, empty string,
                // non-empty string) is valid.
                this.beforeUid = beforeUid;
                return this;
            }

            /**
             * Query from the last, i.e. most recent, page of the archive. This will return the very last page of the
             * archive holding the most recent matching messages. You usually would page backwards from there on.
             *
             * @return a reference to this builder.
             * @see <a href="https://xmpp.org/extensions/xep-0059.html#last">XEP-0059 § 2.5. Requesting the Last Page in
             *      a Result Set</a>
             */
            public Builder queryLastPage() {
                return beforeUid("");
            }

            public MamQueryArgs build() {
                return new MamQueryArgs(this);
            }
        }
    }

    public MamQuery queryArchive(MamQueryArgs mamQueryArgs) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, NotLoggedInException, InterruptedException {
        String queryId = StringUtils.secureUniqueRandomString();
        String node = mamQueryArgs.node;
        DataForm dataForm = mamQueryArgs.getDataForm();

        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, node, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.setTo(archiveAddress);

        mamQueryArgs.maybeAddRsmSet(mamQueryIQ);

        return queryArchive(mamQueryIQ);
    }

    private static FormField getWithFormField(Jid withJid) {
        return FormField.builder(FORM_FIELD_WITH)
                        .setValue(withJid.toString())
                        .build();
    }

    public MamQuery queryMostRecentPage(Jid jid, int max) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, NotLoggedInException, InterruptedException {
        MamQueryArgs mamQueryArgs = MamQueryArgs.builder()
                        // Produces an empty <before/> element for XEP-0059 § 2.5
                        .queryLastPage()
                        .limitResultsToJid(jid)
                        .setResultPageSize(max)
                        .build();
        return queryArchive(mamQueryArgs);
    }

    /**
     * Get the form fields supported by the server.
     *
     * @return the list of form fields.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotLoggedInException if the XMPP connection is not authenticated.
     */
    public List<FormField> retrieveFormFields() throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException, NotLoggedInException {
        return retrieveFormFields(null);
    }

    /**
     * Get the form fields supported by the server.
     *
     * @param node The PubSub node name, can be null
     * @return the list of form fields.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotLoggedInException if the XMPP connection is not authenticated.
     */
    public List<FormField> retrieveFormFields(String node)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
            InterruptedException, NotLoggedInException {
        String queryId = StringUtils.secureUniqueRandomString();
        MamQueryIQ mamQueryIq = new MamQueryIQ(queryId, node, null);
        mamQueryIq.setTo(archiveAddress);

        MamQueryIQ mamResponseQueryIq = connection().createStanzaCollectorAndSend(mamQueryIq).nextResultOrThrow();

        return mamResponseQueryIq.getDataForm().getFields();
    }

    private MamQuery queryArchive(MamQueryIQ mamQueryIq) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, NotLoggedInException {
        MamQueryPage mamQueryPage = queryArchivePage(mamQueryIq);
        return new MamQuery(mamQueryPage, mamQueryIq.getNode(), DataForm.from(mamQueryIq));
    }

    private MamQueryPage queryArchivePage(MamQueryIQ mamQueryIq) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, NotLoggedInException {
        final XMPPConnection connection = getAuthenticatedConnectionOrThrow();
        MamFinIQ mamFinIQ;

        StanzaCollector mamFinIQCollector = connection.createStanzaCollector(new IQReplyFilter(mamQueryIq, connection));

        StanzaCollector.Configuration resultCollectorConfiguration = StanzaCollector.newConfiguration()
                .setStanzaFilter(new MamResultFilter(mamQueryIq)).setCollectorToReset(mamFinIQCollector);

        StanzaCollector cancelledResultCollector;
        try (StanzaCollector resultCollector = connection.createStanzaCollector(resultCollectorConfiguration)) {
            connection.sendStanza(mamQueryIq);
            mamFinIQ = mamFinIQCollector.nextResultOrThrow();
            cancelledResultCollector = resultCollector;
        }

        return new MamQueryPage(cancelledResultCollector, mamFinIQ);
    }

    public final class MamQuery {
        private final String node;
        private final DataForm form;

        private MamQueryPage mamQueryPage;

        private MamQuery(MamQueryPage mamQueryPage, String node, DataForm form) {
            this.node = node;
            this.form = form;

            this.mamQueryPage = mamQueryPage;
        }

        public boolean isComplete() {
            return mamQueryPage.getMamFinIq().isComplete();
        }

        public List<Message> getMessages() {
            return mamQueryPage.messages;
        }

        public List<MamResultExtension> getMamResultExtensions() {
            return mamQueryPage.mamResultExtensions;
        }

        private List<Message> page(RSMSet requestRsmSet) throws NoResponseException, XMPPErrorException,
                        NotConnectedException, NotLoggedInException, InterruptedException {
            String queryId = StringUtils.secureUniqueRandomString();
            MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, node, form);
            mamQueryIQ.setType(IQ.Type.set);
            mamQueryIQ.setTo(archiveAddress);
            mamQueryIQ.addExtension(requestRsmSet);

            mamQueryPage = queryArchivePage(mamQueryIQ);

            return mamQueryPage.messages;
        }

        private RSMSet getPreviousRsmSet() {
            return mamQueryPage.getMamFinIq().getRSMSet();
        }

        public List<Message> pageNext(int count) throws NoResponseException, XMPPErrorException, NotConnectedException,
                        NotLoggedInException, InterruptedException {
            RSMSet previousResultRsmSet = getPreviousRsmSet();
            RSMSet requestRsmSet = new RSMSet(count, previousResultRsmSet.getLast(), RSMSet.PageDirection.after);
            return page(requestRsmSet);
        }

        public List<Message> pagePrevious(int count) throws NoResponseException, XMPPErrorException,
                        NotConnectedException, NotLoggedInException, InterruptedException {
            RSMSet previousResultRsmSet = getPreviousRsmSet();
            RSMSet requestRsmSet = new RSMSet(count, previousResultRsmSet.getFirst(), RSMSet.PageDirection.before);
            return page(requestRsmSet);
        }

        public int getMessageCount() {
            return getMessages().size();
        }

        public MamQueryPage getPage() {
            return mamQueryPage;
        }
    }

    public static final class MamQueryPage {
        private final MamFinIQ mamFin;
        private final List<Message> mamResultCarrierMessages;
        private final List<MamResultExtension> mamResultExtensions;
        private final List<Forwarded<Message>> forwardedMessages;
        private final List<Message> messages;

        private MamQueryPage(StanzaCollector stanzaCollector, MamFinIQ mamFin) {
            this.mamFin = mamFin;

            List<Stanza> mamResultCarrierStanzas = stanzaCollector.getCollectedStanzasAfterCancelled();

            List<Message> mamResultCarrierMessages = new ArrayList<>(mamResultCarrierStanzas.size());
            List<MamResultExtension> mamResultExtensions = new ArrayList<>(mamResultCarrierStanzas.size());
            List<Forwarded<Message>> forwardedMessages = new ArrayList<>(mamResultCarrierStanzas.size());

            for (Stanza mamResultStanza : mamResultCarrierStanzas) {
                Message resultMessage = (Message) mamResultStanza;

                mamResultCarrierMessages.add(resultMessage);

                MamElements.MamResultExtension mamResultExtension = MamElements.MamResultExtension.from(resultMessage);
                mamResultExtensions.add(mamResultExtension);

                forwardedMessages.add(mamResultExtension.getForwarded());
            }

            this.mamResultCarrierMessages = Collections.unmodifiableList(mamResultCarrierMessages);
            this.mamResultExtensions = Collections.unmodifiableList(mamResultExtensions);
            this.forwardedMessages = Collections.unmodifiableList(forwardedMessages);
            this.messages = Collections.unmodifiableList(Forwarded.extractMessagesFrom(forwardedMessages));
        }

        public List<Message> getMessages() {
            return messages;
        }

        public List<Forwarded<Message>> getForwarded() {
            return forwardedMessages;
        }

        public List<MamResultExtension> getMamResultExtensions() {
            return mamResultExtensions;
        }

        public List<Message> getMamResultCarrierMessages() {
            return mamResultCarrierMessages;
        }

        public MamFinIQ getMamFinIq() {
            return mamFin;
        }
    }

    /**
     * Check if this MamManager's archive address supports MAM.
     *
     * @return true if MAM is supported, <code>false</code>otherwise.
     *
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @since 4.2.1
     * @see <a href="https://xmpp.org/extensions/xep-0313.html#support">XEP-0313 § 7. Determining support</a>
     */
    public boolean isSupported() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Note that this may return 'null' but SDM's supportsFeature() does the right thing™ then.
        Jid archiveAddress = getArchiveAddress();
        return serviceDiscoveryManager.supportsFeature(archiveAddress, MamElements.NAMESPACE);
    }

    public boolean isAdvancedConfigurationSupported() throws InterruptedException, XMPPException, SmackException {
        DiscoverItems discoverItems = adHocCommandManager.discoverCommands(archiveAddress);
        for (DiscoverItems.Item item : discoverItems.getItems()) {
            if (item.getNode().equals(ADVANCED_CONFIG_NODE)) {
                return true;
            }
        }
        return false;
    }

    public RemoteCommand getAdvancedConfigurationCommand() throws InterruptedException, XMPPException, SmackException {
        DiscoverItems discoverItems = adHocCommandManager.discoverCommands(archiveAddress);
        for (DiscoverItems.Item item : discoverItems.getItems()) {
            if (item.getNode().equals(ADVANCED_CONFIG_NODE))
                return adHocCommandManager.getRemoteCommand(archiveAddress, item.getNode());
        }
        throw new SmackException.FeatureNotSupportedException(ADVANCED_CONFIG_NODE, archiveAddress);
    }

    private static DataForm.Builder getNewMamForm() {
        FormField field = FormField.buildHiddenFormType(MamElements.NAMESPACE);
        DataForm.Builder form = DataForm.builder();
        form.addField(field);
        return form;
    }

    /**
     * Lookup the archive's message ID of the latest message in the archive. Returns {@code null} if the archive is
     * empty.
     *
     * @return the ID of the lastest message or {@code null}.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws NotLoggedInException if the XMPP connection is not authenticated.
     * @throws InterruptedException if the calling thread was interrupted.
     * @since 4.3.0
     */
    public String getMessageUidOfLatestMessage() throws NoResponseException, XMPPErrorException, NotConnectedException, NotLoggedInException, InterruptedException {
        MamQueryArgs mamQueryArgs = MamQueryArgs.builder()
                .setResultPageSize(1)
                .queryLastPage()
                .build();

        MamQuery mamQuery = queryArchive(mamQueryArgs);
        if (mamQuery.getMessages().isEmpty()) {
            return null;
        }

        return mamQuery.getMamResultExtensions().get(0).getId();
    }

    /**
     * Get the preferences stored in the server.
     *
     * @return the MAM preferences result
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotLoggedInException if the XMPP connection is not authenticated.
     */
    public MamPrefsResult retrieveArchivingPreferences() throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        MamPrefsIQ mamPrefIQ = new MamPrefsIQ();
        return queryMamPrefs(mamPrefIQ);
    }

    /**
     * Update the preferences in the server.
     *
     * @param mamPrefs the MAM preferences to set the archive to
     * @return the currently active preferences after the operation.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotLoggedInException if the XMPP connection is not authenticated.
     * @since 4.3.0
     */
    public MamPrefsResult updateArchivingPreferences(MamPrefs mamPrefs) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, NotLoggedInException {
        MamPrefsIQ mamPrefIQ = mamPrefs.constructMamPrefsIq();
        return queryMamPrefs(mamPrefIQ);
    }

    public MamPrefsResult enableMamForAllMessages() throws NoResponseException, XMPPErrorException,
                    NotConnectedException, NotLoggedInException, InterruptedException {
        return setDefaultBehavior(DefaultBehavior.always);
    }

    public MamPrefsResult enableMamForRosterMessages() throws NoResponseException, XMPPErrorException,
                    NotConnectedException, NotLoggedInException, InterruptedException {
        return setDefaultBehavior(DefaultBehavior.roster);
    }

    public MamPrefsResult setDefaultBehavior(DefaultBehavior desiredDefaultBehavior) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, NotLoggedInException, InterruptedException {
        MamPrefsResult mamPrefsResult = retrieveArchivingPreferences();
        if (mamPrefsResult.mamPrefs.getDefault() == desiredDefaultBehavior) {
            return mamPrefsResult;
        }

        MamPrefs mamPrefs = mamPrefsResult.asMamPrefs();
        mamPrefs.setDefaultBehavior(desiredDefaultBehavior);
        return updateArchivingPreferences(mamPrefs);
    }

    /**
     * MAM preferences result class.
     *
     */
    public static final class MamPrefsResult {
        public final MamPrefsIQ mamPrefs;
        public final DataForm form;

        private MamPrefsResult(MamPrefsIQ mamPrefs, DataForm form) {
            this.mamPrefs = mamPrefs;
            this.form = form;
        }

        public MamPrefs asMamPrefs() {
            return new MamPrefs(this);
        }
    }

    public static final class MamPrefs {
        private final List<Jid> alwaysJids;
        private final List<Jid> neverJids;
        private DefaultBehavior defaultBehavior;

        private MamPrefs(MamPrefsResult mamPrefsResult) {
            MamPrefsIQ mamPrefsIq = mamPrefsResult.mamPrefs;
            this.alwaysJids = new ArrayList<>(mamPrefsIq.getAlwaysJids());
            this.neverJids = new ArrayList<>(mamPrefsIq.getNeverJids());
            this.defaultBehavior = mamPrefsIq.getDefault();
        }

        public void setDefaultBehavior(DefaultBehavior defaultBehavior) {
            this.defaultBehavior = Objects.requireNonNull(defaultBehavior, "defaultBehavior must not be null");
        }

        public DefaultBehavior getDefaultBehavior() {
            return defaultBehavior;
        }

        public List<Jid> getAlwaysJids() {
            return alwaysJids;
        }

        public List<Jid> getNeverJids() {
            return neverJids;
        }

        private MamPrefsIQ constructMamPrefsIq() {
            return new MamPrefsIQ(alwaysJids, neverJids, defaultBehavior);
        }
    }

    private MamPrefsResult queryMamPrefs(MamPrefsIQ mamPrefsIQ) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        final XMPPConnection connection = getAuthenticatedConnectionOrThrow();

        MamPrefsIQ mamPrefsResultIQ = connection.createStanzaCollectorAndSend(mamPrefsIQ).nextResultOrThrow();

        return new MamPrefsResult(mamPrefsResultIQ, DataForm.from(mamPrefsIQ));
    }

}
