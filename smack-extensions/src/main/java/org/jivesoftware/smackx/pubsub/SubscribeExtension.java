/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.Jid;

/**
 * Represents a request to subscribe to a node.
 *
 * @author Robin Collier
 */
public class SubscribeExtension extends NodeExtension {
    protected final Jid jid;

    public SubscribeExtension(Jid subscribeJid) {
        super(PubSubElementType.SUBSCRIBE);
        jid = subscribeJid;
    }

    public SubscribeExtension(Jid subscribeJid, String nodeId) {
        super(PubSubElementType.SUBSCRIBE, nodeId);
        jid = subscribeJid;
    }

    public Jid getJid() {
        return jid;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        xml.optAttribute("node", getNode());
        xml.attribute("jid", getJid());
        xml.closeEmptyElement();
        return xml;
    }
}
