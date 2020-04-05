/**
 *
 * Copyright © 2016-2020 Florian Schmaus
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
import java.util.Date;
import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.iot.element.NodeInfo;

public class IoTFieldsExtension implements ExtensionElement {

    public static final String ELEMENT = "fields";
    public static final String NAMESPACE = Constants.IOT_SENSORDATA_NAMESPACE;
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    private final int seqNr;
    private final boolean done;
    private final List<NodeElement> nodes;

    public IoTFieldsExtension(int seqNr, boolean done, NodeElement node) {
        this(seqNr, done, Collections.singletonList(node));
    }

    public IoTFieldsExtension(int seqNr, boolean done, List<NodeElement> nodes) {
        this.seqNr = seqNr;
        this.done = done;
        this.nodes = Collections.unmodifiableList(nodes);
    }

    public int getSequenceNr() {
        return seqNr;
    }

    public boolean isDone() {
        return done;
    }

    public List<NodeElement> getNodes() {
        return nodes;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("seqnr", Integer.toString(seqNr));
        xml.attribute("done", done);
        xml.rightAngleBracket();

        xml.append(nodes);

        xml.closeElement(this);
        return xml;
    }

    public static IoTFieldsExtension buildFor(int seqNr, boolean done, NodeInfo nodeInfo,
                    List<? extends IoTDataField> data) {
        TimestampElement timestampElement = new TimestampElement(new Date(), data);
        NodeElement nodeElement = new NodeElement(nodeInfo, timestampElement);
        return new IoTFieldsExtension(seqNr, done, nodeElement);
    }

    public static IoTFieldsExtension from(Message message) {
        return message.getExtension(IoTFieldsExtension.class);
    }
}
