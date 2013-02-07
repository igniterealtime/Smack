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
/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
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

package org.jivesoftware.smack.filter;

import junit.framework.TestCase;
import org.jivesoftware.smack.packet.*;

/**
 * Test cases for the PacketTypeFilter class.
 */
public class PacketTypeFilterTest extends TestCase {

    private class InnerClassDummy {
        public class DummyPacket extends Packet {
            public String toXML() {
                return null;
            }
        }
        public DummyPacket getInnerInstance() {
            return new DummyPacket();
        }
    }

    private static class StaticInnerClassDummy {
        public static class StaticDummyPacket extends Packet {
            public String toXML() {
                return null;
            }
        }
        public static StaticDummyPacket getInnerInstance() {
            return new StaticDummyPacket();
        }
    }

    /**
     * Test case for the constructor of PacketTypeFilter objects.
     */
    public void testConstructor() {
        // We dont need to test this since PacketTypeFilter(Class<? extends Packet> packetType) only excepts Packets
        // Test a class that is not a subclass of Packet
        // try {
        // new PacketTypeFilter(Dummy.class);
        // fail("Parameter must be a subclass of Packet.");
        // }
        // catch (IllegalArgumentException e) {}

        // Test a class that is a subclass of Packet
        try {
            new PacketTypeFilter(MockPacket.class);
        }
        catch (IllegalArgumentException e) {
            fail();
        }

        // Test another class which is a subclass of Packet
        try {
            new PacketTypeFilter(IQ.class);
        }
        catch (IllegalArgumentException e) {
            fail();
        }

        // Test an internal class which is a subclass of Packet
        try {
            new PacketTypeFilter(InnerClassDummy.DummyPacket.class);
        }
        catch (IllegalArgumentException e) {
            fail();
        }

        // Test an internal static class which is a static subclass of Packet
        try {
            new PacketTypeFilter(StaticInnerClassDummy.StaticDummyPacket.class);
        }
        catch (IllegalArgumentException e) {
            fail();
        }
    }

    /**
     * Test case to test the accept() method of PacketTypeFilter objects.
     */
    public void testAccept() {
        Packet packet = new MockPacket();
        PacketTypeFilter filter = new PacketTypeFilter(MockPacket.class);
        assertTrue(filter.accept(packet));

        packet = (new InnerClassDummy()).getInnerInstance();
        filter = new PacketTypeFilter(InnerClassDummy.DummyPacket.class);
        assertTrue(filter.accept(packet));

        packet = StaticInnerClassDummy.getInnerInstance();
        filter = new PacketTypeFilter(StaticInnerClassDummy.StaticDummyPacket.class);
        assertTrue(filter.accept(packet));
    }
}
