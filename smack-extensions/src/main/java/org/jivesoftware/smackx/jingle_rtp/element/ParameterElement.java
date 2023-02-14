/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp.element;

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

/**
 * Represents the <code>parameter</code> elements described in the following XEPs:
 * 1. XEP-0167: Jingle RTP Sessions 1.2.1 (2020-09-29)
 * @see <a href="https://xmpp.org/extensions/xep-0167.html#sdp">XEP-0167 § 6. Mapping to Session Description Protoco</a>
 *
 * 2. XEP-0294: Jingle RTP Header Extensions Negotiation 1.1.1 (2021-10-23)
 * @see <a href="https://xmpp.org/extensions/xep-0294.html#element">XEP-0294 § 3. New elements</a>
 *
 * 3. XEP-0339: Source-Specific Media Attributes in Jingle 1.0.1 (2021-10-23)
 * @see <a href="https://xmpp.org/extensions/xep-0339.html#sdp">XEP-0339 § 2. Mapping to Session Description Protocol</a>
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public class ParameterElement extends AbstractXmlElement {
    /**
     * The name of the "parameter" element.
     */
    public static final String ELEMENT = "parameter";

    /**
     * The name of the <code>name</code> parameter in the <code>parameter</code> element.
     */
    public static final String ATTR_NAME = "name";

    /**
     * The name of the <code>value</code> parameter in the <code>parameter</code> element.
     */
    public static final String ATTR_VALUE = "value";

    /**
     * <code>ParameterElement</code> default constructor use by DefaultXmlElementProvider newInstance() etc.
     * Default to use RtpDescription.NAMESPACE, to be modified/
     *
     * @see #getBuilder(String)
     */
    public ParameterElement() {
        super(builder(RtpDescription.NAMESPACE));
    }

    /**
     * Creates a new <code>ParameterElement</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public ParameterElement(Builder builder) {
        super(builder);
    }

    /**
     * Returns the name of the format parameter we are representing here.
     *
     * @return the name of the format parameter we are representing here.
     */
    public String getName() {
        return getAttributeValue(ATTR_NAME);
    }

    /**
     * Returns the value of the format parameter we are representing here.
     *
     * @return the value of the format parameter we are representing here.
     */
    public String getValue() {
        return getAttributeValue(ATTR_VALUE);
    }

    public static Builder builder(String nameSpace) {
        return new Builder(ELEMENT, nameSpace);
    }

    /**
     * Builder for ParameterElement. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the ParameterElement.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, ParameterElement> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the name of the format parameter we are representing here.
         *
         * @param name the name of the format parameter we are representing here.
         * @return builder instance
         */
        public Builder setName(String name) {
            addAttribute(ATTR_NAME, name);
            return this;
        }

        /**
         * Sets that value of the format parameter we are representing here.
         *
         * Note: A RTP Header Extension that requires extra parameters in the a=b form can embed <code>parameter</code> elements
         * to describe it. Any other form of parameter can be stored as the 'key' attribute in a parameter element
         * with an empty value. https://xmpp.org/extensions/xep-0294.html#element
         *
         * @param value the value of the format parameter we are representing here.
         * @return builder instance
         */
        public Builder setValue(String value) {
            addAttribute(ATTR_VALUE, value);
            return this;
        }

        /**
         * Sets the name/value pair of the parameter we are representing here.
         *
         * @param name the name of the format parameter we are representing here.
         * @param value the value of the format parameter we are representing here.
         * @return builder instance
         */
        public Builder setNameValue(String name, String value) {
            addAttribute(ATTR_NAME, name);
            addAttribute(ATTR_VALUE, value);
            return this;
        }

        @Override
        public ParameterElement build() {
            return new ParameterElement(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
