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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.eme.element.ExplicitMessageEncryptionElement;

import org.junit.jupiter.api.Test;

public class ExplicitMessageEncryptionElementTest extends SmackTestSuite {

    @Test
    public void addToMessageTest() {
        Message message = StanzaBuilder.buildMessage().build();

        // Check inital state (no elements)
        assertNull(ExplicitMessageEncryptionElement.from(message));
        assertFalse(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl));

        List<ExtensionElement> extensions = message.getExtensions();
        assertEquals(0, extensions.size());

        MessageBuilder messageBuilder = StanzaBuilder.buildMessage();
        // Add OMEMO
        ExplicitMessageEncryptionElement.set(messageBuilder,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl);

        message = messageBuilder.build();
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

        ExplicitMessageEncryptionElement.set(messageBuilder,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0);

        message = messageBuilder.build();
        extensions = message.getExtensions();
        assertEquals(2, extensions.size());
        assertTrue(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.openpgpV0));
        assertTrue(ExplicitMessageEncryptionElement.hasProtocol(message,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl));

        // Check, if adding additional OMEMO wont add another element
        ExplicitMessageEncryptionElement.set(messageBuilder,
                ExplicitMessageEncryptionElement.ExplicitMessageEncryptionProtocol.omemoVAxolotl);

        message = messageBuilder.build();
        extensions = message.getExtensions();
        assertEquals(2, extensions.size());
    }
}
