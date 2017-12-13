/**
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.jingleold.listeners;

import org.jivesoftware.smackx.jingleold.JingleSessionRequest;

/**
 * Interface to listener Jingle session requests.
 *
 * @author Alvaro Saurin
 */
public interface JingleSessionRequestListener extends JingleListener {
    /**
     * A request to start a session has been received from another user.
     *
     * @param request The request from the other user.
     */
    void sessionRequested(JingleSessionRequest request);
}
