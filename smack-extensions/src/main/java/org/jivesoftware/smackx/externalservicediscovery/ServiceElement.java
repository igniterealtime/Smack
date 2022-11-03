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

import javax.xml.namespace.QName;

/**
 * Represents the <code>service</code> elements described in the following XEPs.
 * XEP-0215: External Service Discovery 1.0.0 (2022-08-23)
 *
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0215.html">XEP-0215: External Service Discovery</a>
 */
public class ServiceElement extends AbstractXmlElement {
    /**
     * The name of the ServiceElement.
     */
    public static final String ELEMENT = "service";

    public static final QName QNAME = new QName(ExternalServices.NAMESPACE, ELEMENT);

    /**
     * The name of the attribute parameters used in the <code>ServiceElement</code>.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0215.html#table-1">2. Protocol#Table 1: Attributes</a>
     */
    public static final String ATTR_ACTION = "action";
    public static final String ATTR_EXPIRES = "expires";
    public static final String ATTR_HOST = "host";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_PASSWORD = "password";
    public static final String ATTR_PORT = "port";
    public static final String ATTR_RESTRICTED = "restricted";
    public static final String ATTR_TRANSPORT = "transport";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_USERNAME = "username";

    /**
     * <code>ServiceElement</code> default constructor use by DefaultXmlElementProvider newInstance() etc.
     * Default to use ExternalServices.NAMESPACE, to be modified/
     *
     * @see #getBuilder(String)
     */
    public ServiceElement() {
        super(builder(ExternalServices.NAMESPACE));
    }

    /**
     * Creates a new <code>ServiceElement</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public ServiceElement(Builder builder) {
        super(builder);
    }

    public String getAction() {
        return getAttributeValue(ATTR_ACTION);
    }

    public String getExpires() {
        return getAttributeValue(ATTR_EXPIRES);
    }

    public String getHost() {
        return getAttributeValue(ATTR_HOST);
    }

    public String getName() {
        return getAttributeValue(ATTR_NAME);
    }

    public String getPassword() {
        return getAttributeValue(ATTR_PASSWORD);
    }

    public int getPort() {
        return getAttributeAsInt(ATTR_PORT);
    }

    public String getRestricted() {
        return getAttributeValue(ATTR_RESTRICTED);
    }

    public String getTransport() {
        return getAttributeValue(ATTR_TRANSPORT);
    }

    public String getType() {
        return getAttributeValue(ATTR_TYPE);
    }

    public String getUserName() {
        return getAttributeValue(ATTR_USERNAME);
    }

    public static Builder builder(String nameSpace) {
        return new Builder(ELEMENT, nameSpace);
    }

    /**
     * Check if this service matches the given one with regards to matching service.
     *
     * @param oElement the other ServerElement to compare to
     * @return true if this ServerElement and the one given have relevant matching attributes.
     */
    public boolean serviceEquals(ServiceElement oElement) {
        if (oElement == null) {
            return false;
        } else if (oElement == this) {
            return true;
        } else {
            return this.getHost().equals(oElement.getHost())
                    && this.getPort() == oElement.getPort()
                    && this.getTransport().equals(oElement.getTransport())
                    && this.getType().equals(oElement.getType());
        }
    }

    /**
     * Builder for ServiceElement. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the ServiceElement.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, ServiceElement> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder setAction(String action) {
            addAttribute(ATTR_HOST, action);
            return this;
        }

        public Builder setExpires(String expires) {
            addAttribute(ATTR_HOST, expires);
            return this;
        }

        public Builder setHost(String host) {
            addAttribute(ATTR_HOST, host);
            return this;
        }

        public Builder setName(String name) {
            addAttribute(ATTR_NAME, name);
            return this;
        }

        public Builder setPassword(String password) {
            addAttribute(ATTR_PASSWORD, password);
            return this;
        }

        public Builder setPort(String port) {
            addAttribute(ATTR_NAME, port);
            return this;
        }

        public Builder setRestricted(String restricted) {
            addAttribute(ATTR_TRANSPORT, restricted);
            return this;
        }

        public Builder setTransport(String transport) {
            addAttribute(ATTR_TRANSPORT, transport);
            return this;
        }

        public Builder setType(String type) {
            addAttribute(ATTR_TYPE, type);
            return this;
        }

        public Builder setUserName(String username) {
            addAttribute(ATTR_USERNAME, username);
            return this;
        }

        @Override
        public ServiceElement build() {
            return new ServiceElement(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
