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
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

public class IoTRemove extends IQ {

    public static final String ELEMENT = "remove";
    public static final String NAMESPACE = Constants.IOT_DISCOVERY_NAMESPACE;

    /**
     * The XMPP address of the Thing to be removed from the registry. According to XEP-0347 § 3.10 the
     * "resource-less JID of the Thing" has to be used, therefore we use {@link BareJid} here.
     */
    private final BareJid jid;

    private final NodeInfo nodeInfo;

    public IoTRemove(BareJid jid) {
        this(jid, NodeInfo.EMPTY);
    }

    public IoTRemove(BareJid jid, NodeInfo nodeInfo) {
        super(ELEMENT, NAMESPACE);
        this.jid = jid;
        this.nodeInfo = nodeInfo;
    }

    public Jid getJid() {
        return jid;
    }

    public String getNodeId() {
        return nodeInfo != null ? nodeInfo.getNodeId() : null;
    }

    public String getSourceId() {
        return nodeInfo != null ? nodeInfo.getSourceId() : null;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("jid", jid);
        nodeInfo.appendTo(xml);
        xml.setEmptyElement();
        return xml;
    }

}
