/**
 *
 * Copyright 2017 Florian Schmaus
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

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;

import org.jivesoftware.smackx.bob.BoBData;
import org.jivesoftware.smackx.bob.BoBHash;
import org.jivesoftware.smackx.bob.element.BoBIQ;

import org.xmlpull.v1.XmlPullParser;

/**
 * Bits of Binary IQ provider class.
 * 
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBIQProvider extends IQProvider<BoBIQ> {

    @Override
    public BoBIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        String cid = parser.getAttributeValue("", "cid");
        BoBHash bobHash = BoBHash.fromCid(cid);

        String dataType = parser.getAttributeValue("", "type");
        int maxAge = ParserUtils.getIntegerAttribute(parser, "max-age", -1);

        String base64EncodedData = parser.nextText();

        BoBData bobData;
        if (dataType != null) {
            bobData = new BoBData(dataType, base64EncodedData, maxAge);
        } else {
            bobData = null;
        }

        return new BoBIQ(bobHash, bobData);
    }

}
