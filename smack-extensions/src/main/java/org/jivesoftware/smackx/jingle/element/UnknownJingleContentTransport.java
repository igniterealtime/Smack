/**
 *
 * Copyright 2017-2019 Florian Schmaus.
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
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

public final class UnknownJingleContentTransport extends JingleContentTransport {

    private final StandardExtensionElement standardExtensionElement;

    public UnknownJingleContentTransport(StandardExtensionElement standardExtensionElement) {
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
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        return standardExtensionElement.toXML(enclosingXmlEnvironment);
    }

    @Override
    public List<JingleContentTransportCandidate> getCandidates() {
        throw new UnsupportedOperationException();
    }

    @Override
    public JingleContentTransportInfo getInfo() {
        throw new UnsupportedOperationException();
    }

    public StandardExtensionElement getStandardExtensionElement() {
        return standardExtensionElement;
    }
}
