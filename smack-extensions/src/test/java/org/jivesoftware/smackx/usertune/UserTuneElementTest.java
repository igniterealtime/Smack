/**
 *
 * Copyright 2019 Aditya Borikar.
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
package org.jivesoftware.smackx.usertune;

import static org.jivesoftware.smack.test.util.XmlUnitUtils.assertXmlSimilar;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.test.util.SmackTestUtil.XmlPullParserKind;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.usertune.element.UserTuneElement;
import org.jivesoftware.smackx.usertune.provider.UserTuneProvider;

import org.junit.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

public class UserTuneElementTest extends SmackTestSuite {

    private final String xml = "<tune xmlns='http://jabber.org/protocol/tune'>" +
            "<artist>Yes</artist>" +
            "<length>686</length>" +
            "<rating>8</rating>" +
            "<source>Yessongs</source>" +
            "<title>Heart of the Sunrise</title>" +
            "<track>3</track>" +
            "<uri>http://www.yesworld.com/lyrics/Fragile.html#9</uri>" +
            "</tune>";

    @Test
    public void toXmlTest() throws IOException, XmlPullParserException, SmackParsingException, URISyntaxException {

        URI uri = new URI("http://www.yesworld.com/lyrics/Fragile.html#9");

        UserTuneElement.Builder builder = UserTuneElement.getBuilder();
        UserTuneElement userTuneElement = builder.setArtist("Yes")
                .setLength(686)
                .setRating(8)
                .setSource("Yessongs")
                .setTitle("Heart of the Sunrise")
                .setTrack("3")
                .setUri(uri)
                .build();
        assertXmlSimilar(xml, userTuneElement.toXML().toString());
    }

    @ParameterizedTest
    @EnumSource(value = SmackTestUtil.XmlPullParserKind.class)
    public void userTuneElementProviderTest(XmlPullParserKind parserKind) throws XmlPullParserException, IOException, SmackParsingException {
        XmlPullParser parser = SmackTestUtil.getParserFor(xml, parserKind);
        UserTuneElement parsed = UserTuneProvider.INSTANCE.parse(parser);
        assertXmlSimilar(xml, parsed.toXML().toString());
    }
}
