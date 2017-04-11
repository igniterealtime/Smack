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
package org.jivesoftware.smackx.pubsub.packet;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.pubsub.PubSubElementType;
import org.jxmpp.jid.Jid;

/**
 * The standard PubSub extension of an {@link IQ} packet.  This is the topmost
 * element of all pubsub requests and replies as defined in the <a href="http://xmpp.org/extensions/xep-0060">Publish-Subscribe</a> 
 * specification.
 * 
 * @author Robin Collier
 */
public class PubSub extends IQ
{
    public static final String ELEMENT = "pubsub";
    public static final String NAMESPACE = "http://jabber.org/protocol/pubsub";

    public PubSub() {
        super(ELEMENT, NAMESPACE);
    }

    public PubSub(PubSubNamespace ns) {
        super(ELEMENT, ns.getXmlns());
    }

    public PubSub(Jid to, Type type, PubSubNamespace ns) {
        super(ELEMENT, (ns == null ? PubSubNamespace.BASIC : ns).getXmlns());
        setTo(to);
        setType(type);
    }

    @SuppressWarnings("unchecked")
    public <PE extends ExtensionElement> PE getExtension(PubSubElementType elem)
    {
        return (PE) getExtension(elem.getElementName(), elem.getNamespace().getXmlns());
    }

    /**
     * Returns the XML representation of a pubsub element according the specification.
     * 
     * The XML representation will be inside of an iq stanza(/packet) like
     * in the following example:
     * <pre>
     * &lt;iq type='set' id="MlIpV-4" to="pubsub.gato.home" from="gato3@gato.home/Smack"&gt;
     *     &lt;pubsub xmlns="http://jabber.org/protocol/pubsub"&gt;
     *                      :
     *         Specific request extension
     *                      :
     *     &lt;/pubsub&gt;
     * &lt;/iq&gt;
     * </pre>
     * 
     */
    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        // N.B. We could use SimpleIQ here, but PubSub IQs will nearly *always* have packet extensions, which means that
        // SimpleIQs xml.setEmptyElement() is counter-productive in this case and we use xml.rightAngleBracket()
        // instead, as there are likely sub-elements to follow.
        xml.rightAngleBracket();
        return xml;
    }

    public static PubSub createPubsubPacket(Jid to, Type type, ExtensionElement extension, PubSubNamespace ns) {
        PubSub pubSub = new PubSub(to, type, ns);
        pubSub.addExtension(extension);
        return pubSub;
    }
}
