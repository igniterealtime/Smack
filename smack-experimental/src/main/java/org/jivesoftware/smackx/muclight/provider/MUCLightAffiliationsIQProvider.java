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
package org.jivesoftware.smackx.muclight.provider;

import java.io.IOException;
import java.util.HashMap;

import org.jivesoftware.smack.provider.IQProvider;

import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.element.MUCLightAffiliationsIQ;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * MUC Light affiliations IQ provider class.
 *
 * @author Fernando Ramirez
 *
 */
public class MUCLightAffiliationsIQProvider extends IQProvider<MUCLightAffiliationsIQ> {

    @Override
    public MUCLightAffiliationsIQ parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        String version = null;
        HashMap<Jid, MUCLightAffiliation> occupants = new HashMap<>();

        outerloop: while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("version")) {
                    version = parser.nextText();
                }

                if (parser.getName().equals("user")) {
                    MUCLightAffiliation affiliation = MUCLightAffiliation
                            .fromString(parser.getAttributeValue("", "affiliation"));
                    occupants.put(JidCreate.from(parser.nextText()), affiliation);
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return new MUCLightAffiliationsIQ(version, occupants);
    }

}
