/*
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smackx.jingle.adapter.JingleTransportAdapter;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;

/**
 * Adapter for Jingle SOCKS5Bytestream components.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleS5BTransportAdapter implements JingleTransportAdapter<JingleS5BTransportImpl> {

    @Override
    public JingleS5BTransportImpl transportFromElement(JingleContentTransport contentTransport) {
        JingleS5BTransport s5b = (JingleS5BTransport) contentTransport;
        List<JingleTransportCandidate<?>> candidates = new ArrayList<>();

        for (JingleContentTransportCandidate e : contentTransport.getCandidates()) {
            candidates.add(JingleS5BTransportCandidateImpl.fromElement((JingleS5BTransportCandidate) e));
        }

        return new JingleS5BTransportImpl(s5b.getStreamId(), s5b.getMode(), null, s5b.getDestinationAddress(), null, candidates);
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE_V1;
    }
}
