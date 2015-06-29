/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt.packet;

import org.jivesoftware.smack.util.StringUtils;

/**
 * Represents Req IQ packet.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public final class HttpOverXmppReq extends AbstractHttpOverXmpp {

    public static final String ELEMENT = "req";

    private HttpOverXmppReq(Builder builder) {
        super(ELEMENT, builder);
        this.method = builder.method;
        this.resource = builder.resource;
        this.maxChunkSize = builder.maxChunkSize;
        this.ibb = builder.ibb;
        this.jingle = builder.jingle;
        this.sipub = builder.sipub;
        setType(Type.set);
    }

    private HttpMethod method;
    private String resource;

    // TODO: validate: xs:minInclusive value='256' xs:maxInclusive value='65536'
    private int maxChunkSize; // 0 means not set

    private boolean sipub;

    private boolean ibb;
    private boolean jingle;

    @Override
    protected IQChildElementXmlStringBuilder getIQHoxtChildElementBuilder(IQChildElementXmlStringBuilder builder) {
        builder.append(" ");
        builder.append("method='").append(method.toString()).append("'");
        builder.append(" ");
        builder.append("resource='").append(StringUtils.escapeForXML(resource)).append("'");
        builder.append(" ");
        builder.append("version='").append(StringUtils.escapeForXML(version)).append("'");
        if (maxChunkSize != 0) {
            builder.append(" ");
            builder.append("maxChunkSize='").append(Integer.toString(maxChunkSize)).append("'");
        }
        builder.append(" ");
        builder.append("sipub='").append(Boolean.toString(sipub)).append("'");
        builder.append(" ");
        builder.append("ibb='").append(Boolean.toString(ibb)).append("'");
        builder.append(" ");
        builder.append("jingle='").append(Boolean.toString(jingle)).append("'");
        builder.append(">");
        return builder;
    }

    /**
     * Returns method attribute.
     *
     * @return method attribute
     */
    public HttpMethod getMethod() {
        return method;
    }

    /**
     * Returns resource attribute.
     *
     * @return resource attribute
     */
    public String getResource() {
        return resource;
    }

    /**
     * Returns maxChunkSize attribute.
     *
     * @return maxChunkSize attribute
     */
    public int getMaxChunkSize() {
        return maxChunkSize;
    }

    /**
     * Returns sipub attribute.
     *
     * @return sipub attribute
     */
    public boolean isSipub() {
        return sipub;
    }

    /**
     * Returns ibb attribute.
     *
     * @return ibb attribute
     */
    public boolean isIbb() {
        return ibb;
    }

    /**
     * Returns jingle attribute.
     *
     * @return jingle attribute
     */
    public boolean isJingle() {
        return jingle;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A configuration builder for HttpOverXmppReq. Use {@link HttpOverXmppReq#builder()} to obtain a new instance and
     * {@link #build} to build the configuration.
     */
    public static final class Builder extends AbstractHttpOverXmpp.Builder<Builder, HttpOverXmppReq> {

        private HttpMethod method;
        private String resource;

        // TODO: validate: xs:minInclusive value='256' xs:maxInclusive
        // value='65536'
        private int maxChunkSize = 0; // 0 means not set

        private boolean sipub = true;

        private boolean ibb = true;
        private boolean jingle = true;

        private Builder() {

        }

        /**
         * Sets method attribute.
         *
         * @param method attribute
         *
         * @return the builder
         */
        public Builder setMethod(HttpMethod method) {
            this.method = method;
            return this;
        }

        /**
         * Sets resource attribute.
         *
         * @param resource attribute
         * 
         * @return the builder
         */
        public Builder setResource(String resource) {
            this.resource = resource;
            return this;
        }

        /**
         * Sets jingle attribute.
         *
         * @param jingle jingle attribute
         * 
         * @return the builder
         */
        public Builder setJingle(boolean jingle) {
            this.jingle = jingle;
            return this;
        }

        /**
         * Sets ibb attribute.
         *
         * @param ibb ibb attribute
         * 
         * @return the builder
         */
        public Builder setIbb(boolean ibb) {
            this.ibb = ibb;
            return this;
        }

        /**
         * Sets sipub attribute.
         *
         * @param sipub sipub attribute
         * 
         * @return the builder
         */
        public Builder setSipub(boolean sipub) {
            this.sipub = sipub;
            return this;
        }

        /**
         * Sets maxChunkSize attribute.
         *
         * @param maxChunkSize maxChunkSize attribute
         * 
         * @return the builder
         */
        public Builder setMaxChunkSize(int maxChunkSize) {
            this.maxChunkSize = maxChunkSize;
            return this;
        }

        @Override
        public HttpOverXmppReq build() {
            if (method == null) {
                throw new IllegalArgumentException("Method cannot be null");
            }
            if (resource == null) {
                throw new IllegalArgumentException("Resource cannot be null");
            }
            return new HttpOverXmppReq(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }

    }
}
