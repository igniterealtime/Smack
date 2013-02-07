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

package org.jivesoftware.smack.util;

import junit.framework.TestCase;

/**
 * A test case for the Cache class.
 */
public class CacheTest extends TestCase {

    public void testMaxSize() {
        Cache<Integer, String> cache = new Cache<Integer, String>(100, -1);
        for (int i=0; i < 1000; i++) {
            cache.put(i, "value");
            assertTrue("Cache size must never be larger than 100.", cache.size() <= 100);
        }
    }

    public void testLRU() {
        Cache<Integer, String> cache = new Cache<Integer, String>(100, -1);
        for (int i=0; i < 1000; i++) {
            cache.put(i, "value");
            assertTrue("LRU algorithm for cache key of '0' failed.",
                    cache.get(new Integer(0)) != null);
        }
    }
}
