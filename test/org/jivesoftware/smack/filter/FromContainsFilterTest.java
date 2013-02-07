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
 * A test case for the FromContainsFilter class.
 */
public class FromContainsFilterTest extends TestCase {

    public void testNullArgs() {
        try {
            new FromContainsFilter(null);
            fail("Parameter can not be null");
        }
        catch (IllegalArgumentException e) {
            assertTrue(true);
        }
    }

    public void testAccept() {
        MockFromPacket packet = new MockFromPacket("foo@bar.com");

        FromContainsFilter fromContainsFilter = new FromContainsFilter("");
        assertTrue(fromContainsFilter.accept(packet));

        fromContainsFilter = new FromContainsFilter("foo");
        assertTrue(fromContainsFilter.accept(packet));

        fromContainsFilter = new FromContainsFilter("foo@bar.com");
        assertTrue(fromContainsFilter.accept(packet));

        fromContainsFilter = new FromContainsFilter("bar@foo.com");
        assertFalse(fromContainsFilter.accept(packet));

        fromContainsFilter = new FromContainsFilter("blah-stuff,net");
        assertFalse(fromContainsFilter.accept(packet));
    }

    /**
     * Wraps the MockPacket class to always give an expected From field.
     */
    private class MockFromPacket extends MockPacket {
        private String from;
        public MockFromPacket(String from) {
            this.from = from;
        }
        public String getFrom() {
            return from;
        }
    }
}
