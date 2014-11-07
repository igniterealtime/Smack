/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.pep.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * Represents XMPP PEP/XEP-163 pubsub packets.<p>
 * 
 * The 'http://jabber.org/protocol/pubsub' namespace  is used to publish personal events items from one client 
 * to subscribed clients (See XEP-163).
 *
 * @author Jeff Williams
 */
public class PEPPubSub extends IQ {
    
    public static final String ELEMENT = "pubsub";
    public static final String NAMESPACE = "http://jabber.org/protocol/pubsub";

    private final PEPItem item;

    /**
    * Creates a new PubSub.
    *
    */
    public PEPPubSub(PEPItem item) {
        super(ELEMENT, NAMESPACE);
        this.item = item;
    }

    /**
     * Returns the XML representation of a Personal Event Publish according the specification.
     * 
     * Usually the XML representation will be inside of a Message XML representation like
     * in the following example:
     * <pre>
     * &lt;message id="MlIpV-4" to="gato1@gato.home" from="gato3@gato.home/Smack"&gt;
     *     &lt;subject&gt;Any subject you want&lt;/subject&gt;
     *     &lt;body&gt;This message contains roster items.&lt;/body&gt;
     *     &lt;x xmlns="jabber:x:roster"&gt;
     *         &lt;item jid="gato1@gato.home"/&gt;
     *         &lt;item jid="gato2@gato.home"/&gt;
     *     &lt;/x&gt;
     * &lt;/message&gt;
     * </pre>
     * 
     */
    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.rightAngleBracket();

        buf.openElement("publish").attribute("node", item.getNode()).rightAngleBracket();
        buf.append(item.toXML());
        buf.closeElement("publish");

        return buf;
    }

}
