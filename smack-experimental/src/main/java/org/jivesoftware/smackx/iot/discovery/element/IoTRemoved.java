/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.discovery.element;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.iot.element.NodeInfo;

public class IoTRemoved extends IQ {

    public static final String ELEMENT = "removed";
    public static final String NAMESPACE = Constants.IOT_DISCOVERY_NAMESPACE;

    private final NodeInfo nodeInfo;

    public IoTRemoved() {
        this(NodeInfo.EMPTY);
    }

    public IoTRemoved(NodeInfo nodeInfo) {
        super(ELEMENT, NAMESPACE);
        this.nodeInfo = nodeInfo;
    }

    public String getNodeId() {
        return nodeInfo.getNodeId();
    }

    public String getSourceId() {
        return nodeInfo.getSourceId();
    }

    public NodeInfo getNodeInfo() {
        return nodeInfo;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        nodeInfo.appendTo(xml);
        xml.setEmptyElement();
        return xml;
    }

}
