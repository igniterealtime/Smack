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
package org.jivesoftware.smack.tbr.provider;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.tbr.element.TBRTokensIQ;
import org.xmlpull.v1.XmlPullParser;

/**
 * TBR tokens provider class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://www.xmpp.org/extensions/inbox/token-reconnection.html">
 *      XEP-xxxx: Token-based reconnection</a>
 */
public class TBRTokensProvider extends IQProvider<TBRTokensIQ> {

    @Override
    public TBRTokensIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        String accessToken = null;
        String refreshToken = null;

        outerloop: while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("access_token")) {
                    accessToken = parser.nextText();
                }

                if (parser.getName().equals("refresh_token")) {
                    refreshToken = parser.nextText();
                }

            } else if (eventType == XmlPullParser.END_TAG) {

                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }

            }
        }

        return new TBRTokensIQ(accessToken, refreshToken);
    }

}
