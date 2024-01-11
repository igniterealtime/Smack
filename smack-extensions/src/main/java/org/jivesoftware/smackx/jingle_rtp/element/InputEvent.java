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
package org.jivesoftware.smackx.jingle_rtp.element;

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

/**
 * Represents the content <code>inputevt</code> element that may be find in <code>content</code> part of a Jingle media negotiation.
 *
 * @author Sebastien Vincent
 * @author Eng Chong Meng
 */
public class InputEvent extends AbstractXmlElement {
    /**
     * Name of the XML element representing the extension.
     */
    public static final String ELEMENT = "inputevt";

    public static final String NAMESPACE = "http://jitsi.org/protocol/inputevt";

    public InputEvent() {
        super(getBuilder());
    }

    public InputEvent(Builder builder) {
        super(builder);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for InputEvent. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the InputEvent.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, InputEvent> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        @Override
        public InputEvent build() {
            return new InputEvent(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
