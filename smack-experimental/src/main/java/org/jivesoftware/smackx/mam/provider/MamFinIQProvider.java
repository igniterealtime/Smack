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
package org.jivesoftware.smackx.mam.provider;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.mam.element.MamFinIQ;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.rsm.provider.RSMSetProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * MAM Fin IQ Provider class.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez
 *
 */
public class MamFinIQProvider extends IQProvider<MamFinIQ> {

    @Override
    public MamFinIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        String queryId = parser.getAttributeValue("", "queryid");
        boolean complete = Boolean.parseBoolean(parser.getAttributeValue("", "complete"));
        boolean stable = Boolean.parseBoolean(parser.getAttributeValue("", "stable"));
        RSMSet rsmSet = null;

        outerloop: while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals(RSMSet.ELEMENT)) {
                    rsmSet = new RSMSetProvider().parse(parser);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return new MamFinIQ(queryId, rsmSet, complete, stable);
    }

}
