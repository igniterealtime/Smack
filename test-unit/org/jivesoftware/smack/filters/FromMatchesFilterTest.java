/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
package org.jivesoftware.smack.filters;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.packet.Packet;
import org.junit.Test;

public class FromMatchesFilterTest {
    private static final String BASE_JID1 = "ss@muc.myserver.com";
    private static final String FULL_JID1_R1 = BASE_JID1 + "/resource";
    private static final String FULL_JID1_R2 = BASE_JID1 + "/resource2";
    private static final String BASE_JID2 = "sss@muc.myserver.com";
    private static final String FULL_JID2 = BASE_JID2 + "/resource";

    private static final String SERVICE_JID1 = "muc.myserver.com";
    private static final String SERVICE_JID2 = "pubsub.myserver.com";

    @Test
    public void compareMatchingFullJid()
    {
	FromMatchesFilter filter = new FromMatchesFilter(FULL_JID1_R1);
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
    }

    @Test
    public void compareMatchingBaseJid()
    {
	FromMatchesFilter filter = new FromMatchesFilter(BASE_JID1);
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
    }

    @Test
    public void compareMatchingServiceJid()
    {
	FromMatchesFilter filter = new FromMatchesFilter(SERVICE_JID1);
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

    }
}
