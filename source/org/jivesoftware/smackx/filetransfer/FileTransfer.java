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

import org.jivesoftware.smack.XMPPException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

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

	private Status status = Status.initial;

    private final Object statusMonitor = new Object();

	protected FileTransferNegotiator negotiator;

	protected String streamID;

	protected long amountWritten = -1;

	private Error error;

	private Exception exception;

    /**
     * Buffer size between input and output
     */
    private static final int BUFFER_SIZE = 8192;

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
        if (amountWritten <= 0 || fileSize <= 0) {
            return 0;
        }
        return (double) amountWritten / (double) fileSize;
	}

	/**
	 * Returns true if the transfer has been cancelled, if it has stopped because
	 * of a an error, or the transfer completed successfully.
	 *
	 * @return Returns true if the transfer has been cancelled, if it has stopped
	 *         because of a an error, or the transfer completed successfully.
	 */
	public boolean isDone() {
		return status == Status.cancelled || status == Status.error
				|| status == Status.complete || status == Status.refused;
	}

	/**
	 * Returns the current status of the file transfer.
	 *
	 * @return Returns the current status of the file transfer.
	 */
	public Status getStatus() {
		return status;
	}

	protected void setError(Error type) {
		this.error = type;
	}

	/**
	 * When {@link #getStatus()} returns that there was an {@link Status#error}
	 * during the transfer, the type of error can be retrieved through this
	 * method.
	 *
	 * @return Returns the type of error that occurred if one has occurred.
	 */
	public Error getError() {
		return error;
	}

	/**
	 * If an exception occurs asynchronously it will be stored for later
	 * retrieval. If there is an error there maybe an exception set.
	 *
	 * @return The exception that occurred or null if there was no exception.
	 * @see #getError()
	 */
	public Exception getException() {
		return exception;
	}

    public String getStreamID() {
        return streamID;
    }

	/**
	 * Cancels the file transfer.
	 */
	public abstract void cancel();

	protected void setException(Exception exception) {
		this.exception = exception;
	}

	protected void setStatus(Status status) {
        synchronized (statusMonitor) {
		    this.status = status;
	    }
    }

    protected boolean updateStatus(Status oldStatus, Status newStatus) {
        synchronized (statusMonitor) {
            if (oldStatus != status) {
                return false;
            }
            status = newStatus;
            return true;
        }
    }

	protected void writeToStream(final InputStream in, final OutputStream out)
			throws XMPPException
    {
		final byte[] b = new byte[BUFFER_SIZE];
		int count = 0;
		amountWritten = 0;

        do {
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
		} while (count != -1 && !getStatus().equals(Status.cancelled));

		// the connection was likely terminated abrubtly if these are not equal
		if (!getStatus().equals(Status.cancelled) && getError() == Error.none
				&& amountWritten != fileSize) {
            setStatus(Status.error);
			this.error = Error.connection;
		}
	}

	/**
	 * A class to represent the current status of the file transfer.
	 *
	 * @author Alexander Wenckus
	 *
	 */
	public enum Status {

		/**
		 * An error occurred during the transfer.
		 *
		 * @see FileTransfer#getError()
		 */
		error("Error"),

		/**
         * The initial status of the file transfer.
         */
        initial("Initial"),

        /**
		 * The file transfer is being negotiated with the peer. The party
		 * Receiving the file has the option to accept or refuse a file transfer
		 * request. If they accept, then the process of stream negotiation will
		 * begin. If they refuse the file will not be transfered.
		 *
		 * @see #negotiating_stream
		 */
		negotiating_transfer("Negotiating Transfer"),

		/**
		 * The peer has refused the file transfer request halting the file
		 * transfer negotiation process.
		 */
		refused("Refused"),

		/**
		 * The stream to transfer the file is being negotiated over the chosen
		 * stream type. After the stream negotiating process is complete the
		 * status becomes negotiated.
		 *
		 * @see #negotiated
		 */
		negotiating_stream("Negotiating Stream"),

		/**
		 * After the stream negotiation has completed the intermediate state
		 * between the time when the negotiation is finished and the actual
		 * transfer begins.
		 */
		negotiated("Negotiated"),

		/**
		 * The transfer is in progress.
		 *
		 * @see FileTransfer#getProgress()
		 */
		in_progress("In Progress"),

		/**
		 * The transfer has completed successfully.
		 */
		complete("Complete"),

		/**
		 * The file transfer was cancelled
		 */
		cancelled("Cancelled");

        private String status;

        private Status(String status) {
            this.status = status;
        }

        public String toString() {
            return status;
        }
    }

    /**
     * Return the length of bytes written out to the stream.
     * @return the amount in bytes written out.
     */
    public long getAmountWritten(){
        return amountWritten;
    }

    public enum Error {
		/**
		 * No error
		 */
		none("No error"),

		/**
		 * The peer did not find any of the provided stream mechanisms
		 * acceptable.
		 */
		not_acceptable("The peer did not find any of the provided stream mechanisms acceptable."),

		/**
		 * The provided file to transfer does not exist or could not be read.
		 */
		bad_file("The provided file to transfer does not exist or could not be read."),

		/**
		 * The remote user did not respond or the connection timed out.
		 */
		no_response("The remote user did not respond or the connection timed out."),

		/**
		 * An error occurred over the socket connected to send the file.
		 */
		connection("An error occured over the socket connected to send the file."),

		/**
		 * An error occurred while sending or receiving the file
		 */
		stream("An error occured while sending or recieving the file.");

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
