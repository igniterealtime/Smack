/**
 *
 * Copyright 2009 Jonas Ådahl.
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

package org.jivesoftware.smack.serverless.service;

import org.jivesoftware.smack.serverless.LLPresence;


/**
 * Interface for receiving notifications about presence changes.
 */
public interface LLPresenceListener {
    /**
     * New link-local presence has been discovered.
     * 
     * @param presence information about the new presence
     */

    public void presenceNew(LLPresence presence);
    /**
     * A link-local presence has gone offline.
     * @param presence the presence which went offline.
     */
    public void presenceRemove(LLPresence presence);
}
