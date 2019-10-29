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
package org.jivesoftware.smackx.usertune.provider;

import java.io.IOException;
import java.net.URI;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.usertune.element.UserTuneElement;

/**
 * This is the Provider Class for {@link UserTuneElement}.
 */
public class UserTuneProvider extends ExtensionElementProvider<UserTuneElement> {

    public static final UserTuneProvider INSTANCE = new UserTuneProvider();

    @Override
    public UserTuneElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {

        UserTuneElement.Builder builder = UserTuneElement.getBuilder();
        XmlPullParser.TagEvent tag = parser.nextTag();
        outerloop: while (true) {
            switch (tag) {
            case START_ELEMENT:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                if (!UserTuneElement.NAMESPACE.equals(namespace)) {
                    continue outerloop;
                }
                while (tag == XmlPullParser.TagEvent.START_ELEMENT) {
                    switch (name) {
                    case "artist":
                        builder.setArtist(parser.nextText());
                        break;
                    case "length":
                        builder.setLength(ParserUtils.getIntegerFromNextText(parser));
                        break;
                    case "rating":
                        builder.setRating(ParserUtils.getIntegerFromNextText(parser));
                        break;
                    case "source":
                        builder.setSource(parser.nextText());
                        break;
                    case "title":
                        builder.setTitle(parser.nextText());
                        break;
                    case "track":
                        builder.setTrack(parser.nextText());
                        break;
                    case "uri":
                        URI uri = ParserUtils.getUriFromNextText(parser);
                        builder.setUri(uri);
                        break;
                    }
                    tag = parser.nextTag();
                    name = parser.getName();
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        return builder.build();
    }
}
