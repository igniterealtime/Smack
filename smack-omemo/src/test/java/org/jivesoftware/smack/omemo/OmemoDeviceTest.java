/**
 *
 * Copyright the original author or authors
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Test the OmemoDevice class.
 *
 * @author Paul Schaub
 */
public class OmemoDeviceTest {

    /**
     * Test, if the equals() method works as intended.
     */
    @Test
    public void testEquals() {
        BareJid romeo, juliet, guyUnderTheBalcony;
        try {
            romeo = JidCreate.bareFrom("romeo@shakespeare.lit");
            guyUnderTheBalcony = JidCreate.bareFrom("romeo@shakespeare.lit/underTheBalcony");
            juliet = JidCreate.bareFrom("juliet@shakespeare.lit");
        } catch (XmppStringprepException e) {
            Assert.fail(e.getMessage());
            return;
        }

        OmemoDevice r = new OmemoDevice(romeo, 1);
        OmemoDevice g = new OmemoDevice(guyUnderTheBalcony, 1);
        OmemoDevice r2 = new OmemoDevice(romeo, 2);
        OmemoDevice j = new OmemoDevice(juliet, 3);
        OmemoDevice j2 = new OmemoDevice(juliet, 1);

        assertTrue(r.equals(g));
        assertFalse(r.equals(r2));
        assertFalse(j.equals(j2));
        assertFalse(j2.equals(r2));
    }
}
