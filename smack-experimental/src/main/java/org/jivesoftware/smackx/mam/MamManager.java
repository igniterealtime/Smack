/**
 *
 * Copyright © 2016-2017 Florian Schmaus, Fernando Ramirez
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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamFinIQ;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ.DefaultBehavior;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.mam.filter.MamResultFilter;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppDateTime;

/**
 * A Manager for Message Archive Management (XEP-0313).
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

    private static final Map<XMPPConnection, Map<Jid, MamManager>> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of MamManager.
     * 
     * @param connection
     * @return the instance of MamManager
     */
    public static MamManager getInstanceFor(XMPPConnection connection) {
        return getInstanceFor(connection, null);
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

    private MamManager(XMPPConnection connection, Jid archiveAddress) {
        super(connection);
        this.archiveAddress = archiveAddress;
    }

    /**
     * Query archive with a maximum amount of results.
     * 
     * @param max
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchive(Integer max) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        return queryArchive(null, max, null, null, null, null);
    }

    /**
     * Query archive with a JID (only messages from/to the JID).
     * 
     * @param withJid
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchive(Jid withJid) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        return queryArchive(null, null, null, null, withJid, null);
    }

    /**
     * Query archive filtering by start and/or end date. If start == null, the
     * value of 'start' will be equal to the date/time of the earliest message
     * stored in the archive. If end == null, the value of 'end' will be equal
     * to the date/time of the most recent message stored in the archive.
     * 
     * @param start
     * @param end
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchive(Date start, Date end) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        return queryArchive(null, null, start, end, null, null);
    }

    /**
     * Query Archive adding filters with additional fields.
     * 
     * @param additionalFields
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchive(List<FormField> additionalFields) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        return queryArchive(null, null, null, null, null, additionalFields);
    }

    /**
     * Query archive filtering by start date. The value of 'end' will be equal
     * to the date/time of the most recent message stored in the archive.
     * 
     * @param start
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchiveWithStartDate(Date start) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        return queryArchive(null, null, start, null, null, null);
    }

    /**
     * Query archive filtering by end date. The value of 'start' will be equal
     * to the date/time of the earliest message stored in the archive.
     * 
     * @param end
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchiveWithEndDate(Date end) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        return queryArchive(null, null, null, end, null, null);
    }


    /**
     * Query archive applying filters: max count, start date, end date, from/to
     * JID and with additional fields.
     * 
     * @param max
     * @param start
     * @param end
     * @param withJid
     * @param additionalFields
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchive(Integer max, Date start, Date end, Jid withJid, List<FormField> additionalFields)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException,
            NotLoggedInException {
      return queryArchive(null, max, start, end, withJid, additionalFields);
    }


    /**
     * Query an message archive like a MUC archive or a pubsub node archive, addressed by an archiveAddress, applying
     * filters: max count, start date, end date, from/to JID and with additional fields. When archiveAddress is null the
     * default, the server will be requested.
     * 
     * @param node The Pubsub node name, can be null
     * @param max
     * @param start
     * @param end
     * @param withJid
     * @param additionalFields
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult queryArchive(String node, Integer max, Date start, Date end, Jid withJid,
                    List<FormField> additionalFields)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException,
            NotLoggedInException {
        DataForm dataForm = null;
        String queryId = UUID.randomUUID().toString();

        if (start != null || end != null || withJid != null || additionalFields != null) {
            dataForm = getNewMamForm();
            addStart(start, dataForm);
            addEnd(end, dataForm);
            addWithJid(withJid, dataForm);
            addAdditionalFields(additionalFields, dataForm);
        }

        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, node, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.setTo(archiveAddress);

        addResultsLimit(max, mamQueryIQ);
        return queryArchive(mamQueryIQ);
    }

    private static void addAdditionalFields(List<FormField> additionalFields, DataForm dataForm) {
        if (additionalFields == null) {
            return;
        }
        for (FormField formField : additionalFields) {
            dataForm.addField(formField);
        }
    }

    private static void addResultsLimit(Integer max, MamQueryIQ mamQueryIQ) {
        if (max == null) {
            return;
        }
        RSMSet rsmSet = new RSMSet(max);
        mamQueryIQ.addExtension(rsmSet);

    }

    private static void addWithJid(Jid withJid, DataForm dataForm) {
        if (withJid == null) {
            return;
        }
        FormField formField = new FormField("with");
        formField.addValue(withJid.toString());
        dataForm.addField(formField);
    }

    private static void addEnd(Date end, DataForm dataForm) {
        if (end == null) {
            return;
        }
        FormField formField = new FormField("end");
        formField.addValue(XmppDateTime.formatXEP0082Date(end));
        dataForm.addField(formField);
    }

    private static void addStart(Date start, DataForm dataForm) {
        if (start == null) {
            return;
        }
        FormField formField = new FormField("start");
        formField.addValue(XmppDateTime.formatXEP0082Date(start));
        dataForm.addField(formField);
    }

    /**
     * Returns a page of the archive.
     * 
     * @param dataForm
     * @param rsmSet
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult page(DataForm dataForm, RSMSet rsmSet) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, NotLoggedInException {

        return page(null, dataForm, rsmSet);

    }

    /**
     * Returns a page of the archive.
     * 
     * @param node The Pubsub node name, can be null
     * @param dataForm
     * @param rsmSet
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult page(String node, DataForm dataForm, RSMSet rsmSet)
                    throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        MamQueryIQ mamQueryIQ = new MamQueryIQ(UUID.randomUUID().toString(), node, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.setTo(archiveAddress);
        mamQueryIQ.addExtension(rsmSet);
        return queryArchive(mamQueryIQ);
    }

    /**
     * Returns the next page of the archive.
     * 
     * @param mamQueryResult
     *            is the previous query result
     * @param count
     *            is the amount of messages that a page contains
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult pageNext(MamQueryResult mamQueryResult, int count) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException, NotLoggedInException {
        RSMSet previousResultRsmSet = mamQueryResult.mamFin.getRSMSet();
        RSMSet requestRsmSet = new RSMSet(count, previousResultRsmSet.getLast(), RSMSet.PageDirection.after);
        return page(mamQueryResult, requestRsmSet);
    }

    /**
     * Returns the previous page of the archive.
     * 
     * @param mamQueryResult
     *            is the previous query result
     * @param count
     *            is the amount of messages that a page contains
     * @return the MAM query result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamQueryResult pagePrevious(MamQueryResult mamQueryResult, int count) throws NoResponseException,
            XMPPErrorException, NotConnectedException, InterruptedException, NotLoggedInException {
        RSMSet previousResultRsmSet = mamQueryResult.mamFin.getRSMSet();
        RSMSet requestRsmSet = new RSMSet(count, previousResultRsmSet.getFirst(), RSMSet.PageDirection.before);
        return page(mamQueryResult, requestRsmSet);
    }

    private MamQueryResult page(MamQueryResult mamQueryResult, RSMSet requestRsmSet) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, NotLoggedInException, InterruptedException {
        ensureMamQueryResultMatchesThisManager(mamQueryResult);

        return page(mamQueryResult.node, mamQueryResult.form, requestRsmSet);
    }

    /**
     * Obtain page before the first message saved (specific chat).
     * <p>
     * Note that the messageUid is the XEP-0313 UID and <b>not</> the stanza ID of the message.
     * </p>
     *
     * @param chatJid
     * @param messageUid the UID of the message of which messages before should be received.
     * @param max
     * @return the MAM query result
     * @throws XMPPErrorException
     * @throws NotLoggedInException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    public MamQueryResult pageBefore(Jid chatJid, String messageUid, int max) throws XMPPErrorException,
            NotLoggedInException, NotConnectedException, InterruptedException, NoResponseException {
        RSMSet rsmSet = new RSMSet(null, messageUid, -1, -1, null, max, null, -1);
        DataForm dataForm = getNewMamForm();
        addWithJid(chatJid, dataForm);
        return page(null, dataForm, rsmSet);
    }

    /**
     * Obtain page after the last message saved (specific chat).
     * <p>
     * Note that the messageUid is the XEP-0313 UID and <b>not</> the stanza ID of the message.
     * </p>
     *
     * @param chatJid
     * @param messageUid the UID of the message of which messages after should be received.
     * @param max
     * @return the MAM query result
     * @throws XMPPErrorException
     * @throws NotLoggedInException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    public MamQueryResult pageAfter(Jid chatJid, String messageUid, int max) throws XMPPErrorException,
            NotLoggedInException, NotConnectedException, InterruptedException, NoResponseException {
        RSMSet rsmSet = new RSMSet(messageUid, null, -1, -1, null, max, null, -1);
        DataForm dataForm = getNewMamForm();
        addWithJid(chatJid, dataForm);
        return page(null, dataForm, rsmSet);
    }

    /**
     * Obtain the most recent page of a chat.
     *
     * @param chatJid
     * @param max
     * @return the MAM query result
     * @throws XMPPErrorException
     * @throws NotLoggedInException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    public MamQueryResult mostRecentPage(Jid chatJid, int max) throws XMPPErrorException, NotLoggedInException,
            NotConnectedException, InterruptedException, NoResponseException {
        return pageBefore(chatJid, "", max);
    }

    /**
     * Get the form fields supported by the server.
     * 
     * @return the list of form fields.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public List<FormField> retrieveFormFields() throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException, NotLoggedInException {
        return retrieveFormFields(null);
    }

    /**
     * Get the form fields supported by the server.
     * 
     * @param node The Pubsub node name, can be null
     * @return the list of form fields.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public List<FormField> retrieveFormFields(String node)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
            InterruptedException, NotLoggedInException {
        String queryId = UUID.randomUUID().toString();
        MamQueryIQ mamQueryIq = new MamQueryIQ(queryId, node, null);
        mamQueryIq.setTo(archiveAddress);

        MamQueryIQ mamResponseQueryIq = connection().createStanzaCollectorAndSend(mamQueryIq).nextResultOrThrow();

        return mamResponseQueryIq.getDataForm().getFields();
    }

    private MamQueryResult queryArchive(MamQueryIQ mamQueryIq) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        final XMPPConnection connection = getAuthenticatedConnectionOrThrow();
        MamFinIQ mamFinIQ = null;

        StanzaCollector mamFinIQCollector = connection.createStanzaCollector(new IQReplyFilter(mamQueryIq, connection));

        StanzaCollector.Configuration resultCollectorConfiguration = StanzaCollector.newConfiguration()
                .setStanzaFilter(new MamResultFilter(mamQueryIq)).setCollectorToReset(mamFinIQCollector);
        StanzaCollector resultCollector = connection.createStanzaCollector(resultCollectorConfiguration);

        try {
            connection.sendStanza(mamQueryIq);
            mamFinIQ = mamFinIQCollector.nextResultOrThrow();
        } finally {
            mamFinIQCollector.cancel();
            resultCollector.cancel();
        }

        List<Forwarded> forwardedMessages = new ArrayList<>(resultCollector.getCollectedCount());

        for (Message resultMessage = resultCollector
                .pollResult(); resultMessage != null; resultMessage = resultCollector.pollResult()) {
            MamElements.MamResultExtension mamResultExtension = MamElements.MamResultExtension.from(resultMessage);
            forwardedMessages.add(mamResultExtension.getForwarded());
        }

        return new MamQueryResult(forwardedMessages, mamFinIQ, mamQueryIq.getNode(), DataForm.from(mamQueryIq));
    }

    /**
     * MAM query result class.
     *
     */
    public final static class MamQueryResult {
        public final List<Forwarded> forwardedMessages;
        public final MamFinIQ mamFin;
        private final String node;
        private final DataForm form;

        private MamQueryResult(List<Forwarded> forwardedMessages, MamFinIQ mamFin, String node, DataForm form) {
            this.forwardedMessages = forwardedMessages;
            this.mamFin = mamFin;
            this.node = node;
            this.form = form;
        }
    }

    private void ensureMamQueryResultMatchesThisManager(MamQueryResult mamQueryResult) {
        EntityFullJid localAddress = connection().getUser();
        EntityBareJid localBareAddress = null;
        if (localAddress != null) {
            localBareAddress = localAddress.asEntityBareJid();
        }
        boolean isLocalUserArchive = archiveAddress == null || archiveAddress.equals(localBareAddress);

        Jid finIqFrom = mamQueryResult.mamFin.getFrom();

        if (finIqFrom != null) {
            if (finIqFrom.equals(archiveAddress) || (isLocalUserArchive && finIqFrom.equals(localBareAddress))) {
                return;
            }
            throw new IllegalArgumentException("The given MamQueryResult is from the MAM archive '" + finIqFrom
                            + "' whereas this MamManager is responsible for '" + archiveAddress + '\'');
        }
        else if (!isLocalUserArchive) {
            throw new IllegalArgumentException(
                            "The given MamQueryResult is from the local entity (user) MAM archive, whereas this MamManager is responsible for '"
                                            + archiveAddress + '\'');
        }
    }

    /**
     * Returns true if Message Archive Management is supported by the server.
     * 
     * @return true if Message Archive Management is supported by the server.
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws InterruptedException
     */
    public boolean isSupportedByServer()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).serverSupportsFeature(MamElements.NAMESPACE);
    }

    private static DataForm getNewMamForm() {
        FormField field = new FormField(FormField.FORM_TYPE);
        field.setType(FormField.Type.hidden);
        field.addValue(MamElements.NAMESPACE);
        DataForm form = new DataForm(DataForm.Type.submit);
        form.addField(field);
        return form;
    }

    /**
     * Get the preferences stored in the server.
     * 
     * @return the MAM preferences result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamPrefsResult retrieveArchivingPreferences() throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        MamPrefsIQ mamPrefIQ = new MamPrefsIQ();
        return queryMamPrefs(mamPrefIQ);
    }

    /**
     * Update the preferences in the server.
     * 
     * @param alwaysJids
     *            is the list of JIDs that should always have messages to/from
     *            archived in the user's store
     * @param neverJids
     *            is the list of JIDs that should never have messages to/from
     *            archived in the user's store
     * @param defaultBehavior
     *            can be "roster", "always", "never" (see XEP-0313)
     * @return the MAM preferences result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamPrefsResult updateArchivingPreferences(List<Jid> alwaysJids, List<Jid> neverJids, DefaultBehavior defaultBehavior)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException,
            NotLoggedInException {
        Objects.requireNonNull(defaultBehavior, "Default behavior must be set");
        MamPrefsIQ mamPrefIQ = new MamPrefsIQ(alwaysJids, neverJids, defaultBehavior);
        return queryMamPrefs(mamPrefIQ);
    }

    /**
     * MAM preferences result class.
     *
     */
    public final static class MamPrefsResult {
        public final MamPrefsIQ mamPrefs;
        public final DataForm form;

        private MamPrefsResult(MamPrefsIQ mamPrefs, DataForm form) {
            this.mamPrefs = mamPrefs;
            this.form = form;
        }
    }

    private MamPrefsResult queryMamPrefs(MamPrefsIQ mamPrefsIQ) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        final XMPPConnection connection = getAuthenticatedConnectionOrThrow();

        MamPrefsIQ mamPrefsResultIQ = connection.createStanzaCollectorAndSend(mamPrefsIQ).nextResultOrThrow();

        return new MamPrefsResult(mamPrefsResultIQ, DataForm.from(mamPrefsIQ));
    }

}
