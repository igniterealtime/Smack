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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.CloseableUtil;


/**
 * An incoming file transfer is created when the
 * {@link FileTransferManager#createIncomingFileTransfer(FileTransferRequest)}
 * method is invoked. It is a file being sent to the local user from another
 * user on the jabber network. There are two stages of the file transfer to be
 * concerned with and they can be handled in different ways depending upon the
 * method that is invoked on this class.
 *
 * The first way that a file is received is by calling the
 * {@link #receiveFile()} method. This method, negotiates the appropriate stream
 * method and then returns the InputStream to read the file
 * data from.
 *
 * The second way that a file can be received through this class is by invoking
 * the {@link #receiveFile(File)} method. This method returns immediately and
 * takes as its parameter a file on the local file system where the file
 * recieved from the transfer will be put.
 *
 * @author Alexander Wenckus
 */
public class IncomingFileTransfer extends FileTransfer {

    private static final Logger LOGGER = Logger.getLogger(IncomingFileTransfer.class.getName());

    private FileTransferRequest receiveRequest;

    private InputStream inputStream;

    protected IncomingFileTransfer(FileTransferRequest request,
            FileTransferNegotiator transferNegotiator) {
        super(request.getRequestor(), request.getStreamID(), transferNegotiator);
        this.receiveRequest = request;
    }

    /**
     * Negotiates the stream method to transfer the file over and then returns
     * the negotiated stream.
     *
     * @return The negotiated InputStream from which to read the data.
     * @throws SmackException
     * @throws XMPPErrorException If there is an error in the negotiation process an exception
     *                       is thrown.
     * @throws InterruptedException
     */
    public InputStream receiveFile() throws SmackException, XMPPErrorException, InterruptedException {
        if (inputStream != null) {
            throw new IllegalStateException("Transfer already negotiated!");
        }

        try {
            inputStream = negotiateStream();
        }
        catch (XMPPErrorException e) {
            setException(e);
            throw e;
        }

        return inputStream;
    }

    /**
     * This method negotiates the stream and then transfer's the file over the negotiated stream.
     * The transferred file will be saved at the provided location.
     *
     * This method will return immediately, file transfer progress can be monitored through several
     * methods:
     *
     * <UL>
     * <LI>{@link FileTransfer#getStatus()}</LI>
     * <LI>{@link FileTransfer#getProgress()}</LI>
     * <LI>{@link FileTransfer#isDone()}</LI>
     * </UL>
     *
     * @param file The location to save the file.
     * @throws SmackException when the file transfer fails
     * @throws IOException
     * @throws IllegalArgumentException This exception is thrown when the the provided file is
     *         either null, or cannot be written to.
     */
    public void receiveFile(final File file) throws SmackException, IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.exists()) {
                 file.createNewFile();
            }
        if (!file.canWrite()) {
                throw new IllegalArgumentException("Cannot write to provided file");
        }

        Thread transferThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    inputStream = negotiateStream();
                }
                catch (Exception e) {
                    setStatus(FileTransfer.Status.error);
                    setException(e);
                    return;
                }

                OutputStream outputStream = null;
                try {
                    outputStream = new FileOutputStream(file);
                    setStatus(Status.in_progress);
                    writeToStream(inputStream, outputStream);
                }
                catch (FileNotFoundException e) {
                    setStatus(Status.error);
                    setError(Error.bad_file);
                    setException(e);
                }
                catch (IOException e) {
                    setStatus(Status.error);
                    setError(Error.stream);
                    setException(e);
                }


                if (getStatus().equals(Status.in_progress)) {
                    setStatus(Status.complete);
                }
                CloseableUtil.maybeClose(inputStream, LOGGER);
                CloseableUtil.maybeClose(outputStream, LOGGER);
            }
        }, "File Transfer " + streamID);
        transferThread.start();
    }

    private InputStream negotiateStream() throws SmackException, XMPPErrorException, InterruptedException {
        setStatus(Status.negotiating_transfer);
        final StreamNegotiator streamNegotiator = negotiator
                .selectStreamNegotiator(receiveRequest);
        setStatus(Status.negotiating_stream);
        FutureTask<InputStream> streamNegotiatorTask = new FutureTask<>(
                new Callable<InputStream>() {

                    @Override
                    public InputStream call() throws Exception {
                        return streamNegotiator
                                .createIncomingStream(receiveRequest.getStreamInitiation());
                    }
                });
        streamNegotiatorTask.run();
        InputStream inputStream;
        try {
            inputStream = streamNegotiatorTask.get(15, TimeUnit.SECONDS);
        }
        catch (ExecutionException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof XMPPErrorException) {
                throw (XMPPErrorException) cause;
            }
            if (cause instanceof InterruptedException) {
                throw (InterruptedException) cause;
            }
            if (cause instanceof NoResponseException) {
                throw (NoResponseException) cause;
            }
            if (cause instanceof SmackException) {
                throw (SmackException) cause;
            }
            throw new SmackException("Error in execution", e);
        }
        catch (TimeoutException e) {
            throw new SmackException("Request timed out", e);
        }
        finally {
            streamNegotiatorTask.cancel(true);
        }
        setStatus(Status.negotiated);
        return inputStream;
    }

    @Override
    public void cancel() {
        setStatus(Status.cancelled);
    }

}
