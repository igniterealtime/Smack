/**
 * 
 */
package org.jivesoftware.smackx.filetransfer;

import org.jivesoftware.smackx.packet.StreamInitiation;

/**
 * A request to send a file recieved from another user.
 * 
 * @author Alexander Wenckus
 * 
 */
public class FileTransferRequest {
	private final StreamInitiation streamInitiation;

	private final FileTransferManager manager;

	/**
	 * A recieve request is constructed from the Stream Initiation request
	 * received from the initator.
	 * 
	 * @param manager
	 *            The manager handling this file transfer
	 * 
	 * @param si
	 *            The Stream initiaton recieved from the initiator.
	 */
	public FileTransferRequest(FileTransferManager manager, StreamInitiation si) {
		this.streamInitiation = si;
		this.manager = manager;
	}

	/**
	 * Returns the name of the file.
	 * 
	 * @return Returns the name of the file.
	 */
	public String getFileName() {
		return streamInitiation.getFile().getName();
	}

	/**
	 * Returns the size in bytes of the file.
	 * 
	 * @return Returns the size in bytes of the file.
	 */
	public long getFileSize() {
		return streamInitiation.getFile().getSize();
	}

	/**
	 * Returns the description of the file provided by the requestor.
	 * 
	 * @return Returns the description of the file provided by the requestor.
	 */
	public String getDescription() {
		return streamInitiation.getFile().getDesc();
	}

	/**
	 * Returns the mime-type of the file.
	 * 
	 * @return Returns the mime-type of the file.
	 */
	public String getMimeType() {
		return streamInitiation.getMimeType();
	}

	/**
	 * Returns the fully-qualified jabber ID of the user that requested this
	 * file transfer.
	 * 
	 * @return Returns the fully-qualified jabber ID of the user that requested
	 *         this file transfer.
	 */
	public String getRequestor() {
		return streamInitiation.getFrom();
	}

	/**
	 * Returns the stream ID that uniquely identifies this file transfer.
	 * 
	 * @return Returns the stream ID that uniquely identifies this file
	 *         transfer.
	 */
	public String getStreamID() {
		return streamInitiation.getSessionID();
	}

	/**
	 * Returns the stream initiation packet that was sent by the requestor which
	 * contains the parameters of the file transfer being transfer and also the
	 * methods available to transfer the file.
	 * 
	 * @return Returns the stream initiation packet that was sent by the
	 *         requestor which contains the parameters of the file transfer
	 *         being transfer and also the methods available to transfer the
	 *         file.
	 */
	protected StreamInitiation getStreamInitiation() {
		return streamInitiation;
	}

	/**
	 * Accepts this file transfer and creates the incoming file transfer.
	 * 
	 * @return Returns the <b><i>IncomingFileTransfer</b></i> on which the
	 *         file transfer can be carried out.
	 */
	public IncomingFileTransfer accept() {
		return manager.createIncomingFileTransfer(this);
	}

	/**
	 * Rejects the file transfer request.
	 */
	public void reject() {
		manager.rejectIncomingFileTransfer(this);
	}

}
