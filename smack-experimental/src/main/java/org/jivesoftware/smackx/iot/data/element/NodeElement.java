/**
 *
 * Copyright © 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.data.element;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.iot.element.NodeInfo;

public class NodeElement implements NamedElement {

    public static final String ELEMENT = "node";

    private final NodeInfo nodeInfo;
    private final List<TimestampElement> timestampElements;

    public NodeElement(NodeInfo nodeInfo, TimestampElement timestampElement) {
        this(nodeInfo, Collections.singletonList(timestampElement));
    }

    public NodeElement(NodeInfo nodeInfo, List<TimestampElement> timestampElements) {
        this.nodeInfo = nodeInfo;
        this.timestampElements = Collections.unmodifiableList(timestampElements);
    }

    public List<TimestampElement> getTimestampElements() {
        return timestampElements;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        nodeInfo.appendTo(xml);
        xml.rightAngleBracket();

        xml.append(timestampElements);

        xml.closeElement(this);
        return xml;
    }

}
