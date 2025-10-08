/*
 *
 * Copyright 2024 Guus der Kinderen
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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;

import org.jivesoftware.smackx.muc.packet.MUCUser;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Tests that verify the correct functionality of Smack's {@link UserStatusListener}.
 */
@SpecificationReference(document = "XEP-0045", version = "1.34.6")
public class UserStatusIntegrationTest extends AbstractMultiUserChatIntegrationTest {

    public UserStatusIntegrationTest(SmackIntegrationTestEnvironment environment) throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, TestNotPossibleException, MultiUserChatException.MucAlreadyJoinedException, MultiUserChatException.MissingMucCreationAcknowledgeException, XmppStringprepException, MultiUserChatException.NotAMucServiceException {
        super(environment);
    }

    /**
     * Verifies that when a member gets its membership removed in an open room, the appropriate event listener is invoked.
     *
     * @throws Exception On unexpected results
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership [...] The service MUST then send updated presence from this individual to all occupants, indicating the loss of membership by sending a presence element that contains an <x/> element qualified by the 'http://jabber.org/protocol/muc#user' namespace and containing an <item/> child with the 'affiliation' attribute set to a value of \"none\".")
    public void testMembershipRevokedInOpenRoom() throws Exception {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("userstatus-membership-revoked-membersonly");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final EntityFullJid mucAddressOwner = JidCreate.entityFullFrom(mucAddress, Resourcepart.from("owner-" + randomString));
        final EntityFullJid mucAddressTarget = JidCreate.entityFullFrom(mucAddress, Resourcepart.from("target-" + randomString));

        createMuc(mucAsSeenByOwner, mucAddressOwner.getResourcepart());
        try {
            mucAsSeenByOwner.grantMembership(conTwo.getUser().asBareJid());
            mucAsSeenByTarget.join(mucAddressTarget.getResourcepart());

            final SimpleResultSyncPoint targetSeesRevoke = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void membershipRevoked() {
                    targetSeesRevoke.signal();
                }
            });

            // Execute system under test.
            mucAsSeenByOwner.revokeMembership(conTwo.getUser().asBareJid());

            // Verify result.
            assertResult(targetSeesRevoke, "Expected '" + conTwo.getUser() + "' (using nickname '" + mucAddressTarget.getResourcepart() + "') to be notified that their membership status was removed by '" + conOne.getUser() + "' (using nickname '" + mucAddressOwner.getResourcepart() + "') in '" + mucAddress + "' (but did not).");
        } finally {
            // Clean up test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }

    /**
     * Verifies that when a member gets its membership removed in a members-only room, the appropriate event listeners are invoked.
     *
     * @throws Exception On unexpected results
     */
    @SmackIntegrationTest(section = "9.4", quote = "An admin might want to revoke a user's membership [...] If the room is members-only, the service MUST remove the user from the room, including a status code of 321 to indicate that the user was removed because of an affiliation change, and inform all remaining occupants")
    public void testMembershipRevokedInMemberOnlyRoom() throws Exception {
        // Setup test fixture.
        final EntityBareJid mucAddress = getRandomRoom("userstatus-membership-revoked-membersonly");
        final MultiUserChat mucAsSeenByOwner = mucManagerOne.getMultiUserChat(mucAddress);
        final MultiUserChat mucAsSeenByTarget = mucManagerTwo.getMultiUserChat(mucAddress);

        final EntityFullJid mucAddressOwner = JidCreate.entityFullFrom(mucAddress, Resourcepart.from("owner-" + randomString));
        final EntityFullJid mucAddressTarget = JidCreate.entityFullFrom(mucAddress, Resourcepart.from("target-" + randomString));

        createMembersOnlyMuc(mucAsSeenByOwner, mucAddressOwner.getResourcepart());
        try {
            mucAsSeenByOwner.grantMembership(conTwo.getUser().asBareJid());
            mucAsSeenByTarget.join(mucAddressTarget.getResourcepart());

            final SimpleResultSyncPoint targetSeesRevoke = new SimpleResultSyncPoint();
            final SimpleResultSyncPoint targetSeesRemove = new SimpleResultSyncPoint();
            mucAsSeenByTarget.addUserStatusListener(new UserStatusListener() {
                @Override
                public void removed(MUCUser mucUser, Presence presence) {
                    targetSeesRemove.signal();
                }

                @Override
                public void membershipRevoked() {
                    targetSeesRevoke.signal();
                }
            });

            // Execute system under test.
            mucAsSeenByOwner.revokeMembership(conTwo.getUser().asBareJid());

            // Verify result.
            assertResult(targetSeesRemove, "Expected '" + conTwo.getUser() + "' (using nickname '" + mucAddressTarget.getResourcepart() + "') to be notified that it is removed from '" + mucAddress + "' which is a member-only room, as their membership status was removed by '" + conOne.getUser() + "' (using nickname '" + mucAddressOwner.getResourcepart() + "') (but did not).");
            assertResult(targetSeesRevoke, "Expected '" + conTwo.getUser() + "' (using nickname '" + mucAddressTarget.getResourcepart() + "') to be notified that their membership status was removed by '" + conOne.getUser() + "' (using nickname '" + mucAddressOwner.getResourcepart() + "') in '" + mucAddress + "' (but did not).");
        } finally {
            // Clean up test fixture.
            tryDestroy(mucAsSeenByOwner);
        }
    }
}
