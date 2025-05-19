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

import org.jivesoftware.smack.SmackException.NotConnectedException;

import org.jivesoftware.smackx.si.packet.StreamInitiation;
import org.jivesoftware.smackx.thumbnail.element.Thumbnail;

import org.jxmpp.jid.Jid;

/**
 * A request to send a file received from another user.
 *
 * @author Alexander Wenckus
 *
 */
public class FileTransferRequest {
    private final StreamInitiation streamInitiation;

    private final FileTransferManager manager;

    /**
     * A receive request is constructed from the Stream Initiation request
     * received from the initiator.
     *
     * @param manager TODO javadoc me please
     *            The manager handling this file transfer
     *
     * @param si TODO javadoc me please
     *            The Stream initiation received from the initiator.
     */
    public FileTransferRequest(FileTransferManager manager, StreamInitiation si) {
        this.streamInitiation = si;
        this.manager = manager;
    }

    public Thumbnail getThumbnail() {
        return streamInitiation.getFile().getThumbnail();
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
     * Returns the description of the file provided by the requester.
     *
     * @return Returns the description of the file provided by the requester.
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
    public Jid getRequestor() {
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
     * Returns the stream initiation stanza that was sent by the requester which
     * contains the parameters of the file transfer being transfer and also the
     * methods available to transfer the file.
     *
     * @return Returns the stream initiation stanza that was sent by the
     *         requester which contains the parameters of the file transfer
     *         being transfer and also the methods available to transfer the
     *         file.
     */
    protected StreamInitiation getStreamInitiation() {
        return streamInitiation;
    }

    /**
     * Accepts this file transfer and creates the incoming file transfer.
     *
     * @return Returns the IncomingFileTransfer on which the
     *         file transfer can be carried out.
     */
    public IncomingFileTransfer accept() {
        return manager.createIncomingFileTransfer(this);
    }

    /**
     * Rejects the file transfer request.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void reject() throws NotConnectedException, InterruptedException {
        manager.rejectIncomingFileTransfer(this);
    }
}
