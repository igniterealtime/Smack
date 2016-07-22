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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;

/**
 * MAM Preferences IQ Provider class.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez
 * 
 */
public class MamPrefsIQProvider extends IQProvider<MamPrefsIQ> {

    @Override
    public MamPrefsIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        String iqType = parser.getAttributeValue("", "type");
        String defaultField = parser.getAttributeValue("", "default");

        if (iqType == null) {
            iqType = "result";
        }

        List<Jid> alwaysJids = null;
        List<Jid> neverJids = null;

        outerloop: while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("always")) {
                    alwaysJids = iterateJids(parser);
                }
                if (parser.getName().equals("never")) {
                    neverJids = iterateJids(parser);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return new MamPrefsIQ(Type.fromString(iqType), alwaysJids, neverJids, defaultField);
    }

    private List<Jid> iterateJids(XmlPullParser parser) throws Exception {
        List<Jid> jids = new ArrayList<>();

        int initialDepth = parser.getDepth();

        outerloop: while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("jid")) {
                    parser.next();
                    jids.add(JidCreate.from(parser.getText()));
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return jids;
    }

}
