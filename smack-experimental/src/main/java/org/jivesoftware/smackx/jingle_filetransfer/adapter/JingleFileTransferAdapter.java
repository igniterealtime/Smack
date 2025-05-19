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
package org.jivesoftware.smackx.jingle_filetransfer.adapter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFileTransferImpl;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleIncomingFileRequest;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChild;

/**
 * Adapter that extends the JingleDescriptionAdapter.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleFileTransferAdapter implements JingleDescriptionAdapter<JingleFileTransferImpl> {
    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferAdapter.class.getName());

    @Override
    public JingleFileTransferImpl descriptionFromElement(JingleSession jingleSession, JingleContent jingleContent) {
        JingleFileTransfer description = (JingleFileTransfer) jingleContent.getDescription();
        JingleContent.Senders senders = jingleContent.getSenders();
        List<ExtensionElement> children = description.getJingleContentDescriptionChildren();
        assert children.size() == 1;
        JingleFileTransferChild file = (JingleFileTransferChild) children.get(0);

        if (senders == JingleContent.Senders.initiator) {
            return new JingleIncomingFileOffer(jingleSession, file);
        }
        else if (senders == JingleContent.Senders.responder) {
            return new JingleIncomingFileRequest(jingleSession, file);
        }
        else {
            if (senders == null) {
                LOGGER.log(Level.INFO, "Senders is null. Gajim workaround: assume 'initiator'.");
                return new JingleIncomingFileOffer(jingleSession, file);
            }
            throw new AssertionError("Senders attribute MUST be either initiator or responder. Is: " + senders);
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransferImpl.NAMESPACE;
    }
}
