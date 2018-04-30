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

import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidateElement;

/**
 * Class that represents a transports candidate component.
 */
public abstract class JingleTransportCandidate<E extends JingleContentTransportCandidateElement> {

    /**
     * Parent {@link JingleTransport}, which contains this as a child candidate.
     */
    private JingleTransport<?> parent;

    /**
     * Priority of this candidate.
     */
    private int priority;

    /**
     * Set the parent {@link JingleTransport}.
     * @param transport parent.
     */
    public void setParent(JingleTransport<?> transport) {
        if (parent != transport) {
            parent = transport;
        }
    }

    /**
     * Return the parent {@link JingleTransport}.
     * @return parent transport.
     */
    public JingleTransport<?> getParent() {
        return parent;
    }

    /**
     * Return the priority of this candidate.
     * @return priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Set the priority of this candidate.
     * @param priority priority.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Return an {@link JingleContentTransportCandidateElement} which represents this.
     * @return element.
     */
    public abstract E getElement();
}
