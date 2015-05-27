/**
 *
 * Copyright 2003-2007 Jive Software.
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

import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Default implementation of the ParticipantStatusListener interface.<p>
 *
 * This class does not provide any behavior by default. It just avoids having
 * to implement all the inteface methods if the user is only interested in implementing
 * some of the methods.
 * 
 * @author Gaston Dombiak
 */
public class DefaultParticipantStatusListener implements ParticipantStatusListener {

    public void joined(EntityFullJid participant) {
    }

    public void left(EntityFullJid participant) {
    }

    public void kicked(EntityFullJid participant, Jid actor, String reason) {
    }

    public void voiceGranted(EntityFullJid participant) {
    }

    public void voiceRevoked(EntityFullJid participant) {
    }

    public void banned(EntityFullJid participant, Jid actor, String reason) {
    }

    public void membershipGranted(EntityFullJid participant) {
    }

    public void membershipRevoked(EntityFullJid participant) {
    }

    public void moderatorGranted(EntityFullJid participant) {
    }

    public void moderatorRevoked(EntityFullJid participant) {
    }

    public void ownershipGranted(EntityFullJid participant) {
    }

    public void ownershipRevoked(EntityFullJid participant) {
    }

    public void adminGranted(EntityFullJid participant) {
    }

    public void adminRevoked(EntityFullJid participant) {
    }

    public void nicknameChanged(EntityFullJid participant, Resourcepart newNickname) {
    }

}
