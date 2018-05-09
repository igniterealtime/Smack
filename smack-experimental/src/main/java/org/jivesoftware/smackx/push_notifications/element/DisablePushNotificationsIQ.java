/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.push_notifications.element;

import org.jivesoftware.smack.packet.IQ;

import org.jxmpp.jid.Jid;

/**
 * Disable Push Notifications IQ.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0357.html">XEP-0357: Push
 *      Notifications</a>
 * @author Fernando Ramirez
 *
 */
public class DisablePushNotificationsIQ extends IQ {

    /**
     * disable element.
     */
    public static final String ELEMENT = "disable";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = PushNotificationsElements.NAMESPACE;

    private final Jid jid;
    private final String node;

    public DisablePushNotificationsIQ(Jid jid, String node) {
        super(ELEMENT, NAMESPACE);
        this.jid = jid;
        this.node = node;
        this.setType(Type.set);
    }

    public DisablePushNotificationsIQ(Jid jid) {
        this(jid, null);
    }

    /**
     * Get the JID.
     *
     * @return the JID
     */
    public Jid getJid() {
        return jid;
    }

    /**
     * Get the node.
     *
     * @return the node
     */
    public String getNode() {
        return node;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("jid", jid);
        xml.optAttribute("node", node);
        xml.rightAngleBracket();
        return xml;
    }

}
