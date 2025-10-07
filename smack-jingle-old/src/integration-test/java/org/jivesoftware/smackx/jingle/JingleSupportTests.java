/*
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

package org.jivesoftware.smackx.jingle;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite that runs all the Jingle support tests
 *
 * @author Alvaro Saurin
 */
public class JingleSupportTests {

    public static Test suite() {
        TestSuite suite = new TestSuite("High and low level API tests for Jingle support");

        // $JUnit-BEGIN$
        suite.addTest(new TestSuite(JingleManagerTest.class));
        // $JUnit-END$

        return suite;
    }
}
