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
package org.jivesoftware.smack.filters;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;

/**
 * 
 * @author Robin Collier
 *
 */
public class FromMatchesFilterTest {
    private static final String BASE_JID1 = "ss@muc.myserver.com";
    private static final String FULL_JID1_R1 = BASE_JID1 + "/resource";
    private static final String FULL_JID1_R2 = BASE_JID1 + "/resource2";
    private static final String BASE_JID2 = "sss@muc.myserver.com";
    private static final String FULL_JID2 = BASE_JID2 + "/resource";

    private static final String BASE_JID3 = "ss@muc.myserver.comm.net";

    private static final String SERVICE_JID1 = "muc.myserver.com";
    private static final String SERVICE_JID2 = "pubsub.myserver.com";

    @Test
    public void autoCompareMatchingFullJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.create(FULL_JID1_R1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void autoCompareMatchingBaseJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.create(BASE_JID1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void autoCompareMatchingServiceJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.create(SERVICE_JID1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void bareCompareMatchingFullJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.createBare(FULL_JID1_R1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void bareCompareMatchingBaseJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.createBare(BASE_JID1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void bareCompareMatchingServiceJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.createBare(SERVICE_JID1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void fullCompareMatchingFullJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.createFull(FULL_JID1_R1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void fullCompareMatchingBaseJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.createFull(BASE_JID1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
    public void fullCompareMatchingServiceJid()
    {
        FromMatchesFilter filter = FromMatchesFilter.createFull(SERVICE_JID1);
        Packet packet = new Packet() {
            @Override
            public String toXML() { return null; }
        };

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
