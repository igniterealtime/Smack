/**
 *
 * Copyright 2019 Aditya Borikar.
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
package org.jivesoftware.smackx.usertune;

import java.net.URI;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.usertune.element.UserTuneElement;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.BareJid;

public class UserTuneIntegrationTest extends AbstractSmackIntegrationTest {

    private final UserTuneManager utm1;
    private final UserTuneManager utm2;

    public UserTuneIntegrationTest(SmackIntegrationTestEnvironment environment) throws NotLoggedInException {
        super(environment);
        utm1 = UserTuneManager.getInstanceFor(conOne);
        utm2 = UserTuneManager.getInstanceFor(conTwo);
    }

    @SmackIntegrationTest
    public void test() throws Exception {
        URI uri = new URI("http://www.yesworld.com/lyrics/Fragile.html#9");
        UserTuneElement.Builder builder = UserTuneElement.getBuilder();
        UserTuneElement userTuneElement1 = builder.setArtist("Yes")
                                                  .setLength(686)
                                                  .setRating(8)
                                                  .setSource("Yessongs")
                                                  .setTitle("Heart of the Sunrise")
                                                  .setTrack("3")
                                                  .setUri(uri)
                                                  .build();

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint userTuneReceived = new SimpleResultSyncPoint();

        final UserTuneListener userTuneListener = new UserTuneListener() {
            @Override
            public void onUserTuneUpdated(BareJid jid, Message message, UserTuneElement userTuneElement) {
                if (userTuneElement.equals(userTuneElement1)) {
                    userTuneReceived.signal();
                }
            }
        };

        utm2.addUserTuneListener(userTuneListener);

        try {
            utm1.publishUserTune(userTuneElement1);
            userTuneReceived.waitForResult(timeout);
        } finally {
            utm2.removeUserTuneListener(userTuneListener);
        }
    }

    @AfterClass
    public void unsubscribe()
            throws SmackException.NotLoggedInException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }
}
