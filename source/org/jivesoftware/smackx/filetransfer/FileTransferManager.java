/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.filetransfer;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.StreamInitiation;

import java.util.ArrayList;
import java.util.List;

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
public class FileTransferManager {

	private final FileTransferNegotiator fileTransferNegotiator;

	private List<FileTransferListener> listeners;

	private Connection connection;

	/**
	 * Creates a file transfer manager to initiate and receive file transfers.
	 * 
	 * @param connection
	 *            The Connection that the file transfers will use.
	 */
	public FileTransferManager(Connection connection) {
		this.connection = connection;
		this.fileTransferNegotiator = FileTransferNegotiator
				.getInstanceFor(connection);
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
		if (listeners == null) {
			initListeners();
		}
		synchronized (this.listeners) {
			listeners.add(li);
		}
	}

	private void initListeners() {
		listeners = new ArrayList<FileTransferListener>();

		connection.addPacketListener(new PacketListener() {
			public void processPacket(Packet packet) {
				fireNewRequest((StreamInitiation) packet);
			}
		}, new AndFilter(new PacketTypeFilter(StreamInitiation.class),
				new IQTypeFilter(IQ.Type.SET)));
	}

	protected void fireNewRequest(StreamInitiation initiation) {
		FileTransferListener[] listeners = null;
		synchronized (this.listeners) {
			listeners = new FileTransferListener[this.listeners.size()];
			this.listeners.toArray(listeners);
		}
		FileTransferRequest request = new FileTransferRequest(this, initiation);
		for (int i = 0; i < listeners.length; i++) {
			listeners[i].fileTransferRequest(request);
		}
	}

	/**
	 * Removes a file transfer listener.
	 * 
	 * @param li
	 *            The file transfer listener to be removed
	 * @see FileTransferListener
	 */
	public void removeFileTransferListener(final FileTransferListener li) {
		if (listeners == null) {
			return;
		}
		synchronized (this.listeners) {
			listeners.remove(li);
		}
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
        else if (!StringUtils.isFullJID(userID)) {
            throw new IllegalArgumentException("The provided user id was not a full JID (i.e. with resource part)");
        }

		return new OutgoingFileTransfer(connection.getUser(), userID,
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

	protected void rejectIncomingFileTransfer(FileTransferRequest request) {
		StreamInitiation initiation = request.getStreamInitiation();

		IQ rejection = FileTransferNegotiator.createIQ(
				initiation.getPacketID(), initiation.getFrom(), initiation
						.getTo(), IQ.Type.ERROR);
		rejection.setError(new XMPPError(XMPPError.Condition.no_acceptable));
		connection.sendPacket(rejection);
	}
}
