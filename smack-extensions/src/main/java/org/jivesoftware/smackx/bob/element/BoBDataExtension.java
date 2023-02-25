/**
 *
 * Copyright 2020-2021 Florian Schmaus
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
package org.jivesoftware.smackx.bob.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.StanzaView;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.bob.BoBData;
import org.jivesoftware.smackx.bob.BoBManager;
import org.jivesoftware.smackx.bob.ContentId;

/**
 * Bits of Binary data extension element.
 *
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBDataExtension implements ExtensionElement {

    public static final String ELEMENT = "data";
    public static final String NAMESPACE = BoBManager.NAMESPACE;

    private final ContentId cid;
    private final BoBData bobData;

    /**
     * Bits of Binary data extension constructor.
     *
     * @param cid TODO javadoc me please
     * @param bobData TODO javadoc me please
     */
    public BoBDataExtension(ContentId cid, BoBData bobData) {
        this.cid = Objects.requireNonNull(cid);
        this.bobData = Objects.requireNonNull(bobData);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    /**
     * Get the content ID.
     *
     * @return the content ID.
     * @since 4.4.1
     */
    public final ContentId getContentId() {
        return cid;
    }

    /**
     * Get the Bits of Binary (BOB) data.
     *
     * @return the BoB data.
     * @since 4.4.1
     */
    public final BoBData getBobData() {
        return bobData;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("cid", cid.getCid());
        xml.attribute("type", bobData.getType());
        xml.optAttribute("max-age", bobData.getMaxAge());
        xml.rightAngleBracket();

        xml.append(bobData.getContentBase64Encoded());

        xml.closeElement(this);
        return xml;
    }

    public static BoBDataExtension from(StanzaView stanza) {
        return stanza.getExtension(BoBDataExtension.class);
    }

}
