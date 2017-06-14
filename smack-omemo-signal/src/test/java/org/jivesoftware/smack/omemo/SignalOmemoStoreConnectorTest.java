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
package org.jivesoftware.smack.omemo;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

import org.jivesoftware.smackx.omemo.signal.SignalOmemoStoreConnector;

import org.junit.Test;

/**
 * Test some functionality of the SignalOmemoStoreConnector.
 */
public class SignalOmemoStoreConnectorTest {

    @Test
    public void getLocalRegistrationIdTest() {
        SignalOmemoStoreConnector connector = new SignalOmemoStoreConnector(null, null);
        assertEquals("RegistrationId must always be 0.", 0, connector.getLocalRegistrationId());
    }

    @Test
    public void isTrustedIdentityTest() {
        SignalOmemoStoreConnector connector = new SignalOmemoStoreConnector(null, null);
        assertTrue("All identities must be trusted by default.", connector.isTrustedIdentity(null, null));
    }
}
