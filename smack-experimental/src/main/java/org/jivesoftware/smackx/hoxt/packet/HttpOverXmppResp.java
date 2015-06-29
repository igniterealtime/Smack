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
 * Represents Resp IQ packet.
 *
 * @author Andriy Tsykholyas
 * @see <a href="http://xmpp.org/extensions/xep-0332.html">XEP-0332: HTTP over XMPP transport</a>
 */
public final class HttpOverXmppResp extends AbstractHttpOverXmpp {

    public static final String ELEMENT = "resp";

    private HttpOverXmppResp(Builder builder) {
        super(ELEMENT, builder);
        this.statusCode = builder.statusCode;
        this.statusMessage = builder.statusMessage;
    }

    private int statusCode;
    private String statusMessage;

    @Override
    protected IQChildElementXmlStringBuilder getIQHoxtChildElementBuilder(IQChildElementXmlStringBuilder builder) {
        builder.append(" ");
        builder.append("version='").append(StringUtils.escapeForXML(version)).append("'");
        builder.append(" ");
        builder.append("statusCode='").append(Integer.toString(statusCode)).append("'");
        if (statusMessage != null) {
            builder.append(" ");
            builder.append("statusMessage='").append(StringUtils.escapeForXML(statusMessage)).append("'");
        }
        builder.append(">");
        return builder;
    }

    /**
     * Returns statusCode attribute.
     *
     * @return statusCode attribute
     */
    public int getStatusCode() {
        return statusCode;
    }

    /**
     * Returns statusMessage attribute.
     *
     * @return statusMessage attribute
     */
    public String getStatusMessage() {
        return statusMessage;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * A configuration builder for HttpOverXmppReq. Use {@link HttpOverXmppResp#builder()} to obtain a new instance and
     * {@link #build} to build the configuration.
     */
    public static final class Builder extends AbstractHttpOverXmpp.Builder<Builder, HttpOverXmppResp> {

        private int statusCode = 200;
        private String statusMessage = null;

        /**
         * Sets statusCode attribute.
         *
         * @param statusCode statusCode attribute
         * 
         * @return the builder
         */
        public Builder setStatusCode(int statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        /**
         * Sets statusMessage attribute.
         *
         * @param statusMessage statusMessage attribute
         * 
         * @return the builder
         */
        public Builder setStatusMessage(String statusMessage) {
            this.statusMessage = statusMessage;
            return this;

        }

        @Override
        public HttpOverXmppResp build() {
            return new HttpOverXmppResp(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }

}
