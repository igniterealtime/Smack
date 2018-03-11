/**
 *
 * Copyright 2018
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

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.xhtmlim.XHTMLText;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

public class BoBExtensionProvider extends ExtensionElementProvider<BoBExtension> {

    @Override
    public BoBExtension parse(XmlPullParser parser, int initialDepth) throws IOException, XmlPullParserException {
        String src = parser.getAttributeValue("", BoBExtension.SRC);
        String alt = parser.getAttributeValue("", BoBExtension.ALT);

        String paragraph = null;

        outerloop: while (true) {
            switch (parser.next()) {
                case XmlPullParser.START_TAG:
                    if (parser.getName().equals(XHTMLText.P)) {
                        paragraph = parser.nextText();
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
            }
        }

        if (src == null || alt == null || paragraph == null) {
            throw new XmlPullParserException("Bits of Binary element with missing attibutes. Attributes: alt="
                    + alt + " src=" + src + " paragraph=" + paragraph);
        }

        BoBHash bobHash = BoBHash.fromSrc(src);

        return new BoBExtension(bobHash, alt, paragraph);
    }

}
