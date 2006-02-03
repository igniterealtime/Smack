/*
 * Created on Jun 23, 2005
 */
package org.jivesoftware.smackx.filetransfer;

/**
 * File transfers can cause several events to be raised. These events can be
 * monitored through this interface.
 * 
 * @author Alexander Wenckus
 */
public interface FileTransferListener {
	/**
	 * A request to send a file has been recieved from another user.
	 * 
	 * @param request
	 *            The request from the other user.
	 */
	public void fileTransferRequest(final FileTransferRequest request);
}
