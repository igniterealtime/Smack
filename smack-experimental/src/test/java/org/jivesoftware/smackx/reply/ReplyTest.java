/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reply;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.reply.element.ReplyElement;
import org.jivesoftware.smackx.reply.provider.ReplyElementProvider;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ReplyTest {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void serializationTest() {

        String replyTo = "anna@example.com";
        String replyId = "message-id1";
        ReplyElement element = new ReplyElement(replyTo, replyId);
        assertXmlSimilar("<reply xmlns=\"urn:xmpp:reply:0\" to=\"anna@example.com\" id=\"message-id1\" />", element.toXML());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void deserializationTest() throws Exception {

        String xml = "<reply xmlns=\"urn:xmpp:reply:0\" to=\"anna@example.com\" id=\"message-id1\" />";

        XmlPullParser parser = TestUtils.getParser(xml);

        ReplyElementProvider provider = new ReplyElementProvider();

        ReplyElement element = provider.parse(parser, 1, null);

        assertEquals("anna@example.com", element.getReplyTo());
        assertEquals("message-id1", element.getReplyId());
    }

}
