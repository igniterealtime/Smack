/**
 *
 * Copyright 2021 Dan Caseley
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;

public class MultiUserChatEntityIntegrationTest extends AbstractMultiUserChatIntegrationTest {

    public MultiUserChatEntityIntegrationTest(SmackIntegrationTestEnvironment environment)
                    throws SmackException.NoResponseException, XMPPException.XMPPErrorException,
                    SmackException.NotConnectedException, InterruptedException, TestNotPossibleException {
        super(environment);
    }

    /**
     * Asserts that a MUC service can be discovered
     *
     * <p>From XEP-0045 § 6:</p>
     * <blockquote>
     * A MUC implementation MUST support Service Discovery (XEP-0030) [9] ("disco").
     * </blockquote>
     *
     * <p>From XEP-0045 § 6.1:</p>
     * <blockquote>
     * An entity often discovers a MUC service by sending a Service Discovery items ("disco#items") request to its own
     * server. The server then returns the services that are associated with it.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestForDiscoveringMUC() throws Exception {
        DiscoverItems items = ServiceDiscoveryManager.getInstanceFor(conOne).discoverItems(conOne.getXMPPServiceDomain());
        // TODO: remove magic string, without making it disco=disco.
        assertTrue(items.getItems().stream().map(item -> item.toString()).anyMatch(i -> i.contains("conference")));
    }

    /**
     * Asserts that a MUC service can have its features discovered
     *
     * <p>From XEP-0045 § 6.2:</p>
     * <blockquote>
     * An entity may wish to discover if a service implements the Multi-User Chat protocol; in order to do so, it
     * sends a service discovery information ("disco#info") query to the MUC service's JID. The service MUST return
     * its identity and the features it supports.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestForDiscoveringFeatures() throws Exception {
        DiscoverInfo info = mucManagerOne.getMucServiceDiscoInfo(mucManagerOne.getMucServiceDomains().get(0));
        assertTrue(info.getIdentities().size() > 0);
        assertTrue(info.getFeatures().size() > 0);
    }

    /**
     * Asserts that a MUC Service lists its public rooms.
     *
     * <p>From XEP-0045 § 6.3:</p>
     * <blockquote>
     * The service discovery items ("disco#items") protocol enables an entity to query a service for a list of
     * associated items, which in the case of a chat service would consist of the specific chat rooms hosted by the
     * service. The service SHOULD return a full list of the public rooms it hosts (i.e., not return any rooms that
     * are hidden).
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestForDiscoveringRooms() throws Exception {
        EntityBareJid mucAddressPublic = getRandomRoom("smack-inttest-publicroom");
        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddressPublic);

        EntityBareJid mucAddressHidden = getRandomRoom("smack-inttest-hiddenroom");
        MultiUserChat mucAsSeenByTwo = mucManagerTwo.getMultiUserChat(mucAddressHidden);

        createMuc(mucAsSeenByOne, Resourcepart.from("one-" + randomString));
        createHiddenMuc(mucAsSeenByTwo, Resourcepart.from("two-" + randomString));

        try {
            Map<EntityBareJid, HostedRoom> rooms = mucManagerThree.getRoomsHostedBy(mucService);
            assertTrue(rooms.containsKey(mucAddressPublic));
            assertFalse(rooms.containsKey(mucAddressHidden));
        } finally {
            tryDestroy(mucAsSeenByOne);
            tryDestroy(mucAsSeenByTwo);
        }
    }

    /**
     * Asserts that a MUC Service returns disco info for a room.
     *
     * <p>From XEP-0045 § 6.4:</p>
     * <blockquote>
     * Using the disco#info protocol, an entity may also query a specific chat room for more detailed information
     * about the room....The room MUST return its identity and SHOULD return the features it supports
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestForDiscoveringRoomInfo() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-discoinfo");
        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(mucAsSeenByOne, Resourcepart.from("one-" + randomString));

        try {
            // Use SDM because mucManagerOne.getRoomInfo(mucAddress) might not use Disco
            DiscoverInfo discoInfo = ServiceDiscoveryManager.getInstanceFor(conOne).discoverInfo(mucAddress, null);
            assertTrue(discoInfo.getIdentities().size() > 0);
            assertTrue(discoInfo.getFeatures().size() > 0);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a MUC Service returns disco info for a room's items.
     *
     * <p>From XEP-0045 § 6.5:</p>
     * <blockquote>
     * An entity MAY also query a specific chat room for its associated items. An implementation MAY return a list
     * of existing occupants if that information is publicly available, or return no list at all if this information is
     * kept private.
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestForDiscoveringRoomItems() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-discoitems");
        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        createMuc(mucAsSeenByOne, Resourcepart.from("one-" + randomString));

        try {
            DiscoverItems roomItems = ServiceDiscoveryManager.getInstanceFor(conTwo).discoverItems(mucAddress, null);
            assertEquals(1, roomItems.getItems().size());
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }

    /**
     * Asserts that a non-occupant receives a Bad Request error when attempting to query an occupant by their
     * occupant JID.
     *
     * <p>From XEP-0045 § 6.6:</p>
     * <blockquote>
     * If a non-occupant attempts to send a disco request to an address of the form &lt;room@service/nick&gt;, a MUC service
     * MUST return a &lt;bad-request/&gt; error
     * </blockquote>
     *
     * @throws Exception when errors occur
     */
    @SmackIntegrationTest
    public void mucTestForRejectingDiscoOnRoomOccupantByNonOccupant() throws Exception {
        EntityBareJid mucAddress = getRandomRoom("smack-inttest-discoitems");
        MultiUserChat mucAsSeenByOne = mucManagerOne.getMultiUserChat(mucAddress);
        final Resourcepart nicknameOne = Resourcepart.from("one-" + randomString);
        createMuc(mucAsSeenByOne, nicknameOne);

        try {
            XMPPException.XMPPErrorException xe = assertThrows(XMPPException.XMPPErrorException.class,
                            () -> ServiceDiscoveryManager.getInstanceFor(conTwo).discoverItems(
                                            JidCreate.entityFullFrom(mucAddress, nicknameOne), null));
            assertEquals(xe.getStanzaError().getCondition(), StanzaError.Condition.bad_request);
        } finally {
            tryDestroy(mucAsSeenByOne);
        }
    }
}
