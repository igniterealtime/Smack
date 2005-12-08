/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.xmlpull.v1.XmlPullParser;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * The DelayInformationProvider parses DelayInformation packets.
 *
 * @author Gaston Dombiak
 */
public class DelayInformationProvider implements PacketExtensionProvider {

    /**
     * Creates a new DeliveryInformationProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument
     * constructor
     */
    public DelayInformationProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        Date stamp = null;
        try {
            synchronized (DelayInformation.UTC_FORMAT) {
                stamp = DelayInformation.UTC_FORMAT.parse(parser.getAttributeValue("", "stamp"));
            }
        } catch (ParseException e) {
            // Try again but assuming that the date follows JEP-82 format
            // (Jabber Date and Time Profiles) 
            try {
                synchronized (DelayInformation.NEW_UTC_FORMAT) {
                    stamp = DelayInformation.NEW_UTC_FORMAT
                            .parse(parser.getAttributeValue("", "stamp"));
                }
            } catch (ParseException e1) {
                // Last attempt. Try parsing the date assuming that it does not include milliseconds
                SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                stamp = formatter.parse(parser.getAttributeValue("", "stamp"));
            }
        }
        DelayInformation delayInformation = new DelayInformation(stamp);
        delayInformation.setFrom(parser.getAttributeValue("", "from"));
        delayInformation.setReason(parser.nextText());
        return delayInformation;
    }

}
