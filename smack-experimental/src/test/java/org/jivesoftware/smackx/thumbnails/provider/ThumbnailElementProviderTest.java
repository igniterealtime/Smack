/**
 *
 * Copyright 2023 Paul Schaub
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
package org.jivesoftware.smackx.thumbnails.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.thumbnails.element.ThumbnailElement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class ThumbnailElementProviderTest {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParseFull(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<thumbnail xmlns='urn:xmpp:thumbs:1'\n" +
                "uri='cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org'\n" +
                "media-type='image/png'\n" +
                "width='128'\n" +
                "height='96'/>";

        ThumbnailElement element = SmackTestUtil.parse(xml, ThumbnailElementProvider.class, parserKind);

        assertEquals("cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org", element.getUri());
        assertEquals("image/png", element.getMediaType());
        assertEquals(128, element.getWidth());
        assertEquals(96, element.getHeight());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void testParseMinimal(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        String xml = "<thumbnail xmlns='urn:xmpp:thumbs:1'\n" +
                "uri='cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org'/>";

        ThumbnailElement element = SmackTestUtil.parse(xml, ThumbnailElementProvider.class, parserKind);

        assertEquals("cid:sha1+ffd7c8d28e9c5e82afea41f97108c6b4@bob.xmpp.org", element.getUri());
        assertNull(element.getMediaType());
        assertNull(element.getWidth());
        assertNull(element.getHeight());
    }
}
