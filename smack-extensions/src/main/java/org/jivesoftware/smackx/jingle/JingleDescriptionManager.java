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
import org.jivesoftware.smackx.jingle.component.JingleDescription;
import org.jivesoftware.smackx.jingle.component.JingleSession;

/**
 * Manager for {@link JingleDescription} components.
 */
public interface JingleDescriptionManager {

    /**
     * Return the namespace of the {@link JingleDescription}.
     * @return namespace.
     */
    String getNamespace();

    /**
     * Notify about an incoming session-initiate wich contains a suitable {@link JingleDescription}.
     * @param session initiated jingleSession.
     */
    void notifySessionInitiate(JingleSession session);

    /**
     * Notify about a content-add request which tries to add a suitable {@link JingleDescription}.
     * @param session affected jingleSession.
     * @param content content which will be added.
     */
    void notifyContentAdd(JingleSession session, JingleContent content);
}
