/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.jivesoftware.smackx.caps.provider;

import static org.junit.Assert.assertNotNull;

import org.jivesoftware.smack.test.util.TestUtils;

import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.caps.packet.CapsExtension;

import org.junit.Test;

public class CapsExtensionProviderTest extends InitExtensions {

    @Test
    public void parseTest() throws Exception {
        // @formatter:off
        final String capsExtensionString =
            "<c xmlns='http://jabber.org/protocol/caps'"
            + " hash='sha-1'"
            + " node='http://foo.example.org/bar'"
            + " ver='QgayPKawpkPSDYmwt/WM94uA1u0='/>";
        // @formatter:on
        CapsExtension capsExtension = TestUtils.parseExtensionElement(capsExtensionString);
        assertNotNull(capsExtension);
    }
}
