/**
 *
 * Copyright 2017-2019 Florian Schmaus
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

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.Pair;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.bob.BoBData;
import org.jivesoftware.smackx.bob.ContentId;
import org.jivesoftware.smackx.bob.element.BoBIQ;

/**
 * Bits of Binary IQ provider class.
 *
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0231.html">XEP-0231: Bits of
 *      Binary</a>
 */
public class BoBIQProvider extends IqProvider<BoBIQ> {

    @Override
    public BoBIQ parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        Pair<ContentId, BoBData> parserResult = BoBProviderUtil.parseContentIdAndBobData(parser, initialDepth,
                        xmlEnvironment);

        return new BoBIQ(parserResult.getFirst(), parserResult.getSecond());
    }

}
