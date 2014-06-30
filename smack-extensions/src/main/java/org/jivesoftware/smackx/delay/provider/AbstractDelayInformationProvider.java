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

import java.util.Date;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.xmlpull.v1.XmlPullParser;

public abstract class AbstractDelayInformationProvider implements PacketExtensionProvider {

    @Override
    public final PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        String stampString = (parser.getAttributeValue("", "stamp"));
        String from = parser.getAttributeValue("", "from");
        String reason = null;
        if (!parser.isEmptyElementTag()) {
            parser.next();
            assert(parser.getEventType() == XmlPullParser.TEXT);
            reason = parser.getText();
        }
        parser.next();
        assert(parser.getEventType() == XmlPullParser.END_TAG);
        Date stamp = parseDate(stampString);
        return new DelayInformation(stamp, from, reason);
    }

    protected abstract Date parseDate(String string) throws Exception;
}
