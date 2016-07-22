/**
 *
 * Copyright © 2016 Florian Schmaus and Fernando Ramirez
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
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.element.MamElements;
import org.jivesoftware.smackx.mam.element.MamFinIQ;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;
import org.jivesoftware.smackx.mam.filter.MamResultFilter;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.XmppDateTime;

/**
 * Message Archive Management Manager class.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez and Florian Schmaus
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

    private static final Map<XMPPConnection, MamManager> INSTANCES = new WeakHashMap<>();

    /**
     * Get the singleton instance of MamManager.
     * 
     * @param connection
     * @return the instance of MamManager
     */
    public static synchronized MamManager getInstanceFor(XMPPConnection connection) {
        MamManager mamManager = INSTANCES.get(connection);

        if (mamManager == null) {
            mamManager = new MamManager(connection);
            INSTANCES.put(connection, mamManager);
        }

        return mamManager;
    }

    private MamManager(XMPPConnection connection) {
        super(connection);
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
        return queryArchive(max, null, null, null, null);
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
        return queryArchive(null, null, null, withJid, null);
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
        return queryArchive(null, start, end, null, null);
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
        return queryArchive(null, null, null, null, additionalFields);
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
        return queryArchive(null, start, null, null, null);
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
        return queryArchive(null, null, end, null, null);
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
        DataForm dataForm = null;
        String queryId = UUID.randomUUID().toString();

        if (start != null || end != null || withJid != null || additionalFields != null) {
            dataForm = getNewMamForm();
            addStart(start, dataForm);
            addEnd(end, dataForm);
            addWithJid(withJid, dataForm);
            addAdditionalFields(additionalFields, dataForm);
        }

        MamQueryIQ mamQueryIQ = prepareMamQueryIQSet(dataForm, queryId);
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

    private void preparePageQuery(MamQueryIQ mamQueryIQ, RSMSet rsmSet) {
        mamQueryIQ.setType(IQ.Type.set);
        mamQueryIQ.addExtension(rsmSet);
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
        MamQueryIQ mamQueryIQ = new MamQueryIQ(UUID.randomUUID().toString(), dataForm);
        preparePageQuery(mamQueryIQ, rsmSet);
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
        return page(mamQueryResult.form, requestRsmSet);
    }

    /**
     * Obtain page before the first message saved (specific chat).
     *
     * @param chatJid
     * @param firstMessageId
     * @param max
     * @return the MAM query result
     * @throws XMPPErrorException
     * @throws NotLoggedInException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    public MamQueryResult pageBefore(Jid chatJid, String firstMessageId, int max) throws XMPPErrorException,
            NotLoggedInException, NotConnectedException, InterruptedException, NoResponseException {
        RSMSet rsmSet = new RSMSet(null, firstMessageId, -1, -1, null, max, null, -1);
        DataForm dataForm = getNewMamForm();
        addWithJid(chatJid, dataForm);
        return page(dataForm, rsmSet);
    }

    /**
     * Obtain page after the last message saved (specific chat).
     *
     * @param chatJid
     * @param lastMessageId
     * @param max
     * @return the MAM query result
     * @throws XMPPErrorException
     * @throws NotLoggedInException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NoResponseException
     */
    public MamQueryResult pageAfter(Jid chatJid, String lastMessageId, int max) throws XMPPErrorException,
            NotLoggedInException, NotConnectedException, InterruptedException, NoResponseException {
        RSMSet rsmSet = new RSMSet(lastMessageId, null, -1, -1, null, max, null, -1);
        DataForm dataForm = getNewMamForm();
        addWithJid(chatJid, dataForm);
        return page(dataForm, rsmSet);
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
        String queryId = UUID.randomUUID().toString();
        MamQueryIQ mamQueryIQ = prepareMamQueryIQGet(queryId);
        return queryFormFields(mamQueryIQ);
    }

    private MamQueryIQ prepareMamQueryIQSet(DataForm dataForm, String queryId) {
        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, dataForm);
        mamQueryIQ.setType(IQ.Type.set);
        return mamQueryIQ;
    }

    private MamQueryIQ prepareMamQueryIQGet(String queryId) {
        MamQueryIQ mamQueryIQ = new MamQueryIQ(queryId, null);
        mamQueryIQ.setType(IQ.Type.get);
        return mamQueryIQ;
    }

    private MamQueryResult queryArchive(MamQueryIQ mamQueryIq) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        final XMPPConnection connection = getAuthenticatedConnectionOrThrow();
        MamFinIQ mamFinIQ = null;

        PacketCollector mamFinIQCollector = connection.createPacketCollector(new IQReplyFilter(mamQueryIq, connection));

        PacketCollector.Configuration resultCollectorConfiguration = PacketCollector.newConfiguration()
                .setStanzaFilter(new MamResultFilter(mamQueryIq)).setCollectorToReset(mamFinIQCollector);
        PacketCollector resultCollector = connection.createPacketCollector(resultCollectorConfiguration);

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

        return new MamQueryResult(forwardedMessages, mamFinIQ, DataForm.from(mamQueryIq));
    }

    /**
     * MAM query result class.
     *
     */
    public final static class MamQueryResult {
        public final List<Forwarded> forwardedMessages;
        public final MamFinIQ mamFin;
        private final DataForm form;

        private MamQueryResult(List<Forwarded> forwardedMessages, MamFinIQ mamFin, DataForm form) {
            this.forwardedMessages = forwardedMessages;
            this.mamFin = mamFin;
            this.form = form;
        }
    }

    private List<FormField> queryFormFields(MamQueryIQ mamQueryIq) throws NoResponseException, XMPPErrorException,
            NotConnectedException, InterruptedException, NotLoggedInException {
        final XMPPConnection connection = connection();
        MamQueryIQ mamResponseQueryIQ = null;
        PacketCollector mamResponseQueryIQCollector = connection
                .createPacketCollector(new IQReplyFilter(mamQueryIq, connection));

        try {
            connection.sendStanza(mamQueryIq);
            mamResponseQueryIQ = mamResponseQueryIQCollector.nextResultOrThrow();
        } finally {
            mamResponseQueryIQCollector.cancel();
        }

        return mamResponseQueryIQ.getDataForm().getFields();
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

    private DataForm getNewMamForm() {
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
        MamPrefsIQ mamPrefIQ = prepareRetrievePreferencesStanza();
        return queryMamPrefs(mamPrefIQ);
    }

    private MamPrefsIQ prepareRetrievePreferencesStanza() {
        MamPrefsIQ mamPrefIQ = new MamPrefsIQ(Type.get, null, null, null);
        return mamPrefIQ;
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
     * @param defaultField
     *            can be "roster", "always", "never" (look at the XEP-0313
     *            documentation)
     * @return the MAM preferences result
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws NotLoggedInException
     */
    public MamPrefsResult updateArchivingPreferences(List<Jid> alwaysJids, List<Jid> neverJids, String defaultField)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException,
            NotLoggedInException {
        MamPrefsIQ mamPrefIQ = prepareUpdatePreferencesStanza(alwaysJids, neverJids, defaultField);
        return queryMamPrefs(mamPrefIQ);
    }

    private MamPrefsIQ prepareUpdatePreferencesStanza(List<Jid> alwaysJids, List<Jid> neverJids, String defaultField) {
        MamPrefsIQ mamPrefIQ = new MamPrefsIQ(Type.set, alwaysJids, neverJids, defaultField);
        return mamPrefIQ;
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
        MamPrefsIQ mamPrefsResultIQ = null;
        PacketCollector prefsResultIQCollector = connection
                .createPacketCollector(new IQReplyFilter(mamPrefsIQ, connection));

        try {
            connection.sendStanza(mamPrefsIQ);
            mamPrefsResultIQ = prefsResultIQCollector.nextResultOrThrow();
        } finally {
            prefsResultIQCollector.cancel();
        }

        return new MamPrefsResult(mamPrefsResultIQ, DataForm.from(mamPrefsIQ));
    }

}
