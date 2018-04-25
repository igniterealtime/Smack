/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.muclight;

import java.util.HashMap;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.muclight.element.MUCLightBlockingIQ;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

public class MUCLightBlockingTest {

    private static final String getBlockingListIQExample = "<iq to='muclight.shakespeare.lit' id='getblock1' type='get'>"
            + "<query xmlns='urn:xmpp:muclight:0#blocking'>" + "</query>" + "</iq>";

    private static final String getBlockingListIQResponse = "<iq type='result' id='getblock1' to='crone1@shakespeare.lit/desktop' from='muclight.shakespeare.lit'>"
            + "<query xmlns='urn:xmpp:muclight:0#blocking'>"
            + "<room action='deny'>coven@muclight.shakespeare.lit</room>"
            + "<room action='deny'>sarasa@muclight.shakespeare.lit</room>"
            + "<user action='deny'>hag77@shakespeare.lit</user>" + "</query>" + "</iq>";

    private static final String blockingRoomsIQExample = "<iq to='muclight.shakespeare.lit' id='block1' type='set'>"
            + "<query xmlns='urn:xmpp:muclight:0#blocking'>"
            + "<room action='deny'>coven@muclight.shakespeare.lit</room>"
            + "<room action='deny'>chapel@shakespeare.lit</room>" + "</query>" + "</iq>";

    private static final String blockingUsersIQExample = "<iq to='muclight.shakespeare.lit' id='block2' type='set'>"
            + "<query xmlns='urn:xmpp:muclight:0#blocking'>" + "<user action='deny'>hag77@shakespeare.lit</user>"
            + "<user action='deny'>hag66@shakespeare.lit</user>" + "</query>" + "</iq>";

    private static final String unblockingUsersAndRoomsExample = "<iq to='muclight.shakespeare.lit' id='unblock1' type='set'>"
            + "<query xmlns='urn:xmpp:muclight:0#blocking'>"
            + "<room action='allow'>coven@muclight.shakespeare.lit</room>"
            + "<user action='allow'>hag66@shakespeare.lit</user>" + "</query>" + "</iq>";

    @Test
    public void checkGetBlockingListIQ() throws Exception {
        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(null, null);
        mucLightBlockingIQ.setType(Type.get);
        mucLightBlockingIQ.setStanzaId("getblock1");
        mucLightBlockingIQ.setTo(JidCreate.from("muclight.shakespeare.lit"));

        Assert.assertEquals(getBlockingListIQExample, mucLightBlockingIQ.toXML(null).toString());
    }

    @Test
    public void checkGetBlockingListResponse() throws Exception {
        IQ iqInfoResult = PacketParserUtils.parseStanza(getBlockingListIQResponse);
        MUCLightBlockingIQ mucLightBlockingIQ = (MUCLightBlockingIQ) iqInfoResult;

        Assert.assertEquals(2, mucLightBlockingIQ.getRooms().size());
        Assert.assertEquals(1, mucLightBlockingIQ.getUsers().size());
        Assert.assertEquals(false, mucLightBlockingIQ.getRooms().get(JidCreate.from("coven@muclight.shakespeare.lit")));
        Assert.assertEquals(false,
                mucLightBlockingIQ.getRooms().get(JidCreate.from("sarasa@muclight.shakespeare.lit")));
        Assert.assertEquals(false, mucLightBlockingIQ.getUsers().get(JidCreate.from("hag77@shakespeare.lit")));
    }

    @Test
    public void checkBlockRoomsIQ() throws Exception {
        HashMap<Jid, Boolean> rooms = new HashMap<>();
        rooms.put(JidCreate.from("coven@muclight.shakespeare.lit"), false);
        rooms.put(JidCreate.from("chapel@shakespeare.lit"), false);

        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(rooms, null);
        mucLightBlockingIQ.setType(Type.set);
        mucLightBlockingIQ.setTo(JidCreate.from("muclight.shakespeare.lit"));
        mucLightBlockingIQ.setStanzaId("block1");

        Assert.assertEquals(blockingRoomsIQExample, mucLightBlockingIQ.toXML(null).toString());
    }

    @Test
    public void checkBlockUsersIQ() throws Exception {
        HashMap<Jid, Boolean> users = new HashMap<>();
        users.put(JidCreate.from("hag77@shakespeare.lit"), false);
        users.put(JidCreate.from("hag66@shakespeare.lit"), false);

        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(null, users);
        mucLightBlockingIQ.setType(Type.set);
        mucLightBlockingIQ.setTo(JidCreate.from("muclight.shakespeare.lit"));
        mucLightBlockingIQ.setStanzaId("block2");

        Assert.assertEquals(blockingUsersIQExample, mucLightBlockingIQ.toXML(null).toString());
    }

    @Test
    public void checkUnblockUsersAndRoomsIQ() throws Exception {
        HashMap<Jid, Boolean> users = new HashMap<>();
        users.put(JidCreate.from("hag66@shakespeare.lit"), true);

        HashMap<Jid, Boolean> rooms = new HashMap<>();
        rooms.put(JidCreate.from("coven@muclight.shakespeare.lit"), true);

        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(rooms, users);
        mucLightBlockingIQ.setType(Type.set);
        mucLightBlockingIQ.setTo(JidCreate.from("muclight.shakespeare.lit"));
        mucLightBlockingIQ.setStanzaId("unblock1");

        Assert.assertEquals(unblockingUsersAndRoomsExample, mucLightBlockingIQ.toXML(null).toString());
    }

}
