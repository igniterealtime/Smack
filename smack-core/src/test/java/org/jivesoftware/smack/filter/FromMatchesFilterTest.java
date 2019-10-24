/**
 *
 * Copyright 2011 Robin Collier
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
package org.jivesoftware.smack.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaBuilder;

import org.junit.Test;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.JidTestUtil;

/**
 * From matches filter test.
 * @author Robin Collier
 *
 */
public class FromMatchesFilterTest {
    private static final Jid BASE_JID1 = JidTestUtil.BARE_JID_1;
    private static final EntityFullJid FULL_JID1_R1 = JidTestUtil.FULL_JID_1_RESOURCE_1;
    private static final EntityFullJid FULL_JID1_R2 = JidTestUtil.FULL_JID_1_RESOURCE_2;
    private static final Jid BASE_JID2 = JidTestUtil.BARE_JID_2;
    private static final Jid FULL_JID2 = JidTestUtil.FULL_JID_2_RESOURCE_1;

    private static final Jid BASE_JID3 = JidTestUtil.DUMMY_AT_EXAMPLE_ORG;

    private static final Jid SERVICE_JID1 = JidTestUtil.MUC_EXAMPLE_ORG;
    private static final Jid SERVICE_JID2 = JidTestUtil.PUBSUB_EXAMPLE_ORG;

    @Test
    public void autoCompareMatchingEntityFullJid() {
        FromMatchesFilter filter = FromMatchesFilter.create(FULL_JID1_R1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(FULL_JID1_R1);
        assertTrue(filter.accept(packet));

        packet.setFrom(BASE_JID1);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID1_R2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
    }

    @Test
    public void autoCompareMatchingBaseJid() {
        FromMatchesFilter filter = FromMatchesFilter.create(BASE_JID1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(BASE_JID1);
        assertTrue(filter.accept(packet));

        packet.setFrom(FULL_JID1_R1);
        assertTrue(filter.accept(packet));

        packet.setFrom(FULL_JID1_R2);
        assertTrue(filter.accept(packet));

        packet.setFrom(BASE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
}

    @Test
    public void autoCompareMatchingServiceJid() {
        FromMatchesFilter filter = FromMatchesFilter.create(SERVICE_JID1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(SERVICE_JID1);
        assertTrue(filter.accept(packet));

        packet.setFrom(SERVICE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID1);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID1_R1);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
    }

    @Test
    public void bareCompareMatchingEntityFullJid() {
        FromMatchesFilter filter = FromMatchesFilter.createBare(FULL_JID1_R1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(BASE_JID1);
        assertTrue(filter.accept(packet));

        packet.setFrom(FULL_JID1_R1);
        assertTrue(filter.accept(packet));

        packet.setFrom(FULL_JID1_R2);
        assertTrue(filter.accept(packet));

        packet.setFrom(BASE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
    }

    @Test
    public void bareCompareMatchingBaseJid() {
        FromMatchesFilter filter = FromMatchesFilter.createBare(BASE_JID1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(BASE_JID1);
        assertTrue(filter.accept(packet));

        packet.setFrom(FULL_JID1_R1);
        assertTrue(filter.accept(packet));

        packet.setFrom(FULL_JID1_R2);
        assertTrue(filter.accept(packet));

        packet.setFrom(BASE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
}

    @Test
    public void bareCompareMatchingServiceJid() {
        FromMatchesFilter filter = FromMatchesFilter.createBare(SERVICE_JID1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(SERVICE_JID1);
        assertTrue(filter.accept(packet));

        packet.setFrom(SERVICE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID1);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID1_R1);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
    }

    @Test
    public void fullCompareMatchingEntityFullJid() {
        FromMatchesFilter filter = FromMatchesFilter.createFull(FULL_JID1_R1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(FULL_JID1_R1);
        assertTrue(filter.accept(packet));

        packet.setFrom(BASE_JID1);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID1_R2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
    }

    @Test
    public void fullCompareMatchingBaseJid() {
        FromMatchesFilter filter = FromMatchesFilter.createFull(BASE_JID1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(BASE_JID1);
        assertTrue(filter.accept(packet));

        packet.setFrom(FULL_JID1_R1);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID1_R2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
    }

    @Test
    public void fullCompareMatchingServiceJid() {
        FromMatchesFilter filter = FromMatchesFilter.createFull(SERVICE_JID1);
        Stanza packet = StanzaBuilder.buildMessage().build();

        packet.setFrom(SERVICE_JID1);
        assertTrue(filter.accept(packet));

        packet.setFrom(SERVICE_JID2);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID1);
        assertFalse(filter.accept(packet));

        packet.setFrom(FULL_JID1_R1);
        assertFalse(filter.accept(packet));

        packet.setFrom(BASE_JID3);
        assertFalse(filter.accept(packet));
    }

}
