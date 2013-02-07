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

package org.jivesoftware.smack.filter;

import junit.framework.TestCase;
import org.jivesoftware.smack.packet.*;

/**
 * A test case for the OrFilter class.
 */
public class OrFilterTest extends TestCase {

    public void testNullArgs() {
        PacketFilter filter = null;
        try {
            OrFilter or = new OrFilter(filter, filter);
            fail("Should have thrown IllegalArgumentException");
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void testAccept() {
        MockPacketFilter trueFilter = new MockPacketFilter(true);
        MockPacketFilter falseFilter = new MockPacketFilter(false);

        MockPacket packet = new MockPacket();

        // Testing TT == T
        OrFilter orFilter = new OrFilter(trueFilter, trueFilter);
        assertTrue(orFilter.accept(packet));

        // Testing TF = F
        orFilter = new OrFilter(trueFilter, falseFilter);
        assertTrue(orFilter.accept(packet));

        // Testing FT = F
        orFilter = new OrFilter(falseFilter, trueFilter);
        assertTrue(orFilter.accept(packet));

        // Testing FF = F
        orFilter = new OrFilter(falseFilter, falseFilter);
        assertFalse(orFilter.accept(packet));

        // Testing TTTT = T
        orFilter = new OrFilter(
            new OrFilter(trueFilter, trueFilter), new OrFilter(trueFilter, trueFilter)
        );
        assertTrue(orFilter.accept(packet));

        // Testing TFTT = F
        orFilter = new OrFilter(
            new OrFilter(trueFilter, falseFilter), new OrFilter(trueFilter, trueFilter)
        );
        assertTrue(orFilter.accept(packet));

        // Testing TTFT = F
        orFilter = new OrFilter(
            new OrFilter(trueFilter, trueFilter), new OrFilter(falseFilter, trueFilter)
        );
        assertTrue(orFilter.accept(packet));

        // Testing TTTF = F
        orFilter = new OrFilter(
            new OrFilter(trueFilter, trueFilter), new OrFilter(trueFilter, falseFilter)
        );
        assertTrue(orFilter.accept(packet));

        // Testing FFFF = F
        orFilter = new OrFilter(
            new OrFilter(falseFilter, falseFilter), new OrFilter(falseFilter, falseFilter)
        );
        assertFalse(orFilter.accept(packet));

        orFilter = new OrFilter();
        orFilter.addFilter(trueFilter);
        orFilter.addFilter(trueFilter);
        orFilter.addFilter(falseFilter);
        orFilter.addFilter(trueFilter);
        assertTrue(orFilter.accept(packet));
    }
}
