/**
 *
 * Copyright 2016 Fernando Ramirez, 2020 Florian Schmaus
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

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.bob.BoBData;
import org.jivesoftware.smackx.bob.BoBManager;
import org.jivesoftware.smackx.bob.ContentId;

/**
 * Bits of Binary IQ class.
 *
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBIQ extends IQ {

    /**
     * data element.
     */
    public static final String ELEMENT = "data";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = BoBManager.NAMESPACE;

    private final ContentId cid;
    private final BoBData bobData;

    /**
     * Bits of Binary IQ constructor.
     *
     * @param cid TODO javadoc me please
     * @param bobData TODO javadoc me please
     */
    public BoBIQ(ContentId cid, BoBData bobData) {
        super(ELEMENT, NAMESPACE);
        this.cid = cid;
        this.bobData = bobData;
    }

    /**
     * Bits of Binary IQ constructor.
     *
     * @param cid TODO javadoc me please
     */
    public BoBIQ(ContentId cid) {
        this(cid, null);
    }

    /**
     * Get the BoB hash.
     *
     * @return the BoB hash
     * @deprecated use {@link #getContentId()} instead.
     */
    // TODO: Remove in Smack 4.5.
    @Deprecated
    public ContentId getBoBHash() {
        return cid;
    }

    /**
     * Get the BoB hash.
     *
     * @return the BoB hash
     */
    public ContentId getContentId() {
        return cid;
    }

    /**
     * Get the BoB data.
     *
     * @return the BoB data
     */
    public BoBData getBoBData() {
        return bobData;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("cid", cid.getCid());

        if (bobData != null) {
            xml.optIntAttribute("max_age", bobData.getMaxAge());
            xml.attribute("type", bobData.getType());
            xml.rightAngleBracket();
            xml.escape(bobData.getContentBase64Encoded());
        } else {
            xml.setEmptyElement();
        }

        return xml;
    }

}
