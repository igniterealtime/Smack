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
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jxmpp.util.XmppStringUtils;

import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The file transfer manager class handles the sending and recieving of files.
 * To send a file invoke the {@link #createOutgoingFileTransfer(String)} method.
 * <p>
 * And to recieve a file add a file transfer listener to the manager. The
 * listener will notify you when there is a new file transfer request. To create
 * the {@link IncomingFileTransfer} object accept the transfer, or, if the
 * transfer is not desirable reject it.
 * 
 * @author Alexander Wenckus
 * 
 */
public class FileTransferManager extends Manager {

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
        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                StreamInitiation si = (StreamInitiation) packet;
                FileTransferRequest request = new FileTransferRequest(FileTransferManager.this, si);
                for (FileTransferListener listener : listeners) {
                    listener.fileTransferRequest(request);
                }
            }
        }, new AndFilter(new PacketTypeFilter(StreamInitiation.class), IQTypeFilter.SET));
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
	public OutgoingFileTransfer createOutgoingFileTransfer(String userID) {
        if (userID == null) {
            throw new IllegalArgumentException("userID was null");
        }
        // We need to create outgoing file transfers with a full JID since this method will later
        // use XEP-0095 to negotiate the stream. This is done with IQ stanzas that need to be addressed to a full JID
        // in order to reach an client entity.
        else if (!XmppStringUtils.isFullJID(userID)) {
            throw new IllegalArgumentException("The provided user id was not a full JID (i.e. with resource part)");
        }

		return new OutgoingFileTransfer(connection().getUser(), userID,
				fileTransferNegotiator.getNextStreamID(),
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

	protected void rejectIncomingFileTransfer(FileTransferRequest request) throws NotConnectedException {
		StreamInitiation initiation = request.getStreamInitiation();

		IQ rejection = IQ.createErrorResponse(initiation, new XMPPError(XMPPError.Condition.no_acceptable));
		connection().sendPacket(rejection);
	}
}
