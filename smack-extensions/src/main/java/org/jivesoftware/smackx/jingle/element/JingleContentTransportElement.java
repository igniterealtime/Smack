/**
 *
 * Copyright 2017 Florian Schmaus
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

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * A jingle transport extension.
 * <jingle>
 *     <content>
 *         <description/>
 *         <transport/> <- Voila.
 *         <security/>
 *     </content>
 * </jingle>
 *
 */
public abstract class JingleContentTransportElement implements ExtensionElement {

    public static final String ELEMENT = "transport";

    protected final List<JingleContentTransportCandidateElement> candidates;
    protected final JingleContentTransportInfoElement info;

    protected JingleContentTransportElement(List<JingleContentTransportCandidateElement> candidates) {
        this(candidates, null);
    }

    protected JingleContentTransportElement(List<JingleContentTransportCandidateElement> candidates, JingleContentTransportInfoElement info) {
        if (candidates != null) {
            this.candidates = Collections.unmodifiableList(candidates);
        }
        else {
            this.candidates = Collections.emptyList();
        }
        this.info = info;
    }

    public List<JingleContentTransportCandidateElement> getCandidates() {
        return candidates;
    }

    public JingleContentTransportInfoElement getInfo() {
        return info;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    protected void addExtraAttributes(XmlStringBuilder xml) {

    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        addExtraAttributes(xml);

        if (candidates.isEmpty() && info == null) {
            xml.closeEmptyElement();

        } else {

            xml.rightAngleBracket();
            xml.append(candidates);
            xml.optElement(info);
            xml.closeElement(this);
        }

        return xml;
    }
}
