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
package org.jivesoftware.smackx.externalservicediscovery;

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

/**
 * Implements the <code>services</code> element described in the following XEP.
 * XEP-0215: External Service Discovery 1.0.0 (2022-08-23)
 *
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0215.html">XEP-0215: External Service Discovery</a>
 */
public class ExternalServices extends AbstractXmlElement {
    /**
     * The XML name of the <code>services</code> element.
     */
    public static final String ELEMENT = "services";

    public static final String NAMESPACE = "urn:xmpp:extdisco:2";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * <code>ExternalServices</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public ExternalServices() {
        super(getBuilder());
    }

    /**
     * Initializes a new <code>ExternalServices</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public ExternalServices(Builder builder) {
        super(builder);
    }

    public String getType() {
        return getAttributeValue(ServiceElement.ATTR_TYPE);
    }

    /**
     * Get all the services of this Services.
     *
     * @return the <code>ExtensionElement</code>s of this source
     */
    public List<ServiceElement> getServices() {
        return getChildElements(ServiceElement.class);
    }

    /**
     * Gets the services that have the given transport type.
     *
     * @param transport the transport type to find.
     * @return list of ServiceElement that support the given transport
     */
    public List<ServiceElement> getServiceType(String transport) {
        List<ServiceElement> services = new ArrayList<>();
        for (ServiceElement service : getServices()) {
            if (transport.equals(service.getTransport()))
                services.add(service);
        }
        return services;
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for ExternalServices. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the ExternalServices.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, ExternalServices> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder setType(String type) {
            addAttribute(ServiceElement.ATTR_TYPE, type);
            return this;
        }

        /**
         * Adds a specific serviceElement to this element.
         *
         * @param serviceElement the <code>ServiceElement</code> to add to this element
         * @return builder instance
         */
        public Builder addService(ServiceElement serviceElement) {
            addChildElement(serviceElement);
            return this;
        }

        @Override
        public ExternalServices build() {
            return new ExternalServices(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
