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
package org.jivesoftware.smackx.jingle_filetransfer.controller;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smackx.jingle.JingleDescriptionController;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFile;
import org.jivesoftware.smackx.jingle_filetransfer.listener.ProgressListener;

/**
 * Interface with methods of a jingle file transfer, that are exposed to the user.
 */
public interface JingleFileTransferController extends JingleDescriptionController {

    /**
     * Add a ProgressListener.
     * @param listener listener
     */
    void addProgressListener(ProgressListener listener);

    /**
     * Remove a ProgressListener.
     * @param listener listener
     */
    void removeProgressListener(ProgressListener listener);

    /**
     * Get the JingleFile object containing metadata about the transferred file.
     * @return metadata
     */
    JingleFile getMetadata();

    /**
     * Actively cancel the file transfer.
     * @param connection connection
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     */
    void cancel(XMPPConnection connection) throws SmackException.NotConnectedException, InterruptedException;
}
