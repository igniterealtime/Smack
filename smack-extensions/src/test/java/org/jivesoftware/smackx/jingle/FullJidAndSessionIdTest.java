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
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.jingle.util.FullJidAndSessionId;

import org.junit.Test;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class FullJidAndSessionIdTest extends SmackTestSuite {

    @Test
    public void equalityTest() throws XmppStringprepException {
        FullJid albert = JidCreate.fullFrom("albert@einstein.emc/squared");
        String albertId = "10121922";
        FullJidAndSessionId fns = new FullJidAndSessionId(albert, albertId);

        assertEquals("10121922", fns.getSessionId());
        assertEquals(JidCreate.fullFrom("albert@einstein.emc/squared"), fns.getFullJid());

        FullJidAndSessionId fns2 = new FullJidAndSessionId(JidCreate.fullFrom("albert@einstein.emc/squared"), "10121922");
        assertTrue(fns.equals(fns2));
        assertEquals(fns.hashCode(), fns2.hashCode());

        assertNotSame(fns, new FullJidAndSessionId(JidCreate.fullFrom("albert@einstein.emc/squared"), "11111111"));
    }
}
