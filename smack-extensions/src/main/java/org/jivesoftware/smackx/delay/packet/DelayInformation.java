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
package org.jivesoftware.smackx.delay.packet;

import java.util.Date;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jxmpp.util.XmppDateTime;

/**
 * Represents timestamp information about data stored for later delivery. A DelayInformation will 
 * always includes the timestamp when the stanza(/packet) was originally sent and may include more 
 * information such as the JID of the entity that originally sent the stanza(/packet) as well as the reason
 * for the delay.<p>
 * 
 * For more information see <a href="http://xmpp.org/extensions/xep-0091.html">XEP-0091</a>
 * and <a href="http://xmpp.org/extensions/xep-0203.html">XEP-0203</a>.
 * 
 * @author Gaston Dombiak
 * @author Florian Schmaus
 */
public class DelayInformation implements ExtensionElement {
    public static final String ELEMENT = "delay";
    public static final String NAMESPACE = "urn:xmpp:delay";

    private final Date stamp;
    private final String from;
    private final String reason;

    /**
     * Creates a new instance with the specified timestamp. 
     * @param stamp the timestamp
     */
    public DelayInformation(Date stamp, String from, String reason) {
        this.stamp = stamp;
        this.from = from;
        this.reason = reason;
    }

    public DelayInformation(Date stamp) {
        this(stamp, null, null);
    }

    /**
     * Returns the JID of the entity that originally sent the stanza(/packet) or that delayed the 
     * delivery of the stanza(/packet) or <tt>null</tt> if this information is not available.
     * 
     * @return the JID of the entity that originally sent the stanza(/packet) or that delayed the 
     *         delivery of the packet.
     */
    public String getFrom() {
        return from;
    }

    /**
     * Returns the timestamp when the stanza(/packet) was originally sent. The returned Date is 
     * be understood as UTC.
     * 
     * @return the timestamp when the stanza(/packet) was originally sent.
     */
    public Date getStamp() {
        return stamp;
    }

    /**
     * Returns a natural-language description of the reason for the delay or <tt>null</tt> if 
     * this information is not available.
     * 
     * @return a natural-language description of the reason for the delay or <tt>null</tt>.
     */
    public String getReason() {
        return reason;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("stamp", XmppDateTime.formatXEP0082Date(stamp));
        xml.optAttribute("from", from);
        xml.rightAngleBracket();
        xml.optAppend(reason);
        xml.closeElement(this);
        return xml;
    }

    /**
     * Return delay information from the given stanza.
     *
     * @param packet
     * @return the DelayInformation or null
     * @deprecated use {@link #from(Stanza)} instead
     */
    @Deprecated
    public static DelayInformation getFrom(Stanza packet) {
        return from(packet);
    }

    /**
     * Return delay information from the given stanza.
     *
     * @param packet
     * @return the DelayInformation or null
     */
    public static DelayInformation from(Stanza packet) {
        return packet.getExtension(ELEMENT, NAMESPACE);
    }
}
