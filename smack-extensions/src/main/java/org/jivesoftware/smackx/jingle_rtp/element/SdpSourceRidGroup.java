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
 * Represents <code>rid-group</code> elements which we use to represent a simulcast sdp lines (from unified plan).
 *
 * @author Brian Baldino
 * @author Eng Chong Meng
 */
public class SdpSourceRidGroup extends SdpSourceGroup {
    /**
     * The name of the "rid-group" element.
     */
    public static final String ELEMENT = "rid-group";

    /**
     * <code>SdpSourceRidGroup</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public SdpSourceRidGroup() {
        super(getBuilder());
    }

    /**
     * Initializes a new <code>SdpSourceRidGroup</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SdpSourceRidGroup(Builder builder) {
        super(builder);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SdpSourceRidGroup. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SdpSourceRidGroup.
     */
    public static final class Builder extends SdpSourceGroup.Builder {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        @Override
        public SdpSourceRidGroup build() {
            return new SdpSourceRidGroup(this);
        }

        @Override
        public SdpSourceRidGroup.Builder getThis() {
            return this;
        }
    }
}
