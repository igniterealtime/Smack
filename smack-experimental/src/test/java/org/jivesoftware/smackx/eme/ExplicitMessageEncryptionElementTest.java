/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.eme;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;

import org.junit.Test;

public class ExplicitMessageEncryptionElementTest extends SmackTestSuite {

    @Test
    public void addToMessageTest() {
        Message message = new Message();

        // Check inital state (no elements)
        assertNull(ExplicitMessageEncryptionElement.from(message));
        assertFalse(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl));

        List<ExtensionElement> extensions = message.getExtensions();
        assertEquals(0, extensions.size());

        // Add OMEMO
        ExplicitMessageEncryptionElement.set(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl);
        extensions = message.getExtensions();
        assertEquals(1, extensions.size());
        assertTrue(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl));
        assertTrue(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl.getNamespace()));
        assertFalse(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        assertFalse(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0.getNamespace()));

        ExplicitMessageEncryptionElement.set(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0);
        extensions = message.getExtensions();
        assertEquals(2, extensions.size());
        assertTrue(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        assertTrue(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl));

        // Check, if adding additional OMEMO wont add another element
        ExplicitMessageEncryptionElement.set(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl);

        extensions = message.getExtensions();
        assertEquals(2, extensions.size());
    }
}
