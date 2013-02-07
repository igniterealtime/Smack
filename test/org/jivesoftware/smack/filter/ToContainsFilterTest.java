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
 * A test case for the ToContainsFilter class.
 */
public class ToContainsFilterTest extends TestCase {

    public void testNullArgs() {
        try {
            new ToContainsFilter(null);
            fail("Parameter can not be null");
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void testAccept() {
        MockToPacket packet = new MockToPacket("foo@bar.com");

        ToContainsFilter toContainsFilter = new ToContainsFilter("");
        assertTrue(toContainsFilter.accept(packet));

        toContainsFilter = new ToContainsFilter("foo");
        assertTrue(toContainsFilter.accept(packet));

        toContainsFilter = new ToContainsFilter("foo@bar.com");
        assertTrue(toContainsFilter.accept(packet));

        toContainsFilter = new ToContainsFilter("bar@foo.com");
        assertFalse(toContainsFilter.accept(packet));

        toContainsFilter = new ToContainsFilter("blah-stuff,net");
        assertFalse(toContainsFilter.accept(packet));
    }

    /**
     * Wraps the MockPacket class to always give an expected To field.
     */
    private class MockToPacket extends MockPacket {
        private String to;
        public MockToPacket(String to) {
            this.to = to;
        }
        public String getTo() {
            return to;
        }
    }
}
