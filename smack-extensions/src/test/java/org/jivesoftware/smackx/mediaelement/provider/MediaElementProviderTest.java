/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.mediaelement.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.List;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.mediaelement.element.MediaElement;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class MediaElementProviderTest {

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void simpleMediaElementTest(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        final String xml =
                        "<media xmlns='urn:xmpp:media-element' height='80' width='290'>" +
                          "<uri type='audio/x-wav'>" +
                            "http://victim.example.com/challenges/speech.wav?F3A6292C" +
                          "</uri>" +
                        "</media>";

        MediaElement mediaElement = SmackTestUtil.parse(xml, MediaElementProvider.class, parserKind);
        assertEquals(80, mediaElement.getHeight().intValue());
        assertEquals(290, mediaElement.getWidth().intValue());

        List<MediaElement.Uri> uris = mediaElement.getUris();
        assertEquals(1, uris.size());

        MediaElement.Uri uri = uris.get(0);
        assertEquals("audio/x-wav", uri.getType());
        assertEquals("http://victim.example.com/challenges/speech.wav?F3A6292C", uri.getUri().toString());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void parseMediaElementTest(SmackTestUtil.XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        final String xml =
                        "<media xmlns='urn:xmpp:media-element'>" +
                          "<uri type='audio/x-wav'>" +
                            "http://victim.example.com/challenges/speech.wav?F3A6292C" +
                          "</uri>" +
                          "<uri type='audio/ogg; codecs=speex'>" +
                             "cid:sha1+a15a505e360702b79c75a5f67773072ed392f52a@bob.xmpp.org" +
                          "</uri>" +
                          "<uri type='audio/mpeg'>" +
                            "http://victim.example.com/challenges/speech.mp3?F3A6292C" +
                          "</uri>" +
                        "</media>";

        MediaElement mediaElement = SmackTestUtil.parse(xml, MediaElementProvider.class, parserKind);

        List<MediaElement.Uri> uris = mediaElement.getUris();
        assertEquals(3, uris.size());
    }
}
