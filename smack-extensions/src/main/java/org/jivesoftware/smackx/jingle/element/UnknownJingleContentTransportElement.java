/**
 *
 * Copyright 2017 Florian Schmaus, Paul Schaub.
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
package org.jivesoftware.smackx.jingle.element;

import java.util.List;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public final class UnknownJingleContentTransportElement extends JingleContentTransportElement {

    private final StandardExtensionElement standardExtensionElement;

    public UnknownJingleContentTransportElement(StandardExtensionElement standardExtensionElement) {
        super(null, null);
        this.standardExtensionElement = standardExtensionElement;
    }

    @Override
    public String getElementName() {
        return standardExtensionElement.getElementName();
    }

    @Override
    public String getNamespace() {
        return standardExtensionElement.getNamespace();
    }

    @Override
    public XmlStringBuilder toXML() {
        return standardExtensionElement.toXML();
    }

    @Override
    public List<JingleContentTransportCandidateElement> getCandidates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JingleContentTransportInfoElement getInfo() {
        throw new UnsupportedOperationException();
    }

    public StandardExtensionElement getStandardExtensionElement() {
        return standardExtensionElement;
    }
}