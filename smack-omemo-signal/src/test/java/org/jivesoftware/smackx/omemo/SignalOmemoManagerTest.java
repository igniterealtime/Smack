/**
 *
 * Copyright 2017 Paul Schaub
 *
 * This file is part of smack-omemo-signal.
 *
 * smack-omemo-signal is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.jivesoftware.smackx.omemo;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.provider.OmemoVAxolotlProvider;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;

import org.junit.Test;

/**
 * Test OmemoManager functionality.
 */
public class SignalOmemoManagerTest extends SmackTestSuite {

    @Test
    public void instantiationTest() {
        SignalOmemoService.acknowledgeLicense();
        SignalOmemoService.setup();

        DummyConnection dummy = new DummyConnection();
        DummyConnection silly = new DummyConnection();
        OmemoManager a = OmemoManager.getInstanceFor(dummy, 123);
        OmemoManager b = OmemoManager.getInstanceFor(dummy, 234);
        OmemoManager c = OmemoManager.getInstanceFor(silly, 123);
        OmemoManager d = OmemoManager.getInstanceFor(dummy, 123);

        assertNotNull(a);
        assertNotNull(b);
        assertNotNull(c);
        assertNotNull(d);

        assertEquals(Integer.valueOf(123), a.getDeviceId());
        assertEquals(Integer.valueOf(234), b.getDeviceId());

        assertFalse(a == b);
        assertFalse(a == c);
        assertFalse(b == c);
        assertTrue(a == d);

    }

    @Test
    public void randomDeviceIdTest() {
        int a = OmemoManager.randomDeviceId();
        int b = OmemoManager.randomDeviceId();

        assertNotSame(a, b); // This is highly unlikely

        assertTrue(a > 0);
        assertTrue(b > 0);
    }

    @Test
    public void stanzaRecognitionTest() throws Exception {
        String omemoXML = "<encrypted xmlns='eu.siacs.conversations.axolotl'><header sid='1009'><key rid='1337'>MwohBfRqBm2atj3fT0/KUDg59Cnvfpgoe/PLNIu1xgSXujEZEAAYACIwKh6TTC7VBQZcCcKnQlO+6s1GQ9DIRKH4JU7XrJ+JJnkPUwJ4VLSeOEQD7HmFbhQPTLZO0u/qlng=</key><iv>sN0amy4e2NBrlb4G/OjNIQ==</iv></header><payload>4xVUAeg4M0Mhk+5n3YG1x12Dw/cYTc0Z</payload></encrypted>";
        OmemoElement omemoElement = new OmemoVAxolotlProvider().parse(TestUtils.getParser(omemoXML));

        Message m = StanzaBuilder.buildMessage().addExtension(omemoElement).build();
        Message n = StanzaBuilder.buildMessage().build();

        assertTrue(OmemoManager.stanzaContainsOmemoElement(m));
        assertFalse(OmemoManager.stanzaContainsOmemoElement(n));
    }
}
