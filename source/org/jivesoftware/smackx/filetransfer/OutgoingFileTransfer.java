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
import org.jivesoftware.smack.packet.XMPPError;

import java.io.*;

/**
 * Handles the sending of a file to another user. File transfer's in jabber have
 * several steps and there are several methods in this class that handle these
 * steps differently.
 *
 * @author Alexander Wenckus
 *
 */
public class OutgoingFileTransfer extends FileTransfer {

	private static int RESPONSE_TIMEOUT = 60 * 1000;

	/**
	 * Returns the time in milliseconds after which the file transfer
	 * negotiation process will timeout if the other user has not responded.
	 *
	 * @return Returns the time in milliseconds after which the file transfer
	 *         negotiation process will timeout if the remote user has not
	 *         responded.
	 */
	public static int getResponseTimeout() {
		return RESPONSE_TIMEOUT;
	}

	/**
	 * Sets the time in milliseconds after which the file transfer negotiation
	 * process will timeout if the other user has not responded.
	 *
	 * @param responseTimeout
	 *            The timeout time in milliseconds.
	 */
	public static void setResponseTimeout(int responseTimeout) {
		RESPONSE_TIMEOUT = responseTimeout;
	}

	private OutputStream outputStream;

	private String initiator;

	private Thread transferThread;

	protected OutgoingFileTransfer(String initiator, String target,
			String streamID, FileTransferNegotiator transferNegotiator) {
		super(target, streamID, transferNegotiator);
		this.initiator = initiator;
	}

	protected void setOutputStream(OutputStream stream) {
		if (outputStream == null) {
			this.outputStream = stream;
		}
	}

	/**
	 * Returns the output stream connected to the peer to transfer the file. It
	 * is only available after it has been succesfully negotiated by the
	 * {@link StreamNegotiator}.
	 *
	 * @return Returns the output stream connected to the peer to transfer the
	 *         file.
	 */
	protected OutputStream getOutputStream() {
		if (getStatus().equals(FileTransfer.Status.negotiated)) {
			return outputStream;
		} else {
			return null;
		}
	}

	/**
	 * This method handles the negotiation of the file transfer and the stream,
	 * it only returns the created stream after the negotiation has been completed.
	 *
	 * @param fileName
	 *            The name of the file that will be transmitted. It is
	 *            preferable for this name to have an extension as it will be
	 *            used to determine the type of file it is.
	 * @param fileSize
	 *            The size in bytes of the file that will be transmitted.
	 * @param description
	 *            A description of the file that will be transmitted.
	 * @return The OutputStream that is connected to the peer to transmit the
	 *         file.
	 * @throws XMPPException
	 *             Thrown if an error occurs during the file transfer
	 *             negotiation process.
	 */
	public synchronized OutputStream sendFile(String fileName, long fileSize,
			String description) throws XMPPException {
		if (isDone() || outputStream != null) {
			throw new IllegalStateException(
					"The negotation process has already"
							+ " been attempted on this file transfer");
		}
		try {
			this.outputStream = negotiateStream(fileName, fileSize, description);
		} catch (XMPPException e) {
			handleXMPPException(e);
			throw e;
		}
		return outputStream;
	}

	/**
	 * This methods handles the transfer and stream negotiation process. It
	 * returns immediately and its progress can be monitored through the
	 * {@link NegotiationProgress} callback. When the negotiation process is
	 * complete the OutputStream can be retrieved from the callback via the
	 * {@link NegotiationProgress#getOutputStream()} method.
	 *
	 * @param fileName
	 *            The name of the file that will be transmitted. It is
	 *            preferable for this name to have an extension as it will be
	 *            used to determine the type of file it is.
	 * @param fileSize
	 *            The size in bytes of the file that will be transmitted.
	 * @param description
	 *            A description of the file that will be transmitted.
	 * @param progress
	 *            A callback to monitor the progress of the file transfer
	 *            negotiation process and to retrieve the OutputStream when it
	 *            is complete.
	 */
	public synchronized void sendFile(final String fileName,
			final long fileSize, final String description,
			NegotiationProgress progress) {
		checkTransferThread();
		if (isDone() || outputStream != null) {
			throw new IllegalStateException(
					"The negotation process has already"
							+ " been attempted for this file transfer");
		}
		progress.delegate = this;
		transferThread = new Thread(new Runnable() {
			public void run() {
				try {
					OutgoingFileTransfer.this.outputStream = negotiateStream(
							fileName, fileSize, description);
				} catch (XMPPException e) {
					handleXMPPException(e);
				}
			}
		}, "File Transfer Negotiation " + streamID);
		transferThread.start();
	}

	private void checkTransferThread() {
		if (transferThread != null && transferThread.isAlive() || isDone()) {
			throw new IllegalStateException(
					"File transfer in progress or has already completed.");
		}
	}

	/**
	 * This method handles the stream negotiation process and transmits the file
	 * to the remote user. It returns immediatly and the progress of the file
	 * transfer can be monitored through several methods:
	 *
	 * <UL>
	 * <LI>{@link FileTransfer#getStatus()}
	 * <LI>{@link FileTransfer#getProgress()}
	 * <LI>{@link FileTransfer#isDone()}
	 * </UL>
	 *
	 * @throws XMPPException
	 *             If there is an error during the negotiation process or the
	 *             sending of the file.
	 */
	public synchronized void sendFile(final File file, final String description)
			throws XMPPException {
		checkTransferThread();
		if (file == null || !file.exists() || !file.canRead()) {
			throw new IllegalArgumentException("Could not read file");
		} else {
			setFileInfo(file.getAbsolutePath(), file.getName(), file.length());
		}

		transferThread = new Thread(new Runnable() {
			public void run() {
				try {
					outputStream = negotiateStream(file.getName(), file
							.length(), description);
				} catch (XMPPException e) {
					handleXMPPException(e);
					return;
				}
				if (outputStream == null) {
					return;
				}

                if (!updateStatus(Status.negotiated, Status.in_progress)) {
					return;
				}

				InputStream inputStream = null;
				try {
					inputStream = new FileInputStream(file);
					writeToStream(inputStream, outputStream);
				} catch (FileNotFoundException e) {
					setStatus(FileTransfer.Status.error);
					setError(Error.bad_file);
					setException(e);
				} catch (XMPPException e) {
					setStatus(FileTransfer.Status.error);
					setException(e);
				} finally {
					try {
						if (inputStream != null) {
							inputStream.close();
						}

						outputStream.flush();
						outputStream.close();
					} catch (IOException e) {
                        /* Do Nothing */
					}
				}
                updateStatus(Status.in_progress, FileTransfer.Status.complete);
				}

		}, "File Transfer " + streamID);
		transferThread.start();
	}

	private void handleXMPPException(XMPPException e) {
		setStatus(FileTransfer.Status.error);
		XMPPError error = e.getXMPPError();
		if (error != null) {
			int code = error.getCode();
			if (code == 403) {
				setStatus(Status.refused);
				return;
			} else if (code == 400) {
				setStatus(Status.error);
				setError(Error.not_acceptable);
			}
		}
		setException(e);
	}

	/**
	 * Returns the amount of bytes that have been sent for the file transfer. Or
	 * -1 if the file transfer has not started.
	 * <p>
	 * Note: This method is only useful when the {@link #sendFile(File, String)}
	 * method is called, as it is the only method that actualy transmits the
	 * file.
	 *
	 * @return Returns the amount of bytes that have been sent for the file
	 *         transfer. Or -1 if the file transfer has not started.
	 */
	public long getBytesSent() {
		return amountWritten;
	}

	private OutputStream negotiateStream(String fileName, long fileSize,
			String description) throws XMPPException {
		// Negotiate the file transfer profile

        if (!updateStatus(Status.initial, Status.negotiating_transfer)) {
            throw new XMPPException("Illegal state change");
        }
		StreamNegotiator streamNegotiator = negotiator.negotiateOutgoingTransfer(
				getPeer(), streamID, fileName, fileSize, description,
				RESPONSE_TIMEOUT);

		if (streamNegotiator == null) {
			setStatus(Status.error);
			setError(Error.no_response);
			return null;
		}

        // Negotiate the stream
        if (!updateStatus(Status.negotiating_transfer, Status.negotiating_stream)) {
            throw new XMPPException("Illegal state change");
        }
		outputStream = streamNegotiator.createOutgoingStream(streamID,
                initiator, getPeer());

        if (!updateStatus(Status.negotiating_stream, Status.negotiated)) {
            throw new XMPPException("Illegal state change");
		}
		return outputStream;
	}

	public void cancel() {
		setStatus(Status.cancelled);
	}

	/**
	 * A callback class to retrive the status of an outgoing transfer
	 * negotiation process.
	 *
	 * @author Alexander Wenckus
	 *
	 */
	public static class NegotiationProgress {

		private OutgoingFileTransfer delegate;

		/**
		 * Returns the current status of the negotiation process.
		 *
		 * @return Returns the current status of the negotiation process.
		 */
		public Status getStatus() {
			if (delegate == null) {
				throw new IllegalStateException("delegate not yet set");
			}
			return delegate.getStatus();
		}

		/**
		 * Once the negotiation process is completed the output stream can be
		 * retrieved.
		 *
		 * @return Once the negotiation process is completed the output stream
		 *         can be retrieved.
		 *
		 */
		public OutputStream getOutputStream() {
			if (delegate == null) {
				throw new IllegalStateException("delegate not yet set");
			}
			return delegate.getOutputStream();
		}
	}

}
