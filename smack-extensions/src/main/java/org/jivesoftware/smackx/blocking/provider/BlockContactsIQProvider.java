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
package org.jivesoftware.smackx.blocking.provider;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.blocking.element.BlockContactsIQ;
import org.jxmpp.jid.Jid;
import org.xmlpull.v1.XmlPullParser;

/**
 * Block contact IQ provider class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0191.html">XEP-0191: Blocking
 *      Command</a>
 */
public class BlockContactsIQProvider extends IQProvider<BlockContactsIQ> {

    @Override
    public BlockContactsIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        List<Jid> jids = new ArrayList<>();

        outerloop: while (true) {
            int eventType = parser.next();

            switch (eventType) {

            case XmlPullParser.START_TAG:
                if (parser.getName().equals("item")) {
                    Jid jid = ParserUtils.getJidAttribute(parser);
                    jids.add(jid);
                }
                break;

            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;

            }
        }

        return new BlockContactsIQ(jids);
    }

}
