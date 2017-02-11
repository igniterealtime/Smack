/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.filetransfer;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jxmpp.jid.EntityFullJid;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The file transfer manager class handles the sending and recieving of files.
 * To send a file invoke the {@link #createOutgoingFileTransfer(EntityFullJid)} method.
 * <p>
 * And to recieve a file add a file transfer listener to the manager. The
 * listener will notify you when there is a new file transfer request. To create
 * the {@link IncomingFileTransfer} object accept the transfer, or, if the
 * transfer is not desirable reject it.
 * 
 * @author Alexander Wenckus
 * 
 */
public final class FileTransferManager extends Manager {

    private static final Map<XMPPConnection, FileTransferManager> INSTANCES = new WeakHashMap<XMPPConnection, FileTransferManager>();

    public static synchronized FileTransferManager getInstanceFor(XMPPConnection connection) {
        FileTransferManager fileTransferManager = INSTANCES.get(connection);
        if (fileTransferManager == null) {
            fileTransferManager = new FileTransferManager(connection);
            INSTANCES.put(connection, fileTransferManager);
        }
        return fileTransferManager;
    }

    private final FileTransferNegotiator fileTransferNegotiator;

    private final List<FileTransferListener> listeners = new CopyOnWriteArrayList<FileTransferListener>();

    /**
     * Creates a file transfer manager to initiate and receive file transfers.
     * 
     * @param connection
     *            The XMPPConnection that the file transfers will use.
     */
    private FileTransferManager(XMPPConnection connection) {
        super(connection);
        this.fileTransferNegotiator = FileTransferNegotiator
                .getInstanceFor(connection);
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(StreamInitiation.ELEMENT,
                        StreamInitiation.NAMESPACE, IQ.Type.set, Mode.async) {
            @Override
            public IQ handleIQRequest(IQ packet) {
                StreamInitiation si = (StreamInitiation) packet;
                final FileTransferRequest request = new FileTransferRequest(FileTransferManager.this, si);
                for (final FileTransferListener listener : listeners) {
                            listener.fileTransferRequest(request);
                }
                return null;
            }
        });
    }

    /**
     * Add a file transfer listener to listen to incoming file transfer
     * requests.
     * 
     * @param li
     *            The listener
     * @see #removeFileTransferListener(FileTransferListener)
     * @see FileTransferListener
     */
    public void addFileTransferListener(final FileTransferListener li) {
        listeners.add(li);
    }

    /**
     * Removes a file transfer listener.
     * 
     * @param li
     *            The file transfer listener to be removed
     * @see FileTransferListener
     */
    public void removeFileTransferListener(final FileTransferListener li) {
        listeners.remove(li);
    }

    /**
     * Creates an OutgoingFileTransfer to send a file to another user.
     * 
     * @param userID
     *            The fully qualified jabber ID (i.e. full JID) with resource of the user to
     *            send the file to.
     * @return The send file object on which the negotiated transfer can be run.
     * @exception IllegalArgumentException if userID is null or not a full JID
     */
    public OutgoingFileTransfer createOutgoingFileTransfer(EntityFullJid userID) {
        // We need to create outgoing file transfers with a full JID since this method will later
        // use XEP-0095 to negotiate the stream. This is done with IQ stanzas that need to be addressed to a full JID
        // in order to reach an client entity.
        if (userID == null) {
            throw new IllegalArgumentException("userID was null");
        }

        return new OutgoingFileTransfer(connection().getUser(), userID,
                FileTransferNegotiator.getNextStreamID(),
                fileTransferNegotiator);
    }

    /**
     * When the file transfer request is acceptable, this method should be
     * invoked. It will create an IncomingFileTransfer which allows the
     * transmission of the file to procede.
     * 
     * @param request
     *            The remote request that is being accepted.
     * @return The IncomingFileTransfer which manages the download of the file
     *         from the transfer initiator.
     */
    protected IncomingFileTransfer createIncomingFileTransfer(
            FileTransferRequest request) {
        if (request == null) {
            throw new NullPointerException("RecieveRequest cannot be null");
        }

        IncomingFileTransfer transfer = new IncomingFileTransfer(request,
                fileTransferNegotiator);
        transfer.setFileInfo(request.getFileName(), request.getFileSize());

        return transfer;
    }

    /**
     * Reject an incoming file transfer.
     * <p>
     * Specified in XEP-95 4.2 and 3.2 Example 8
     * </p>
     * @param request
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    protected void rejectIncomingFileTransfer(FileTransferRequest request) throws NotConnectedException, InterruptedException {
        StreamInitiation initiation = request.getStreamInitiation();

        // Reject as specified in XEP-95 4.2. Note that this is not to be confused with the Socks 5
        // Bytestream rejection as specified in XEP-65 5.3.1 Example 13, which says that
        // 'not-acceptable' should be returned. This is done by Smack in
        // Socks5BytestreamManager.replyRejectPacket(IQ).
        IQ rejection = IQ.createErrorResponse(initiation, XMPPError.getBuilder(
                        XMPPError.Condition.forbidden));
        connection().sendStanza(rejection);
    }
}
