/**
 *
 * Copyright 2017-2019 Florian Schmaus
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

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

import java.util.Collections;
import java.util.List;

/**
 * A jingle transport extension.
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 */
public class JingleContentTransport extends AbstractXmlElement {
    public static final String ELEMENT = "transport";
    private static Builder mBuilder;

    protected List<JingleContentTransportCandidate> candidates;
    protected JingleContentTransportInfo info;

    public JingleContentTransport() {
        this(getBuilder());
    }

    /**
     * Creates a new <code>RtpDescription</code>.
     *
     * @param builder Builder instance
     */
    public JingleContentTransport(Builder builder) {
        super(builder);
    }

    protected JingleContentTransport(List<JingleContentTransportCandidate> candidates) {
        this(candidates, null);
    }

    protected JingleContentTransport(List<JingleContentTransportCandidate> candidates, JingleContentTransportInfo info) {
        super(mBuilder = getBuilder());
        if (candidates != null) {
            this.candidates = Collections.unmodifiableList(candidates);
        }
        else {
            this.candidates = Collections.emptyList();
        }
        this.info = info;

        mBuilder.addTransportCandidate(candidates)
                .addTransportInfo(info)
                .build();
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    public List<JingleContentTransportCandidate> getCandidates() {
        return candidates;
    }

    public JingleContentTransportInfo getInfo() {
        return info;
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, null);
    }

    /**
     * Builder for JingleContentTransport. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RtpDescription.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, JingleContentTransport> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder addTransportInfo(JingleContentTransportInfo info) {
            return (info == null) ? this : addChildElement(info);
        }

        public Builder addTransportCandidate(List<JingleContentTransportCandidate> xElements) {
            return addChildElements(xElements);
        }

        @Override
        public JingleContentTransport build() {
            return new JingleContentTransport(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
