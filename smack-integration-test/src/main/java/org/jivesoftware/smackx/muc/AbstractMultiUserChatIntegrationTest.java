/**
 *
 * Copyright 2021 Florian Schmaus
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

import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Localpart;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;


public class AbstractMultiUserChatIntegrationTest extends AbstractSmackIntegrationTest {

    final String randomString = StringUtils.insecureRandomString(6);

    final MultiUserChatManager mucManagerOne;
    final MultiUserChatManager mucManagerTwo;
    final MultiUserChatManager mucManagerThree;
    final DomainBareJid mucService;

    public AbstractMultiUserChatIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            InterruptedException, TestNotPossibleException {
        super(environment);
        mucManagerOne = MultiUserChatManager.getInstanceFor(conOne);
        mucManagerTwo = MultiUserChatManager.getInstanceFor(conTwo);
        mucManagerThree = MultiUserChatManager.getInstanceFor(conThree);

        List<DomainBareJid> services = mucManagerOne.getMucServiceDomains();
        if (services.isEmpty()) {
            throw new TestNotPossibleException("No MUC (XEP-45) service found");
        }
        else {
            mucService = services.get(0);
        }
    }

    /**
     * Gets a random room name.
     *
     * @param prefix A prefix to add to the room name for descriptive purposes
     * @return the bare JID of a random room
     * @throws XmppStringprepException if the prefix isn't a valid XMPP Localpart
     */
    public EntityBareJid getRandomRoom(String prefix) throws XmppStringprepException {
        final String roomNameLocal = String.join("-", prefix, testRunId, StringUtils.insecureRandomString(6));
        return JidCreate.entityBareFrom(Localpart.from(roomNameLocal), mucService.getDomain());
    }

    /**
     * Destroys a MUC room, ignoring any exceptions.
     *
     * @param muc The room to destroy (can be null).
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     */
    static void tryDestroy(final MultiUserChat muc) throws SmackException.NoResponseException, XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException {
        if (muc == null) {
            return;
        }
        muc.destroy("test fixture teardown", null);
    }

    static void createMuc(MultiUserChat muc, Resourcepart resourceName) throws
            SmackException.NoResponseException, XMPPException.XMPPErrorException,
            InterruptedException, MultiUserChatException.MucAlreadyJoinedException,
            SmackException.NotConnectedException,
            MultiUserChatException.MissingMucCreationAcknowledgeException,
            MultiUserChatException.NotAMucServiceException {
        MultiUserChat.MucCreateConfigFormHandle handle = muc.create(resourceName);
        if (handle != null) {
            handle.makeInstant();
        }
    }

    static void createMuc(MultiUserChat muc, String nickname) throws
            XmppStringprepException, MultiUserChatException.MucAlreadyJoinedException,
            XMPPException.XMPPErrorException, SmackException.NotConnectedException,
            MultiUserChatException.MissingMucCreationAcknowledgeException,
            SmackException.NoResponseException, InterruptedException,
            MultiUserChatException.NotAMucServiceException {
        createMuc(muc, Resourcepart.from(nickname));
    }

    static void createMembersOnlyMuc(MultiUserChat muc, Resourcepart resourceName) throws
            SmackException.NoResponseException, XMPPException.XMPPErrorException,
            InterruptedException, MultiUserChatException.MucAlreadyJoinedException,
            SmackException.NotConnectedException,
            MultiUserChatException.MissingMucCreationAcknowledgeException,
            MultiUserChatException.MucConfigurationNotSupportedException,
            MultiUserChatException.NotAMucServiceException {
        MultiUserChat.MucCreateConfigFormHandle handle = muc.create(resourceName);
        if (handle != null) {
            handle.getConfigFormManager().makeMembersOnly().submitConfigurationForm();
        }
    }

    static void createModeratedMuc(MultiUserChat muc, Resourcepart resourceName)
                    throws SmackException.NoResponseException, XMPPException.XMPPErrorException, InterruptedException,
                    MultiUserChatException.MucAlreadyJoinedException, SmackException.NotConnectedException,
                    MultiUserChatException.MissingMucCreationAcknowledgeException,
                    MultiUserChatException.NotAMucServiceException,
                    MultiUserChatException.MucConfigurationNotSupportedException {
        MultiUserChat.MucCreateConfigFormHandle handle = muc.create(resourceName);
        handle.getConfigFormManager().makeModerated().submitConfigurationForm();
    }

    static void createHiddenMuc(MultiUserChat muc, Resourcepart resourceName)
                    throws SmackException.NoResponseException, XMPPException.XMPPErrorException, InterruptedException,
                    MultiUserChatException.MucAlreadyJoinedException, SmackException.NotConnectedException,
                    MultiUserChatException.MissingMucCreationAcknowledgeException, MultiUserChatException.NotAMucServiceException, XmppStringprepException,
                    MultiUserChatException.MucConfigurationNotSupportedException {
        MultiUserChat.MucCreateConfigFormHandle handle = muc.create(resourceName);
        handle.getConfigFormManager().makeHidden().submitConfigurationForm();
    }
}
