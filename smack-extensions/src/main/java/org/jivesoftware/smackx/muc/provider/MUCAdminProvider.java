/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.muc.provider;

import java.io.IOException;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.muc.packet.MUCAdmin;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * The MUCAdminProvider parses MUCAdmin packets. (@see MUCAdmin)
 * 
 * @author Gaston Dombiak
 */
public class MUCAdminProvider extends IQProvider<MUCAdmin> {

    @Override
    public MUCAdmin parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException {
        MUCAdmin mucAdmin = new MUCAdmin();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    mucAdmin.addItem(MUCParserUtils.parseItem(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }

        return mucAdmin;
    }
}
