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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.JingleContentElement;

import org.junit.jupiter.api.Test;

/**
 * Test the JingleContentElement class.
 */
public class JingleContentTest extends SmackTestSuite {

    @Test
    public void emptyBuilderThrowsTest() {
        JingleContentElement.Builder builder = JingleContentElement.getBuilder();
        assertThrows(IllegalArgumentException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void onlyCreatorBuilderThrowsTest() {
        JingleContentElement.Builder builder = JingleContentElement.getBuilder();
        builder.setCreator(JingleContentElement.Creator.initiator);
        assertThrows(IllegalArgumentException.class, () -> {
            builder.build();
        });
    }

    @Test
    public void parserTest() throws Exception {

        JingleContentElement.Builder builder = JingleContentElement.getBuilder();

        builder.setCreator(JingleContentElement.Creator.initiator);
        builder.setName("A name");

        JingleContentElement content = builder.build();
        assertNotNull(content);
        assertNull(content.getDescription());
        assertEquals(JingleContentElement.Creator.initiator, content.getCreator());
        assertEquals("A name", content.getName());

        builder.setSenders(JingleContentElement.Senders.both);
        content = builder.build();
        assertEquals(JingleContentElement.Senders.both, content.getSenders());

        builder.setDisposition("session");
        JingleContentElement content1 = builder.build();
        assertEquals("session", content1.getDisposition());
        assertNotSame(content.toXML(new XmlEnvironment(null)).toString(), content1.toXML(new XmlEnvironment(null)).toString());
        assertEquals(content1.toXML(new XmlEnvironment(null)).toString(), builder.build().toXML(new XmlEnvironment(null)).toString());

        String xml =
                "<content creator='initiator' disposition='session' name='A name' senders='both'/>";
        assertEquals(xml, content1.toXML().toString());
    }
}
