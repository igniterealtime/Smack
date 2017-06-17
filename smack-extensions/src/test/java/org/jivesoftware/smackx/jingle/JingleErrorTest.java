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

import org.jivesoftware.smackx.jingle.element.JingleError;

import org.junit.Test;

/**
 * Test the JingleError class.
 */
public class JingleErrorTest extends SmackTestSuite {

    @Test
    public void parserTest() {
        assertEquals("<out-of-order xmlns='urn:xmpp:jingle:errors:1'/>",
                JingleError.fromString("out-of-order").toXML().toString());
        assertEquals("<tie-break xmlns='urn:xmpp:jingle:errors:1'/>",
                JingleError.fromString("tie-break").toXML().toString());
        assertEquals("<unknown-session xmlns='urn:xmpp:jingle:errors:1'/>",
                JingleError.fromString("unknown-session").toXML().toString());
        assertEquals("<unsupported-info xmlns='urn:xmpp:jingle:errors:1'/>",
                  JingleError.fromString("unsupported-info").toXML().toString());
        assertEquals("unknown-session", JingleError.fromString("unknown-session").getMessage());
    }

    @Test(expected = IllegalArgumentException.class)
    public void illegalArgumentTest() {
        JingleError.fromString("inexistent-error");
    }
}
