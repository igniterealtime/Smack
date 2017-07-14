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

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;

import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.MultipleCryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Test Omemo related Exceptions.
 */
public class OmemoExceptionsTest {

    @Test
    public void undecidedOmemoIdentityExceptionTest() throws XmppStringprepException {
        OmemoDevice alice = new OmemoDevice(JidCreate.bareFrom("alice@server.tld"), 1234);
        OmemoDevice bob = new OmemoDevice(JidCreate.bareFrom("bob@server.tld"), 5678);
        OmemoDevice mallory = new OmemoDevice(JidCreate.bareFrom("mallory@server.tld"), 9876);

        UndecidedOmemoIdentityException u = new UndecidedOmemoIdentityException(alice);
        assertTrue(u.getUndecidedDevices().contains(alice));
        assertTrue(u.getUndecidedDevices().size() == 1);

        UndecidedOmemoIdentityException v = new UndecidedOmemoIdentityException(bob);
        v.getUndecidedDevices().add(mallory);
        assertTrue(v.getUndecidedDevices().size() == 2);
        assertTrue(v.getUndecidedDevices().contains(bob));
        assertTrue(v.getUndecidedDevices().contains(mallory));

        u.getUndecidedDevices().add(bob);
        u.join(v);
        assertTrue(u.getUndecidedDevices().size() == 3);
    }

    @Test
    public void cannotEstablishOmemoSessionExceptionTest() throws XmppStringprepException {
        OmemoDevice alice1 = new OmemoDevice(JidCreate.bareFrom("alice@server.tld"), 1234);
        OmemoDevice alice2 = new OmemoDevice(JidCreate.bareFrom("alice@server.tld"), 2345);
        OmemoDevice bob = new OmemoDevice(JidCreate.bareFrom("bob@server.tld"), 5678);

        CannotEstablishOmemoSessionException c = new CannotEstablishOmemoSessionException(alice1, null);
        assertEquals(1, c.getFailures().size());
        assertTrue(c.getFailures().containsKey(alice1.getJid()));

        c.addSuccess(alice2);
        assertFalse(c.requiresThrowing());

        c.addFailures(new CannotEstablishOmemoSessionException(bob, null));
        assertTrue(c.requiresThrowing());
        assertEquals(1, c.getSuccesses().size());
        assertEquals(2, c.getFailures().size());

        c.getSuccesses().remove(alice2.getJid());
        c.addFailures(new CannotEstablishOmemoSessionException(alice2, null));
        assertEquals(2, c.getFailures().size());
    }

    @Test
    public void multipleCryptoFailedExceptionTest() {
        CryptoFailedException e1 = new CryptoFailedException("Fail");
        CryptoFailedException e2 = new CryptoFailedException("EpicFail");
        ArrayList<CryptoFailedException> l = new ArrayList<>();
        l.add(e1); l.add(e2);
        MultipleCryptoFailedException m = MultipleCryptoFailedException.from(l);

        assertEquals(2, m.getCryptoFailedExceptions().size());
        assertTrue(m.getCryptoFailedExceptions().contains(e1));
        assertTrue(m.getCryptoFailedExceptions().contains(e2));

        ArrayList<CryptoFailedException> el = new ArrayList<>();
        try {
            MultipleCryptoFailedException m2 = MultipleCryptoFailedException.from(el);
            fail("MultipleCryptoFailedException must not allow empty list.");
        } catch (IllegalArgumentException e) {
            // Expected
        }
    }
}
