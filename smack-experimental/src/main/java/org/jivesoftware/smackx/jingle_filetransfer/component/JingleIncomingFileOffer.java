/**
 *
 * Copyright 2017 Paul Schaub
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
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle_filetransfer.controller.IncomingFileOfferController;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;

/**
 * Behind the scenes logic of an incoming Jingle file offer (They offer a file).
 */
public class JingleIncomingFileOffer extends AbstractJingleFileOffer implements IncomingFileOfferController {

    private static final Logger LOGGER = Logger.getLogger(JingleIncomingFileOffer.class.getName());
    private OutputStream target;

    public JingleIncomingFileOffer(JingleFileTransferChildElement offer) {
        super(new JingleFile(offer));
        this.state = State.pending;
    }

    @Override
    public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
        return null;
    }

    @Override
    public void onBytestreamReady(BytestreamSession bytestreamSession) {
        if (target == null) {
            throw new IllegalStateException("Target OutputStream is null");
        }

        if (state == State.negotiating) {
            state = State.active;
            notifyProgressListenersStarted();
        } else {
            return;
        }

        HashElement hashElement = metadata.getHashElement();
        MessageDigest digest = null;
        if (hashElement != null) {
            digest = HashManager.getMessageDigest(hashElement.getAlgorithm());
            LOGGER.log(Level.FINE, "File offer had checksum: " + digest.toString() + ": " + hashElement.getHashB64());
        }

        LOGGER.log(Level.FINE, "Receive file");

        InputStream inputStream = null;
        try {
            inputStream = bytestreamSession.getInputStream();

            if (digest != null) {
                inputStream = new DigestInputStream(inputStream, digest);
            }

            int length = 0;
            int read = 0;
            byte[] bufbuf = new byte[4096];
            while ((length = inputStream.read(bufbuf)) >= 0) {
                if (getState() == State.cancelled) {
                    break;
                }
                target.write(bufbuf, 0, length);
                read += length;
                LOGGER.log(Level.FINER, "Read " + read + " (" + length + ") of " + metadata.getSize() + " bytes.");

                percentage = ((float) read) / ((float) metadata.getSize());

                if (read == (int) metadata.getSize()) {
                    break;
                }
            }
            LOGGER.log(Level.FINE, "Reading/Writing finished.");
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Cannot get InputStream from BytestreamSession.", e);
        } finally {
            state = State.ended;
            if (inputStream != null) {
                try {
                    inputStream.close();
                    LOGGER.log(Level.FINER, "CipherInputStream closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close InputStream.", e);
                }
            }

            if (target != null) {
                try {
                    target.close();
                    LOGGER.log(Level.FINER, "FileOutputStream closed.");
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Could not close OutputStream.", e);
                }
            }
        }

        if (digest != null) {
            byte[] mDigest = ((DigestInputStream) inputStream).getMessageDigest().digest();
            if (!Arrays.equals(hashElement.getHash(), mDigest)) {
                LOGGER.log(Level.WARNING, "CHECKSUM MISMATCH!");
            } else {
                LOGGER.log(Level.FINE, "CHECKSUM MATCHED :)");
            }
        }
        notifyProgressListenersTerminated(JingleReasonElement.Reason.success);
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
        state = State.negotiating;

        if (!target.exists()) {
            target.createNewFile();
        }

        this.target = new FileOutputStream(target);

        JingleSession session = getParent().getParent();
        if (session.getSessionState() == JingleSession.SessionState.pending) {
            session.sendAccept(connection);
        }
    }

    @Override
    public void accept(XMPPConnection connection, OutputStream stream)
            throws InterruptedException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            SmackException.NoResponseException {
        state  = State.negotiating;

        target = stream;

        JingleSession session = getParent().getParent();
        if (session.getSessionState() == JingleSession.SessionState.pending) {
            session.sendAccept(connection);
        }
    }
}
