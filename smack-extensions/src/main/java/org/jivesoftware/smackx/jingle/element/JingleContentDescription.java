/**
 *
 * Copyright 2017 Florian Schmaus.
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
package org.jivesoftware.smackx.jingle.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

import java.util.Collections;
import java.util.List;

/**
 * Jingle content description.
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 */
public class JingleContentDescription extends AbstractXmlElement {
    public static final String ELEMENT = "description";
    private static Builder mBuilder;

    private final List<ExtensionElement> payloads;

    public JingleContentDescription() {
        this(getBuilder());
    }

    /**
     * Creates a new <code>RtpDescription</code>.
     *
     * @param builder Builder instance
     */
    public JingleContentDescription(Builder builder) {
        super(builder);
        this.payloads = Collections.emptyList();
    }

    protected JingleContentDescription(List<? extends ExtensionElement> payloads) {
        super(mBuilder = getBuilder());
        if (payloads != null) {
            this.payloads = Collections.unmodifiableList(payloads);
        } else {
            this.payloads = Collections.emptyList();
        }
        mBuilder.addPayload(payloads)
                .build();
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public List<ExtensionElement> getJingleContentDescriptionChildren() {
        return payloads;
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, null);
    }

    /**
     * Builder for JingleContentDescription. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RtpDescription.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, JingleContentDescription> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder addPayload(List<? extends ExtensionElement> payloads) {
            return addChildElements(payloads);
        }

        @Override
        public JingleContentDescription build() {
            return new JingleContentDescription(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
