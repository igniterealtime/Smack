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
package org.jivesoftware.smackx.jingle.component;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.bytestreams.BytestreamSession;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Class that represents a contents transport component.
 */
public abstract class JingleTransport<D extends JingleContentTransportElement> {

    private JingleContent parent;
    private final ArrayList<JingleTransportCandidate<?>> ourCandidates = new ArrayList<>();
    private final ArrayList<JingleTransportCandidate<?>> theirCandidates = new ArrayList<>();

    protected BytestreamSession bytestreamSession;

    public abstract D getElement();

    public void addOurCandidate(JingleTransportCandidate<?> candidate) {
// Insert sorted by descending priority
        int i;
        // Find appropriate index
        for (i = 0; i < ourCandidates.size(); i++) {
            JingleTransportCandidate<?> c = ourCandidates.get(i);

            if (c == candidate || c.equals(candidate)) {
                c.setParent(this); // Candidate might equal, but not be same, so set parent just in case
                return;
            }

            if (c.getPriority() < candidate.getPriority()) {
                break;
            }
        }

        ourCandidates.add(i, candidate);
        candidate.setParent(this);
    }

    public void addTheirCandidate(JingleTransportCandidate<?> candidate) {
        // Insert sorted by descending priority
        int i;
        // Find appropriate index
        for (i = 0; i < theirCandidates.size(); i++) {
            JingleTransportCandidate<?> c = theirCandidates.get(i);

            if (c == candidate || c.equals(candidate)) {
                c.setParent(this); // Candidate might equal, but not be same, so set parent just in case
                return;
            }

            if (c.getPriority() < candidate.getPriority()) {
                break;
            }
        }

        theirCandidates.add(i, candidate);
        candidate.setParent(this);
    }

    public abstract void prepare(XMPPConnection connection);

    public List<JingleTransportCandidate<?>> getOurCandidates() {
        return ourCandidates;
    }

    public List<JingleTransportCandidate<?>> getTheirCandidates() {
        return theirCandidates;
    }

    public abstract String getNamespace();

    public abstract void establishIncomingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException;

    public abstract void establishOutgoingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException;

    public abstract IQ handleTransportInfo(JingleContentTransportInfoElement info, JingleElement wrapping);

    public void setParent(JingleContent parent) {
        this.parent = parent;
    }

    public JingleContent getParent() {
        return parent;
    }

    public abstract void handleSessionAccept(JingleContentTransportElement transportElement, XMPPConnection connection);

    public abstract void cleanup();
}
