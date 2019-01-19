/**
 *
 * Copyright 2019 Vincent Lau
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
package org.jivesoftware.smackx.muc;

import org.jxmpp.jid.parts.Resourcepart;

public interface AutoJoinSuccessCallback {

    /**
     * Invoked after the automatic rejoin rooms on reconnect success.
     *
     * @param muc the joined MultiUserChat.
     * @param nickname nickname used by participant to join the room.
     */
    void autoJoinSuccess(MultiUserChat muc, Resourcepart nickname);

}
