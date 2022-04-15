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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfo;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;

/**
 * Behind the scenes logic of an outgoing Jingle file offer.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleOutgoingFileOffer extends AbstractJingleFileOffer implements OutgoingFileOfferController {
    private static final Logger LOGGER = Logger.getLogger(JingleOutgoingFileOffer.class.getName());

    private final InputStream source;

    public JingleOutgoingFileOffer(File file, JingleFile metadata) throws FileNotFoundException {
        super(metadata);
        this.source = new FileInputStream(file);
    }

    public JingleOutgoingFileOffer(InputStream inputStream, JingleFile metadata) {
        super(metadata);
        this.source = inputStream;
    }

    @Override
    public Jingle handleDescriptionInfo(JingleContentDescriptionInfo info) {
        return null;
    }

    @Override
    public void onBytestreamReady(BytestreamSession bytestreamSession) {
        if (source == null) {
            throw new IllegalStateException("Source InputStream is null!");
        }

        notifyProgressListenersStarted();
        OutputStream outputStream;
        try {
            outputStream = bytestreamSession.getOutputStream();

            int writeByte = 0;
            byte[] buf = new byte[8192];
            while (true) {
                int length = source.read(buf);
                if (length < 0) {
                    break;
                }
                outputStream.write(buf, 0, length);
                writeByte += length;
                notifyProgressListeners(writeByte);
            }

            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception while sending file: " + e, e);
            notifyProgressListenersOnError(JingleReason.Reason.connectivity_error, e.getMessage());
        } finally {
            try {
                source.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close FileInputStream: " + e, e);
            }
        }
        notifyProgressListenersFinished();
    }

    @Override
    public boolean isOffer() {
        return true;
    }

    @Override
    public boolean isRequest() {
        return false;
    }
}
