/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smack.omemo;

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smackx.omemo.util.OmemoKeyUtil;

import org.junit.Test;

/**
 * Test KeyUtil functions.
 *
 * @author Paul Schaub
 */
public class OmemoKeyUtilTest {

    @Test
    public void testAddInBounds() {
        int high = Integer.MAX_VALUE - 2;
        int max = Integer.MAX_VALUE;
        assertEquals(OmemoKeyUtil.addInBounds(high, 3), 1);
        assertEquals(OmemoKeyUtil.addInBounds(1,2), 3);
        assertEquals(OmemoKeyUtil.addInBounds(max, 5), 5);
    }

    @Test
    public void testPrettyFingerprint() {
        String ugly = "FFFFFFFFEEEEEEEEDDDDDDDDCCCCCCCCBBBBBBBBAAAAAAAA9999999988888888";
        String pretty = OmemoKeyUtil.prettyFingerprint(ugly);
        assertEquals(pretty, "FFFFFFFF EEEEEEEE DDDDDDDD CCCCCCCC BBBBBBBB AAAAAAAA 99999999 88888888");
    }
}
