/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

    public void joined(String participant) {
    }

    public void left(String participant) {
    }

    public void kicked(String participant, String actor, String reason) {
    }

    public void voiceGranted(String participant) {
    }

    public void voiceRevoked(String participant) {
    }

    public void banned(String participant, String actor, String reason) {
    }

    public void membershipGranted(String participant) {
    }

    public void membershipRevoked(String participant) {
    }

    public void moderatorGranted(String participant) {
    }

    public void moderatorRevoked(String participant) {
    }

    public void ownershipGranted(String participant) {
    }

    public void ownershipRevoked(String participant) {
    }

    public void adminGranted(String participant) {
    }

    public void adminRevoked(String participant) {
    }

    public void nicknameChanged(String participant, String newNickname) {
    }

}
