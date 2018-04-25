/**
 *
 * Copyright 2016 Fernando Ramirez
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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.xhtmlim.XHTMLText;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;

/**
 * Bits of Binary extension element.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBExtension extends XHTMLExtension {

    private final BoBHash bobHash;
    private final String alt;
    private final String paragraph;

    /**
     * Bits of Binary extension constructor.
     * 
     * @param bobHash
     * @param alt
     * @param paragraph
     */
    public BoBExtension(BoBHash bobHash, String alt, String paragraph) {
        this.bobHash = bobHash;
        this.alt = alt;
        this.paragraph = paragraph;
    }

    /**
     * Get the BoB hash.
     * 
     * @return the BoB hash
     */
    public BoBHash getBoBHash() {
        return bobHash;
    }

    /**
     * Get the alt field.
     * 
     * @return the alt field
     */
    public String getAlt() {
        return alt;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();

        xml.halfOpenElement(Message.BODY);
        xml.xmlnsAttribute(XHTMLText.NAMESPACE);
        xml.rightAngleBracket();

        xml.openElement(XHTMLText.P);
        xml.optEscape(paragraph);

        xml.halfOpenElement(XHTMLText.IMG);
        xml.optAttribute("alt", alt);
        xml.attribute("src", bobHash.toSrc());
        xml.closeEmptyElement();

        xml.closeElement(XHTMLText.P);
        xml.closeElement(Message.BODY);
        xml.closeElement(this);
        return xml;
    }

    public static BoBExtension from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }

}
