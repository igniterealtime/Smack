/**
 *
 * Copyright 2016 Fernando Ramirez
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

/**
 * Bits of Binary extension provider class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBExtensionProvider extends ExtensionElementProvider<BoBExtension> {

    @Override
    public BoBExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        BoBHash bobHash = null;
        String alt = null;

        outerloop: while (true) {
            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:
                if (parser.getName().equals(XHTMLText.IMG)) {
                    alt = parser.getAttributeValue("", "alt");

                    String src = parser.getAttributeValue("", "src");
                    bobHash = BoBHash.fromSrc(src);
                }
                break;

            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;

            }
        }

        return new BoBExtension(bobHash, alt, null);
    }

}
