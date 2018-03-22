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

import java.io.IOException;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.xhtmlim.XHTMLText;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class BoBExtensionProvider extends ExtensionElementProvider<BoBExtension> {

    @Override
    public BoBExtension parse(XmlPullParser parser, int initialDepth) throws IOException, XmlPullParserException {
        String alt = null;
        String src = null;
        String paragraph = null;

        int eventType;
        String name;
        while (true) {
            eventType = parser.getEventType();
            name = parser.getName();

            if (eventType == XmlPullParser.START_TAG) {
                if (name.equals(XHTMLText.P)) {
                    paragraph = parser.getText();
                }
                else if (name.equals(XHTMLText.IMG)) {
                    alt = parser.getAttributeValue(null, BoBExtension.ALT);
                    src = parser.getAttributeValue(null, BoBExtension.SRC);
                }
            }
            else if (eventType == XmlPullParser.END_TAG && parser.getDepth() == initialDepth) {
                if (src == null || alt == null || paragraph == null) {
                    throw new XmlPullParserException("Bits of Binary element with missing attibutes. "
                            + "Attributes: alt=" + alt + " src=" + src + " paragraph=" + paragraph);
                }
                return new BoBExtension(BoBHash.fromSrc(src), alt, paragraph);
            }
            parser.next();
        }
    }

}
