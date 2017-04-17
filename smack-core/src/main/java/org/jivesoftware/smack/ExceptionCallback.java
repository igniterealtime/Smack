/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Stanza;

public interface ExceptionCallback {

    /**
     * Exception callback.
     * @param exception the exception that was encountered.
     * @param stanza the stanza with the error, can be null if the problem
     *               is not reported from the server, but internal like parsing
     *               or being disconnected while waiting for result.
     */
    public void processException(Exception exception, Stanza stanza);

}
