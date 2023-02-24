/**
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
package org.jivesoftware.smackx.jingle.component;

import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;

/**
 * Class that represents a transports candidate component.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public abstract class JingleTransportCandidate<E extends JingleContentTransportCandidate> {

    private JingleTransport<?> parent;
    private int priority;

    public void setParent(JingleTransport<?> transport) {
        if (parent != transport) {
            parent = transport;
        }
    }

    public JingleTransport<?> getParent() {
        return parent;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) {
        this.priority = priority;
    }

    public abstract E getElement();
}
