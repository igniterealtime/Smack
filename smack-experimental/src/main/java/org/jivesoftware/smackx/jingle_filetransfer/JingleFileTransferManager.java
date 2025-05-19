/**
 *
 * Copyright 2017-2024 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_filetransfer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.jingle.JingleDescriptionManager;
import org.jivesoftware.smackx.jingle.JingleHandler;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.JingleTransportMethodManager;
import org.jivesoftware.smackx.jingle.component.JingleContentImpl;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle.transports.JingleTransportManager;
import org.jivesoftware.smackx.jingle_filetransfer.adapter.JingleFileTransferAdapter;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFile;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFileTransferImpl;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleIncomingFileRequest;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleOutgoingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleOutgoingFileRequest;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileRequestController;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingFileOfferListener;
import org.jivesoftware.smackx.jingle_filetransfer.listener.IncomingFileRequestListener;
import org.jivesoftware.smackx.jingle_filetransfer.provider.JingleFileTransferProvider;

import org.jxmpp.jid.FullJid;

/**
 * Manager for JingleFileTransfer (XEP-0234).
 * @see <a href="https://xmpp.org/extensions/xep-0234.html">XEP-0234: Jingle File Transfer 0.19.1 (2019-06-19)</a>
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public final class JingleFileTransferManager extends Manager implements JingleDescriptionManager, JingleHandler {
    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferManager.class.getName());

    private static final WeakHashMap<XMPPConnection, JingleFileTransferManager> INSTANCES = new WeakHashMap<>();

    private final List<IncomingFileOfferListener> offerListeners = Collections.synchronizedList(new ArrayList<>());

    private final List<IncomingFileRequestListener> requestListeners = Collections.synchronizedList(new ArrayList<>());

    private final XMPPConnection mConnection;

    private JingleFileTransferManager(XMPPConnection connection) {
        super(connection);
        mConnection = connection;
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(getNamespace());

        JingleContentProviderManager.addJingleContentDescriptionProvider(getNamespace(), new JingleFileTransferProvider());
        JingleContentProviderManager.addJingleDescriptionAdapter(new JingleFileTransferAdapter());
        JingleContentProviderManager.addJingleDescriptionManager(this);

        JingleManager jingleManager = JingleManager.getInstanceFor(connection);
        jingleManager.registerDescriptionHandler(getNamespace(), this);
    }

    public static synchronized JingleFileTransferManager getInstanceFor(XMPPConnection connection) {
        JingleFileTransferManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new JingleFileTransferManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    /**
     * Register a new JingleSessionHandler with JingleManager when a new session-initiate is received.
     *
     * @param jingle Jingle session-initiate
     * @return IQ.Result
     */
    @Override
    public IQ handleJingleRequest(Jingle jingle) {
        // see <a href="https://xmpp.org/extensions/xep-0166.html#def">XEP-0166 Jingle#7. Formal Definition</a>
        // conversations excludes initiator attribute in session-initiate
        FullJid initiator = jingle.getInitiator();
        if (initiator == null) {
            initiator = jingle.getFrom().asEntityFullJidIfPossible();
        }

        JingleSessionImpl session = new JingleSessionImpl(mConnection, initiator, jingle);
        return session.handleJingleSessionRequest(jingle);
    }

    public OutgoingFileOfferController sendFile(File file, FullJid recipient)
            throws SmackException.NotConnectedException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NoResponseException, SmackException.FeatureNotSupportedException, IOException, NoSuchAlgorithmException {
        return sendFile(file, JingleFile.fromFile(file, null, null, null), recipient);
    }

    public OutgoingFileOfferController sendFile(File file, JingleFile metadata, FullJid recipient) throws SmackException.FeatureNotSupportedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, FileNotFoundException {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File MUST NOT be null and MUST exist.");
        }

        throwIfRecipientLacksSupport(recipient);

        JingleSessionImpl session = new JingleSessionImpl(mConnection, recipient);

        JingleContentImpl content
                = new JingleContentImpl(mConnection, JingleContent.Creator.initiator, JingleContent.Senders.initiator);
        session.addContentImpl(content);

        JingleOutgoingFileOffer outgoingFileOffer = new JingleOutgoingFileOffer(session, file, metadata);
        content.setDescription(outgoingFileOffer);

        JingleTransportManager<?> transportManager = JingleTransportMethodManager.getBestAvailableTransportManager(mConnection);
        JingleTransport<?> transport = transportManager.createTransportForInitiator(content);
        content.setTransport(transport);

        session.sendInitiate(connection());
        return outgoingFileOffer;
    }

    public OutgoingFileOfferController sendStream(final InputStream inputStream, JingleFile metadata, FullJid recipient)
            throws XMPPException.XMPPErrorException, SmackException.FeatureNotSupportedException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {

        throwIfRecipientLacksSupport(recipient);

        JingleSessionImpl session = new JingleSessionImpl(mConnection, recipient);
        JingleContentImpl content
                = new JingleContentImpl(mConnection, JingleContent.Creator.initiator, JingleContent.Senders.initiator);
        session.addContentImpl(content);

        JingleOutgoingFileOffer outgoingFileOffer = new JingleOutgoingFileOffer(session, inputStream, metadata);
        content.setDescription(outgoingFileOffer);

        JingleTransportManager<?> transportManager = JingleTransportMethodManager.getBestAvailableTransportManager(mConnection);
        JingleTransport<?> transport = transportManager.createTransportForInitiator(content);
        content.setTransport(transport);

        session.sendInitiate(connection());
        return outgoingFileOffer;
    }

    public OutgoingFileRequestController requestFile(JingleSession jingleSession, JingleFile metadata, FullJid from) {
        JingleOutgoingFileRequest request = new JingleOutgoingFileRequest(jingleSession, metadata);

        // TODO at some point.
        return request;
    }

    public void addIncomingFileOfferListener(IncomingFileOfferListener listener) {
        offerListeners.add(listener);
    }

    public void removeIncomingFileOfferListener(IncomingFileOfferListener listener) {
        offerListeners.remove(listener);
    }

    public void notifyIncomingFileOfferListeners(JingleIncomingFileOffer offer) {
        LOGGER.log(Level.INFO, "Incoming File transfer: [" + offer.getNamespace() + ", "
                + offer.getParent().getTransport().getNamespace() + ", "
                + (offer.getParent().getSecurity() != null ? offer.getParent().getSecurity().getNamespace() : "") + "]");
        for (IncomingFileOfferListener l : offerListeners) {
            l.onIncomingFileOffer(offer);
        }
    }

    public void addIncomingFileRequestListener(IncomingFileRequestListener listener) {
        requestListeners.add(listener);
    }

    public void removeIncomingFileRequestListener(IncomingFileRequestListener listener) {
        requestListeners.remove(listener);
    }

    public void notifyIncomingFileRequestListeners(JingleIncomingFileRequest request) {
        for (IncomingFileRequestListener l : requestListeners) {
            l.onIncomingFileRequest(request);
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransferImpl.NAMESPACE;
    }

    private void notifyTransfer(JingleFileTransferImpl transfer) {
        if (transfer.isOffer()) {
            notifyIncomingFileOfferListeners((JingleIncomingFileOffer) transfer);
        } else {
            notifyIncomingFileRequestListeners((JingleIncomingFileRequest) transfer);
        }
    }

    @Override
    public void notifySessionInitiate(JingleSessionImpl session) {
        JingleContentImpl content = session.getSoleContentOrThrow();
        notifyTransfer((JingleFileTransferImpl) content.getDescription());
    }

    @Override
    public void notifyContentAdd(JingleSessionImpl session, JingleContentImpl content) {
        notifyTransfer((JingleFileTransferImpl) content.getDescription());
    }

    private void throwIfRecipientLacksSupport(FullJid recipient) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, SmackException.FeatureNotSupportedException {
        if (!ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(recipient, getNamespace())) {
            throw new SmackException.FeatureNotSupportedException(getNamespace(), recipient);
        }
    }
}
