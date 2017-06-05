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

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.StringUtils;
import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotSame;

/**
 * Test JingleSession class.
 */
public class JingleSessionTest extends SmackTestSuite {

    @Test
    public void sessionTest() throws XmppStringprepException {
        Jid romeo = JidCreate.from("romeo@montague.lit");
        Jid juliet = JidCreate.from("juliet@capulet.lit");
        String sid = StringUtils.randomString(24);

        JingleSession s1 = new JingleSession(romeo, juliet, sid);
        JingleSession s2 = new JingleSession(juliet, romeo, sid);
        JingleSession s3 = new JingleSession(romeo, juliet, StringUtils.randomString(23));
        JingleSession s4 = new JingleSession(juliet, romeo, sid);

        assertNotSame(s1, s2);
        assertNotSame(s1, s3);
        assertNotSame(s2, s3);
        assertEquals(s2, s4);
        assertEquals(s2.hashCode(), s4.hashCode());
    }
}
