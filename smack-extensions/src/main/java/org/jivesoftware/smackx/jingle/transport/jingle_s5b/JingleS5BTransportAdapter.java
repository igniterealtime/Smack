/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b;

import java.util.ArrayList;

import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidateElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportElement;

/**
 * Adapter for Jingle SOCKS5Bytestream components.
 */
public class JingleS5BTransportAdapter implements JingleTransportAdapter<JingleS5BTransport> {

    @Override
    public JingleS5BTransport transportFromElement(JingleContentTransportElement element) {
        JingleS5BTransportElement s5b = (JingleS5BTransportElement) element;
        ArrayList<JingleTransportCandidate<?>> candidates = new ArrayList<>();

        for (JingleContentTransportCandidateElement e : element.getCandidates()) {
            candidates.add(JingleS5BTransportCandidate.fromElement((JingleS5BTransportCandidateElement) e));
        }

        return new JingleS5BTransport(s5b.getStreamId(), s5b.getMode(), null, s5b.getDestinationAddress(), null, candidates);
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE;
    }
}
