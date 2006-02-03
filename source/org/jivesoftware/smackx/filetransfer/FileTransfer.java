/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jivesoftware.smack.XMPPException;

/**
 * Contains the generic file information and progress related to a particular
 * file transfer.
 * 
 * @author Alexander Wenckus
 * 
 */
public abstract class FileTransfer {

	private String fileName;

	private String filePath;

	private long fileSize;

	private String peer;

	private org.jivesoftware.smackx.filetransfer.FileTransfer.Status status;

	protected FileTransferNegotiator negotiator;

	protected String streamID;

	protected long amountWritten = -1;

	private Error error;

	private Exception exception;

	protected FileTransfer(String peer, String streamID,
			FileTransferNegotiator negotiator) {
		this.peer = peer;
		this.streamID = streamID;
		this.negotiator = negotiator;
	}

	protected void setFileInfo(String fileName, long fileSize) {
		this.fileName = fileName;
		this.fileSize = fileSize;
	}

	protected void setFileInfo(String path, String fileName, long fileSize) {
		this.filePath = path;
		this.fileName = fileName;
		this.fileSize = fileSize;
	}

	/**
	 * Returns the size of the file being transfered.
	 * 
	 * @return Returns the size of the file being transfered.
	 */
	public long getFileSize() {
		return fileSize;
	}

	/**
	 * Returns the name of the file being transfered.
	 * 
	 * @return Returns the name of the file being transfered.
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * Returns the local path of the file.
	 * 
	 * @return Returns the local path of the file.
	 */
	public String getFilePath() {
		return filePath;
	}

	/**
	 * Returns the JID of the peer for this file transfer.
	 * 
	 * @return Returns the JID of the peer for this file transfer.
	 */
	public String getPeer() {
		return peer;
	}

	/**
	 * Returns the progress of the file transfer as a number between 0 and 1.
	 * 
	 * @return Returns the progress of the file transfer as a number between 0
	 *         and 1.
	 */
	public double getProgress() {
		return 0;
	}

	/**
	 * Returns true if the transfer has been cancled, if it has stopped because
	 * of a an error, or the transfer completed succesfully.
	 * 
	 * @return Returns true if the transfer has been cancled, if it has stopped
	 *         because of a an error, or the transfer completed succesfully.
	 */
	public boolean isDone() {
		return status == Status.CANCLED || status == Status.ERROR
				|| status == Status.COMPLETE;
	}

	/**
	 * Retuns the current status of the file transfer.
	 * 
	 * @return Retuns the current status of the file transfer.
	 */
	public Status getStatus() {
		return status;
	}

	protected void setError(Error type) {
		this.error = type;
	}

	/**
	 * When {@link #getStatus()} returns that there was an {@link Status#ERROR}
	 * during the transfer, the type of error can be retrieved through this
	 * method.
	 * 
	 * @return Returns the type of error that occured if one has occured.
	 */
	public Error getError() {
		return error;
	}

	/**
	 * If an exception occurs asynchronously it will be stored for later
	 * retrival. If there is an error there maybe an exception set.
	 * 
	 * @return The exception that occured or null if there was no exception.
	 * @see #getError()
	 */
	public Exception getException() {
		return exception;
	}

	/**
	 * Cancels the file transfer.
	 */
	public abstract void cancel();

	protected void setException(Exception exception) {
		this.exception = exception;
	}

	protected final void setStatus(Status status) {
		this.status = status;
	}

	protected void writeToStream(final InputStream in, final OutputStream out)
			throws XMPPException {
		final byte[] b = new byte[1000];
		int count = 0;
		amountWritten = 0;
		try {
			count = in.read(b);
		} catch (IOException e) {
			throw new XMPPException("error reading from input stream", e);
		}
		while (count != -1 && !getStatus().equals(Status.CANCLED)) {
			if (getStatus().equals(Status.CANCLED)) {
				return;
			}

			// write to the output stream
			try {
				out.write(b, 0, count);
			} catch (IOException e) {
				throw new XMPPException("error writing to output stream", e);
			}

			amountWritten += count;

			// read more bytes from the input stream
			try {
				count = in.read(b);
			} catch (IOException e) {
				throw new XMPPException("error reading from input stream", e);
			}
		}

		// the connection was likely terminated abrubtly if these are not
		// equal
		if (!getStatus().equals(Status.CANCLED) && getError() == Error.NONE
				&& amountWritten != fileSize) {
			this.error = Error.CONNECTION;
		}
	}

	/**
	 * A class to represent the current status of the file transfer.
	 * 
	 * @author Alexander Wenckus
	 * 
	 */
	public static class Status {
		/**
		 * An error occured during the transfer.
		 * 
		 * @see FileTransfer#getError()
		 */
		public static final Status ERROR = new Status();

		/**
		 * The file transfer is being negotiated with the peer. The party
		 * recieving the file has the option to accept or refuse a file transfer
		 * request. If they accept, then the process of stream negotiation will
		 * begin. If they refuse the file will not be transfered.
		 * 
		 * @see #NEGOTIATING_STREAM
		 */
		public static final Status NEGOTIATING_TRANSFER = new Status();

		/**
		 * The peer has refused the file transfer request halting the file
		 * transfer negotiation process.
		 */
		public static final Status REFUSED = new Status();

		/**
		 * The stream to transfer the file is being negotiated over the chosen
		 * stream type. After the stream negotiating process is complete the
		 * status becomes negotiated.
		 * 
		 * @see #NEGOTIATED
		 */
		public static final Status NEGOTIATING_STREAM = new Status();

		/**
		 * After the stream negotitation has completed the intermediate state
		 * between the time when the negotiation is finished and the actual
		 * transfer begins.
		 */
		public static final Status NEGOTIATED = new Status();

		/**
		 * The transfer is in progress.
		 * 
		 * @see FileTransfer#getProgress()
		 */
		public static final Status IN_PROGRESS = new Status();

		/**
		 * The transfer has completed successfully.
		 */
		public static final Status COMPLETE = new Status();

		/**
		 * The file transfer was canceled
		 */
		public static final Status CANCLED = new Status();
	}

	public static class Error {
		/**
		 * No error
		 */
		public static final Error NONE = new Error("No error");

		/**
		 * The peer did not find any of the provided stream mechanisms
		 * acceptable.
		 */
		public static final Error NOT_ACCEPTABLE = new Error(
				"The peer did not find any of the provided stream mechanisms acceptable.");

		/**
		 * The provided file to transfer does not exist or could not be read.
		 */
		public static final Error BAD_FILE = new Error(
				"The provided file to transfer does not exist or could not be read.");

		/**
		 * The remote user did not respond or the connection timed out.
		 */
		public static final Error NO_RESPONSE = new Error(
				"The remote user did not respond or the connection timed out.");

		/**
		 * An error occured over the socket connected to send the file.
		 */
		public static final Error CONNECTION = new Error(
				"An error occured over the socket connected to send the file.");

		/**
		 * An error occured while sending or recieving the file
		 */
		protected static final Error STREAM = new Error(
				"An error occured while sending or recieving the file");

		private final String msg;

		private Error(String msg) {
			this.msg = msg;
		}

		/**
		 * Returns a String representation of this error.
		 * 
		 * @return Returns a String representation of this error.
		 */
		public String getMessage() {
			return msg;
		}

		public String toString() {
			return msg;
		}
	}

}
