/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle_filetransfer.component;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.hashes.HashManager;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfo;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle_filetransfer.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;

/**
 * Behind the scenes logic of an incoming Jingle file offer.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleIncomingFileOffer extends AbstractJingleFileOffer implements IncomingFileOfferController {

    private static final Logger LOGGER = Logger.getLogger(JingleIncomingFileOffer.class.getName());
    private OutputStream target;

    public JingleIncomingFileOffer(JingleFileTransferChild offer) {
        super(new JingleFile(offer));
        mState = State.pending;
    }

    @Override
    public Jingle handleDescriptionInfo(JingleContentDescriptionInfo info) {
        return null;
    }

    @Override
    public void onBytestreamReady(BytestreamSession bytestreamSession) {
        if (target == null) {
            throw new IllegalStateException("Target OutputStream is null");
        }

        mState = State.active;
        notifyProgressListenersStarted();

        HashElement hashElement = metadata.getHash();
        MessageDigest digest = null;
        if (hashElement != null) {
            digest = HashManager.getMessageDigest(hashElement.getAlgorithm());
            LOGGER.log(Level.INFO, "File offer had checksum: " + digest.toString());
        }

        LOGGER.log(Level.INFO, "Receiving file");
        InputStream inputStream = null;
        try {
            inputStream = bytestreamSession.getInputStream();

            if (digest != null) {
                inputStream = new DigestInputStream(inputStream, digest);
            }

            int length;
            int readByte = 0;
            long fileSize = metadata.getSize();
            byte[] bufbuf = new byte[4096];
            while ((length = inputStream.read(bufbuf)) >= 0) {
                // User cancels incoming file transfer in active progress.
                if (mState == State.cancelled) {
                    LOGGER.log(Level.INFO, "User canceled file offer in active transfer.");
                    break;
                }

                target.write(bufbuf, 0, length);
                readByte += length;
                // LOGGER.log(Level.INFO, "Read " + readByte + " (" + length + ") of " + fileSize + " bytes.");
                notifyProgressListeners(readByte);
                if (readByte == fileSize) {
                    break;
                }
            }
            LOGGER.log(Level.INFO, "Reading/Writing finished.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot get InputStream from BytestreamSession: " + e, e);
            notifyProgressListenersOnError(JingleReason.Reason.connectivity_error, e.getMessage());
        } finally {
            mState = State.ended;
            if (inputStream != null) {
                try {
                    inputStream.close();
                    LOGGER.log(Level.INFO, "CipherInputStream closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not close InputStream: " + e, e);
                }
            }

            if (target != null) {
                try {
                    target.close();
                    LOGGER.log(Level.INFO, "FileOutputStream closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close OutputStream: " + e, e);
                }
            }
        }

        if (digest != null) {
            byte[] mDigest = ((DigestInputStream) inputStream).getMessageDigest().digest();
            if (!Arrays.equals(hashElement.getHash(), mDigest)) {
                LOGGER.log(Level.WARNING, "CHECKSUM MISMATCH!");
            } else {
                LOGGER.log(Level.INFO, "CHECKSUM MATCHED :)");
            }
        }

        notifyProgressListenersFinished();
        getParent().onContentFinished();
    }

    @Override
    public boolean isOffer() {
        return true;
    }

    @Override
    public boolean isRequest() {
        return false;
    }

    @Override
    public void accept(XMPPConnection connection, File target)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException, IOException {
        mState = State.negotiating;

        if (!target.exists()) {
            target.createNewFile();
        }

        this.target = new FileOutputStream(target);

        JingleSessionImpl session = getParent().getParent();
        if (session.getSessionState() == JingleSessionImpl.SessionState.pending) {
            session.sendAccept(connection);
        }
    }

    @Override
    public void accept(XMPPConnection connection, OutputStream stream)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException {
        mState = State.negotiating;
        target = stream;

        JingleSessionImpl session = getParent().getParent();
        if (session.getSessionState() == JingleSessionImpl.SessionState.pending) {
            session.sendAccept(connection);
        }
    }
}
