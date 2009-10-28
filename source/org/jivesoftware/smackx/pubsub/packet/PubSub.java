/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.PubSubElementType;

/**
 * The standard PubSub extension of an {@link IQ} packet.  This is the topmost
 * element of all pubsub requests and replies as defined in the <a href="http://xmpp.org/extensions/xep-0060">Publish-Subscribe</a> 
 * specification.
 * 
 * @author Robin Collier
 */
public class PubSub extends IQ
{
	private PubSubNamespace ns = PubSubNamespace.BASIC;
	
	/**
    * Returns the XML element name of the extension sub-packet root element.
    *
    * @return the XML element name of the packet extension.
    */
    public String getElementName() {
        return "pubsub";
    }

    /** 
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is 
     * http://jabber.org/protocol/pubsub with a specific fragment depending
     * on the request.  The namespace is defined at <a href="http://xmpp.org/registrar/namespaces.html">XMPP Registrar</a> at
     * 
     * The default value has no fragment.
     * 
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() 
    {
        return ns.getXmlns();
    }

    /**
     * Set the namespace for the packet if it something other than the default
     * case of {@link PubSubNamespace#BASIC}.  The {@link #getNamespace()} method will return 
     * the result of calling {@link PubSubNamespace#getXmlns()} on the specified enum.
     * 
     * @param ns - The new value for the namespace.
     */
	public void setPubSubNamespace(PubSubNamespace ns)
	{
		this.ns = ns;
	}

	public PacketExtension getExtension(PubSubElementType elem)
	{
		return getExtension(elem.getElementName(), elem.getNamespace().getXmlns());
	}

	/**
	 * Returns the current value of the namespace.  The {@link #getNamespace()} method will return 
     * the result of calling {@link PubSubNamespace#getXmlns()} this value.
	 * 
	 * @return The current value of the namespace.
	 */
	public PubSubNamespace getPubSubNamespace()
	{
		return ns;
	}
    /**
     * Returns the XML representation of a pubsub element according the specification.
     * 
     * The XML representation will be inside of an iq packet like
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
    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append("\">");
        buf.append(getExtensionsXML());
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

}
