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
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertNull;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.jingle.element.JingleContent;

import org.junit.Test;

/**
 * Test the JingleContent class.
 */
public class JingleContentTest extends SmackTestSuite {

    @Test(expected = NullPointerException.class)
    public void emptyBuilderThrowsTest() {
        JingleContent.Builder builder = JingleContent.getBuilder();
        builder.build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void onlyCreatorBuilderThrowsTest() {
        JingleContent.Builder builder = JingleContent.getBuilder();
        builder.setCreator(JingleContent.Creator.initiator);
        builder.build();
    }

    @Test
    public void parserTest() throws Exception {

        JingleContent.Builder builder = JingleContent.getBuilder();

        builder.setCreator(JingleContent.Creator.initiator);
        builder.setName("A name");

        JingleContent content = builder.build();
        assertNotNull(content);
        assertNull(content.getDescription());
        assertEquals(JingleContent.Creator.initiator, content.getCreator());
        assertEquals("A name", content.getName());

        builder.setSenders(JingleContent.Senders.both);
        content = builder.build();
        assertEquals(JingleContent.Senders.both, content.getSenders());

        builder.setDisposition("session");
        JingleContent content1 = builder.build();
        assertEquals("session", content1.getDisposition());
        assertNotSame(content.toXML().toString(), content1.toXML().toString());
        assertEquals(content1.toXML().toString(), builder.build().toXML().toString());

        String xml =
                "<content creator='initiator' disposition='session' name='A name' senders='both'>" +
                "</content>";
        assertEquals(xml, content1.toXML().toString());
    }
}
