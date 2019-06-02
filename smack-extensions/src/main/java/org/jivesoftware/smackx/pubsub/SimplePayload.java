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

import java.io.IOException;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * The default payload representation for {@link PayloadItem#getPayload()}.  It simply
 * stores the XML payload as a string.
 *
 * @author Robin Collier
 */
public class SimplePayload implements ExtensionElement {
    private final String elemName;
    private final String ns;
    private final String payload;

    /**
     * Construct a <tt>SimplePayload</tt> object with the specified element name,
     * namespace and content.  The content must be well formed XML.
     *
     * @param xmlPayload The payload data
     */
    public SimplePayload(String xmlPayload) {
        XmlPullParser parser;
        try {
            parser = PacketParserUtils.getParserFor(xmlPayload);
        }
        catch (XmlPullParserException | IOException e) {
            throw new AssertionError(e);
        }
        QName qname = parser.getQName();

        payload = xmlPayload;

        elemName = StringUtils.requireNotNullNorEmpty(qname.getLocalPart(), "Could not determine element name from XML payload");
        ns = StringUtils.requireNotNullNorEmpty(qname.getNamespaceURI(), "Could not determine namespace from XML payload");
    }

    /**
     * Construct a <tt>SimplePayload</tt> object with the specified element name,
     * namespace and content.  The content must be well formed XML.
     *
     * @param elementName The root element name (of the payload)
     * @param namespace The namespace of the payload, null if there is none
     * @param xmlPayload The payload data
     * @deprecated use {@link #SimplePayload(String)} insteas.
     */
    // TODO: Remove in Smack 4.5
    @Deprecated
    public SimplePayload(String elementName, String namespace, CharSequence xmlPayload) {
        this(xmlPayload.toString());
        if (!elementName.equals(this.elemName)) {
            throw new IllegalArgumentException();
        }
        if (!namespace.equals(this.ns)) {
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String getElementName() {
        return elemName;
    }

    @Override
    public String getNamespace() {
        return ns;
    }

    @Override
    public String toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return payload;
    }

    @Override
    public String toString() {
        return getClass().getName() + "payload [" + toXML() + "]";
    }
}
