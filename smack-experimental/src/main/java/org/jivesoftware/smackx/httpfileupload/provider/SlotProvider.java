/**
 *
 * Copyright © 2017 Grigory Fedorov
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
package org.jivesoftware.smackx.httpfileupload.provider;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.httpfileupload.element.Slot;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.net.URL;

/**
 * Provider for Slot.
 *
 * @author Grigory Fedorov
 * @see <a href="http://xmpp.org/extensions/xep-0363.html">XEP-0363: HTTP File Upload</a>
 */
public class SlotProvider extends IQProvider<Slot> {

    @Override
    public Slot parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, SmackException {
        URL putUrl = null;
        URL getUrl = null;

        outerloop: while (true) {
            int event = parser.next();

            switch (event) {
                case XmlPullParser.START_TAG:
                    String name = parser.getName();
                    switch(name) {
                        case "put":
                            putUrl = new URL(parser.nextText());
                            break;
                        case "get":
                            getUrl = new URL(parser.nextText());
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }

        return new Slot(putUrl, getUrl);
    }
}
