/*
 *
 * Copyright © 2014-2026 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Limits feature.
 * For any XMPP stream, there is an "initiating entity" (a client or server) and a "responding entity" that they are connecting to.
 * The responding entity advertises its limits in the <code>&lt;stream:features/&gt;</code> element that it sends at the start of the stream.
 * <a href="https://xmpp.org/extensions/xep-0478.html">XEP-0478: Stream Limits Advertisement</a>
 */
public class Limits implements ExtensionElement {
    public static final String ELEMENT = "limits";
    public static final String NAMESPACE = "urn:xmpp:stream-limits:0";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The maximum size of any first-level stream elements (including stanzas),
     * in bytes the announcing entity is willing to accept.
     * Guidance on acceptable limits is provided in RFC 6120 section 13.12.
     * If the responding entity is unable to determine its limits, this child can be absent.
     * Element: <code>&lt;max-bytes/&gt;</code>. Type UnsignedInt.
     */
    public final int maxBytes;

    /**
     * The number of seconds without any traffic from the initiating entity after which
     * the server may consider the stream idle, and either perform liveness checks
     * (using e.g. Stream Management (XEP-0198) or XMPP Ping (XEP-0199)) or terminate the stream.
     * Guidance on handling idle connections is provided in RFC 6120 section 4.6.
     * If the responding entity is unable to determine its limits, this child can be absent.
     * Element: <code>&lt;idle-seconds/&gt;</code>. Type UnsignedInt.
     */
    public final int idleSeconds;

    public Limits(int maxBytes, int idleSeconds) {
        this.maxBytes = maxBytes;
        this.idleSeconds = idleSeconds;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public int getMaxBytes() {
        return maxBytes;
    }

    public int getIdleSeconds() {
        return idleSeconds;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();
        if (maxBytes != 0) {
            xml.element("max-bytes", String.valueOf(maxBytes));
        }
        if (idleSeconds != 0) {
            xml.element("idle-seconds", String.valueOf(idleSeconds));
        }
        xml.closeElement(this);
        return xml;
    }

}
