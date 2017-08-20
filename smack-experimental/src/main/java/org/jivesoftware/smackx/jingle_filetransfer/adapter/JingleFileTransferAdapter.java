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
package org.jivesoftware.smackx.jingle_filetransfer.adapter;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smackx.jingle.adapter.JingleDescriptionAdapter;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFileTransfer;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleIncomingFileOffer;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleIncomingFileRequest;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferChildElement;
import org.jivesoftware.smackx.jingle_filetransfer.element.JingleFileTransferElement;

/**
 * Created by vanitas on 28.07.17.
 */
public class JingleFileTransferAdapter implements JingleDescriptionAdapter<JingleFileTransfer> {
    private static final Logger LOGGER = Logger.getLogger(JingleFileTransferAdapter.class.getName());

    @Override
    public JingleFileTransfer descriptionFromElement(JingleContentElement.Creator creator, JingleContentElement.Senders senders,
                                                     String contentName, String contentDisposition, JingleContentDescriptionElement element) {
        JingleFileTransferElement description = (JingleFileTransferElement) element;
        List<NamedElement> children = description.getJingleContentDescriptionChildren();
        assert children.size() == 1;
        JingleFileTransferChildElement file = (JingleFileTransferChildElement) children.get(0);

        if (senders == JingleContentElement.Senders.initiator) {
            return new JingleIncomingFileOffer(file);
        } else if (senders == JingleContentElement.Senders.responder) {
            return new JingleIncomingFileRequest(file);
        } else {
            if (senders == null) {
                LOGGER.log(Level.INFO, "Senders is null. Gajim workaround: assume 'initiator'.");
                return new JingleIncomingFileOffer(file);
            }
            throw new AssertionError("Senders attribute MUST be either initiator or responder. Is: " + senders);
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }
}
