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

package org.jivesoftware.smackx.xhtmlim.packet;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * An XHTML sub-packet, which is used by XMPP clients to exchange formatted text. The XHTML 
 * extension is only a subset of XHTML 1.0.
 * <p>
 * The following link summarizes the requirements of XHTML IM:
 * <a href="http://www.xmpp.org/extensions/xep-0071.html">XEP-0071: XHTML-IM</a>.
 * </p>
 *
 * @author Gaston Dombiak
 */
public class XHTMLExtension implements ExtensionElement {

    public static final String ELEMENT = "html";
    public static final String NAMESPACE = "http://jabber.org/protocol/xhtml-im";

    private List<CharSequence> bodies = new ArrayList<CharSequence>();

    /**
    * Returns the XML element name of the extension sub-packet root element.
    * Always returns "html"
    *
    * @return the XML element name of the packet extension.
    */
    public String getElementName() {
        return ELEMENT;
    }

    /** 
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is always "http://jabber.org/protocol/xhtml-im"
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() {
        return NAMESPACE;
    }

    /**
     * Returns the XML representation of a XHTML extension according the specification.
     * 
     * Usually the XML representation will be inside of a Message XML representation like
     * in the following example:
     * <pre>
     * &lt;message id="MlIpV-4" to="gato1@gato.home" from="gato3@gato.home/Smack"&gt;
     *     &lt;subject&gt;Any subject you want&lt;/subject&gt;
     *     &lt;body&gt;This message contains something interesting.&lt;/body&gt;
     *     &lt;html xmlns="http://jabber.org/protocol/xhtml-im"&gt;
     *         &lt;body&gt;&lt;p style='font-size:large'&gt;This message contains something &lt;em&gt;interesting&lt;/em&gt;.&lt;/p&gt;&lt;/body&gt;
     *     &lt;/html&gt;
     * &lt;/message&gt;
     * </pre>
     * 
     */
    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        // Loop through all the bodies and append them to the string buffer
        for (CharSequence body : getBodies()) {
            xml.append(body);
        }
        xml.closeElement(this);
        return xml;
    }

    /**
     * Returns a List of the bodies in the packet.
     *
     * @return a List of the bodies in the packet.
     */
    public List<CharSequence> getBodies() {
        synchronized (bodies) {
            return Collections.unmodifiableList(new ArrayList<CharSequence>(bodies));
        }
    }

    /**
     * Adds a body to the packet.
     *
     * @param body the body to add.
     */
    public void addBody(CharSequence body) {
        synchronized (bodies) {
            bodies.add(body);
        }
    }

    /**
     * Returns a count of the bodies in the XHTML packet.
     *
     * @return the number of bodies in the XHTML packet.
     */
    public int getBodiesCount() {
        synchronized (bodies) {
            return bodies.size();
        }
    }

    public static XHTMLExtension from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }
}
