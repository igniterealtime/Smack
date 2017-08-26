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
import org.jivesoftware.smackx.jingle.callback.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;

/**
 * Class that represents a contents transport component.
 */
public abstract class JingleTransport<D extends JingleContentTransportElement> {

    private JingleContent parent;

    /**
     * List of transport candidates that we proposed to the peer.
     */
    private final ArrayList<JingleTransportCandidate<?>> ourCandidates = new ArrayList<>();

    /**
     * List of transport candidates that our peer proposed to us.
     */
    private final ArrayList<JingleTransportCandidate<?>> theirCandidates = new ArrayList<>();

    /**
     * The {@link BytestreamSession} we try to establish.
     */
    protected BytestreamSession bytestreamSession;

    /**
     * Return a {@link JingleContentTransportElement} which represents the state of this.
     * @return element.
     */
    public abstract D getElement();

    /**
     * Add a {@link JingleTransportCandidate} to the list of our proposed candidates.
     * The insertion is made sorted with descending priority.
     * @param candidate candidate.
     */
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

    /**
     * Add a {@link JingleTransportCandidate} to the list of their proposed candidates.
     * The insertion is made sorted with descending priority.
     * @param candidate candidate.
     */
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

    /**
     * Prepare the transport (can be used to register listeners etc.).
     * @param connection connection.
     */
    public abstract void prepare(XMPPConnection connection);

    /**
     * Return the list of {@link JingleTransportCandidate}s we proposed.
     * @return our candidates
     */
    public List<JingleTransportCandidate<?>> getOurCandidates() {
        return ourCandidates;
    }

    /**
     * Return the list of {@link JingleTransportCandidate}s our peer proposed to us.
     * @return their candidates.
     */
    public List<JingleTransportCandidate<?>> getTheirCandidates() {
        return theirCandidates;
    }

    /**
     * Return the namespace of this transport.
     * @return namespace.
     */
    public abstract String getNamespace();

    /**
     * Establish a incoming {@link BytestreamSession} with peer.
     * On success, call {@link JingleTransportCallback#onTransportReady(BytestreamSession)}.
     * On failure, call {@link JingleTransportCallback#onTransportFailed(Exception)}.
     * @param connection connection
     * @param callback callback
     * @param session {@link JingleSession} in which's context we try to connect.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     */
    public abstract void establishIncomingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException;

    /**
     * Establish a outgoing {@link BytestreamSession} with peer.
     * On success, call {@link JingleTransportCallback#onTransportReady(BytestreamSession)}.
     * On failure, call {@link JingleTransportCallback#onTransportFailed(Exception)}.
     * @param connection connection
     * @param callback callback
     * @param session {@link JingleSession} in which's context we try to connect.
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     */
    public abstract void establishOutgoingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException;

    /**
     * Handle an incoming transport-info request.
     * @param info info
     * @param wrapping wrapping {@link JingleElement}.
     * @return result.
     */
    public abstract IQ handleTransportInfo(JingleContentTransportInfoElement info, JingleElement wrapping);

    /**
     * Set the parent {@link JingleContent} of this transport component.
     * @param parent content.
     */
    public void setParent(JingleContent parent) {
        this.parent = parent;
    }

    /**
     * Return the parent {@link JingleContent} of this transport component.
     * @return content.
     */
    public JingleContent getParent() {
        return parent;
    }

    /**
     * Handle an incoming session-accept request.
     * @param transportElement the {@link JingleContentTransportElement} we received
     * @param connection connection.
     */
    public abstract void handleSessionAccept(JingleContentTransportElement transportElement, XMPPConnection connection);

    /**
     * Shut down components etc.
     */
    public abstract void cleanup();
}
