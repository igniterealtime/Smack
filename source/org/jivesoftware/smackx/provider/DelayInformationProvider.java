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

import java.util.Date;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.xmlpull.v1.XmlPullParser;

/**
 * The DelayInformationProvider parses DelayInformation packets.
 * 
 * @author Gaston Dombiak
 */
public class DelayInformationProvider implements PacketExtensionProvider {

    /**
     * Creates a new DeliveryInformationProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public DelayInformationProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        DelayInformation.UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        Date stamp = DelayInformation.UTC_FORMAT.parse(parser.getAttributeValue("", "stamp"));
        DelayInformation delayInformation = new DelayInformation(stamp);
        delayInformation.setFrom(parser.getAttributeValue("", "from"));
        delayInformation.setReason(parser.nextText());
        return delayInformation;
    }

}
