/**
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smackx.bob.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Pair;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.bob.BoBData;
import org.jivesoftware.smackx.bob.ContentId;

public class BoBProviderUtil {

    public static Pair<ContentId, BoBData> parseContentIdAndBobData(XmlPullParser parser, int initialDepth,
                    XmlEnvironment xmlEnvironment) throws IOException, XmlPullParserException {
        String cid = parser.getAttributeValue("", "cid");
        ContentId contentId = ContentId.fromCid(cid);

        String dataType = parser.getAttributeValue("", "type");
        Integer maxAge = ParserUtils.getIntegerAttribute(parser, "max-age");

        String base64EncodedData = parser.nextText();

        BoBData bobData = null;
        if (dataType != null) {
            bobData = new BoBData(dataType, base64EncodedData, maxAge);
        }

        return Pair.create(contentId, bobData);
    }
}
