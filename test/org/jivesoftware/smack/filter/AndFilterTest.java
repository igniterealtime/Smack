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
 * A test case for the AndFilter class.
 */
public class AndFilterTest extends TestCase {

    public void testNullArgs() {
        PacketFilter filter = null;
        try {
            new AndFilter(filter, filter);
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

        AndFilter andFilter = new AndFilter(trueFilter, trueFilter);
        assertTrue(andFilter.accept(packet));

        andFilter = new AndFilter(trueFilter, falseFilter);
        assertFalse(andFilter.accept(packet));

        andFilter = new AndFilter(falseFilter, trueFilter);
        assertFalse(andFilter.accept(packet));

        andFilter = new AndFilter(falseFilter, falseFilter);
        assertFalse(andFilter.accept(packet));

        andFilter = new AndFilter();
        andFilter.addFilter(trueFilter);
        andFilter.addFilter(trueFilter);
        andFilter.addFilter(falseFilter);
        andFilter.addFilter(trueFilter);
        assertFalse(andFilter.accept(packet));
    }
}
