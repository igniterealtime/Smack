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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;

/**
 * Manager for JingleTransport components.
 */
public interface JingleTransportManager extends Comparable<JingleTransportManager> {

    String getNamespace();

    JingleTransport<?> createTransportForInitiator(JingleContent content);

    JingleTransport<?> createTransportForResponder(JingleContent content, JingleTransport<?> peersTransport);

    JingleTransport<?> createTransportForResponder(JingleContent content, JingleContentTransportElement peersTransportElement);

    /**
     * Return a (usually) positive integer, which is used to define a strict order over the set of available transport
     * managers.
     * @return priority.
     */
    int getPriority();
}
