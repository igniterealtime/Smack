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
package org.jivesoftware.smackx.jingle;

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;

import org.jivesoftware.smackx.jingle.element.JingleError;
import org.jivesoftware.smackx.jingle.provider.JingleErrorProvider;

import org.junit.Test;

/**
 * Test the JingleError class.
 */
public class JingleErrorTest extends SmackTestSuite {

    @Test
    public void tieBreakTest() throws Exception {
        String xml = "<tie-break xmlns='urn:xmpp:jingle:errors:1'/>";
        JingleError error = new JingleErrorProvider().parse(TestUtils.getParser(xml));
        assertEquals(xml, error.toXML().toString());
    }

    @Test
    public void unknownSessionTest() throws Exception {
        String xml = "<unknown-session xmlns='urn:xmpp:jingle:errors:1'/>";
        JingleError error = new JingleErrorProvider().parse(TestUtils.getParser(xml));
        assertEquals(xml, error.toXML().toString());
    }

    @Test
    public void unsupportedInfoTest() throws Exception {
        String xml = "<unsupported-info xmlns='urn:xmpp:jingle:errors:1'/>";
        JingleError error = new JingleErrorProvider().parse(TestUtils.getParser(xml));
        assertEquals(xml, error.toXML().toString());
    }

    @Test
    public void outOfOrderTest() throws Exception {
        String xml = "<out-of-order xmlns='urn:xmpp:jingle:errors:1'/>";
        JingleError error = new JingleErrorProvider().parse(TestUtils.getParser(xml));
        assertEquals(xml, error.toXML().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalArgumentTest() {
        JingleError.fromString("inexistent-error");
    }


}
