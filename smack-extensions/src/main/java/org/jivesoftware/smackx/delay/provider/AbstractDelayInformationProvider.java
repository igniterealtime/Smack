/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.delay.provider;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class AbstractDelayInformationProvider extends ExtensionElementProvider<DelayInformation> {

    @Override
    public final DelayInformation parse(XmlPullParser parser,
                    int initialDepth) throws XmlPullParserException,
                    IOException, SmackException {
        String stampString = (parser.getAttributeValue("", "stamp"));
        String from = parser.getAttributeValue("", "from");
        String reason = null;
        if (!parser.isEmptyElementTag()) {
            int event = parser.next();
            switch (event) {
            case XmlPullParser.TEXT:
                reason = parser.getText();
                parser.next();
                break;
            case XmlPullParser.END_TAG:
                reason = "";
                break;
            default:
                throw new IllegalStateException("Unexpected event: " + event);
            }
        } else {
            parser.next();
        }
        Date stamp;
        try {
            stamp = parseDate(stampString);
        } catch (ParseException e) {
            throw new SmackException(e);
        }
        return new DelayInformation(stamp, from, reason);
    }

    protected abstract Date parseDate(String string) throws ParseException;
}
