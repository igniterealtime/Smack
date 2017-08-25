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
package org.jivesoftware.smackx.jingle_filetransfer.element;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionChildElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle_filetransfer.component.JingleFileTransfer;

/**
 * Jingle File Transfer Element.
 */
public class JingleFileTransferElement extends JingleContentDescriptionElement {

    public JingleFileTransferElement(JingleContentDescriptionChildElement payload) {
        this(Collections.singletonList(payload));
    }

    public JingleFileTransferElement(List<JingleContentDescriptionChildElement> payloads) {
        super(payloads);
        if (payloads.size() != 1) {
            throw new IllegalArgumentException("Jingle File Transfers only support one payload element.");
        }
    }

    @Override
    public String getNamespace() {
        return JingleFileTransfer.NAMESPACE;
    }
}
