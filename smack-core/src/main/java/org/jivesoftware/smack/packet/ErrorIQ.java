/**
 *
 * Copyright © 2014-2023 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import java.util.Objects;

import javax.xml.namespace.QName;

/**
 * An XMPP error IQ.
 * <p>
 * According to RFC 6120 § 8.3.1 "4. An error stanza MUST contain an &lt;error/&gt; child element.", so this class can
 * only be constructed if a stanza error is provided.
 */
public final class ErrorIQ extends IQ {

    public static final String ELEMENT = StanzaError.ERROR;

    private final IQ request;

    private ErrorIQ(Builder builder, QName childElementQName) {
        super(builder, childElementQName);
        Objects.requireNonNull(builder.getError(), "Must provide an stanza error when building error IQs");
        this.request = builder.request;
    }

    public static ErrorIQ createErrorResponse(final IQ request, final StanzaError error) {
        Builder builder = new Builder(error, request);
        builder.setError(error);
        return builder.build();
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (request == null) {
            return null;
        }

        return request.getIQChildElementBuilder(xml);
    }

    public static Builder builder(StanzaError error) {
        return new Builder(error, IqData.EMPTY.ofType(IQ.Type.error));
    }

    public static Builder builder(StanzaError error, IqData iqData) {
        return new Builder(error, iqData);
    }

    public static final class Builder extends IqBuilder<Builder, ErrorIQ> {

        private IQ request;

        Builder(StanzaError error, IqData iqData) {
            super(iqData);
            if (iqData.getType() != IQ.Type.error) {
                throw new IllegalArgumentException("Error IQs must be of type 'error'");
            }
            Objects.requireNonNull(error, "Must provide an stanza error when building error IQs");
            setError(error);
        }

        Builder(StanzaError error, IQ request) {
            this(error, AbstractIqBuilder.createErrorResponse(request));
            this.request = request;
        }

        @Override
        public Builder getThis() {
            return this;
        }

        @Override
        public ErrorIQ build() {
            QName childElementQname = null;
            if (request != null) {
                childElementQname = request.getChildElementQName();
            }
            return new ErrorIQ(this, childElementQname);
        }

    }
}
