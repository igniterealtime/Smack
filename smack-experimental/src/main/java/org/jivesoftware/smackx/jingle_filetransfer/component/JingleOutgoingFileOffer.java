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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle_filetransfer.controller.OutgoingFileOfferController;

/**
 * Backend logic of an outgoing file offer (We offer a file).
 */
public class JingleOutgoingFileOffer extends AbstractJingleFileOffer implements OutgoingFileOfferController {
    private static final Logger LOGGER = Logger.getLogger(JingleOutgoingFileOffer.class.getName());

    private final InputStream source;

    public JingleOutgoingFileOffer(File file, JingleFile metadata) throws FileNotFoundException {
        super(metadata);
        this.source = new FileInputStream(file);
        this.state = State.pending;
    }

    public JingleOutgoingFileOffer(InputStream inputStream, JingleFile metadata) {
        super(metadata);
        this.source = inputStream;
        this.state = State.pending;
    }

    @Override
    public JingleElement handleDescriptionInfo(JingleContentDescriptionInfoElement info) {
        return null;
    }

    @Override
    public void onBytestreamReady(BytestreamSession bytestreamSession) {
        if (source == null) {
            throw new IllegalStateException("Source InputStream is null!");
        }

        state = State.active;

        notifyProgressListenersStarted();

        OutputStream outputStream = null;

        try {
            outputStream = bytestreamSession.getOutputStream();

            byte[] buf = new byte[8192];

            int written = 0;

            while (true) {
                if (getState() == State.cancelled) {
                    break;
                }
                int r = source.read(buf);
                if (r < 0) {
                    break;
                }
                outputStream.write(buf, 0, r);
                written += r;
                percentage = ((float) getMetadata().getSize()) / ((float) written);
            }

            outputStream.flush();
            outputStream.close();

        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Exception while sending file.", e);
        } finally {
            state = State.ended;
            try {
                source.close();
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Could not close FileInputStream.", e);
            }
        }

        notifyProgressListenersTerminated(JingleReasonElement.Reason.success);
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
