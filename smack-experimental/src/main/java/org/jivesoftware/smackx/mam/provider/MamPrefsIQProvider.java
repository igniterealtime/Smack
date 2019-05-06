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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.mam.element.MamPrefsIQ;
import org.jivesoftware.smackx.mam.element.MamPrefsIQ.DefaultBehavior;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;

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
    public MamPrefsIQ parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        String iqType = parser.getAttributeValue("", "type");
        String defaultBehaviorString = parser.getAttributeValue("", "default");
        DefaultBehavior defaultBehavior = null;
        if (defaultBehaviorString != null) {
            defaultBehavior = DefaultBehavior.valueOf(defaultBehaviorString);
        }

        if (iqType == null) {
            iqType = "result";
        }

        List<Jid> alwaysJids = null;
        List<Jid> neverJids = null;

        outerloop: while (true) {
            final XmlPullParser.Event eventType = parser.next();
            final String name = parser.getName();
            switch (eventType) {
            case START_ELEMENT:
                switch (name) {
                case "always":
                    alwaysJids = iterateJids(parser);
                    break;
                case "never":
                    neverJids = iterateJids(parser);
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return new MamPrefsIQ(alwaysJids, neverJids, defaultBehavior);
    }

    private static List<Jid> iterateJids(XmlPullParser parser) throws XmlPullParserException, IOException {
        List<Jid> jids = new ArrayList<>();

        int initialDepth = parser.getDepth();

        outerloop: while (true) {
            final XmlPullParser.Event eventType = parser.next();
            final String name = parser.getName();
            switch (eventType) {
            case START_ELEMENT:
                switch (name) {
                case "jid":
                    parser.next();
                    jids.add(JidCreate.from(parser.getText()));
                    break;
                }
                break;
            case  END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return jids;
    }

}
