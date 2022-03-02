/**
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smackx.AbstractXmlElement;

import javax.xml.namespace.QName;

/**
 * Implements <code>ExtensionElement</code> for the "transferred" element defined below.
 * XEP-0251: Jingle Session Transfer 0.2 (2009-10-05)
 * @see <a href="https://xmpp.org/extensions/xep-0251.html#unattended">XEP-0251 § 2. Unattended Transfer</a>
 * @see <a href="https://xmpp.org/extensions/xep-0251.html#attended ">XEP-0251 § 3. Attended Transfer</a>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5359#page-50">RFC5359 § 2.4. Transfer - Unattended /Attended</a>
 *
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public class SdpTransferred extends AbstractXmlElement {
    /**
     * The name of the "transfer" element.
     */
    public static final String ELEMENT = "transferred";

    /**
     * The namespace of the "transfer" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:transfer:0";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     /**
     * The name of the "to" attribute of the "transfer" element.
     */
    public static final String ATTR_TO = "to";

    public SdpTransferred() {
        super(builder());
    }

    /**
     * Initializes a new <code>SdpTransferred</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param build the builder for this extension element.
     */
    public SdpTransferred(Builder build) {
        super(build);
    }

    public static Builder builder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SdpTransferred. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SdpTransferred.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, SdpTransferred> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        @Override
        public SdpTransferred build() {
            return new SdpTransferred(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
