/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.xmlpull.v1.XmlPullParser;

/**
 * The DelayInformationProvider parses DelayInformation packets.
 * 
 * @author Gaston Dombiak
 * @author Henning Staib
 */
public class DelayInformationProvider implements PacketExtensionProvider {
    
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        String stampString = (parser.getAttributeValue("", "stamp"));
        Date stamp = null;
        
        try {
            stamp = StringUtils.parseDate(stampString);
        }
        catch (ParseException parseExc) {
            /*
             * if date could not be parsed but XML is valid, don't shutdown
             * connection by throwing an exception instead set timestamp to epoch 
             * so that it is obviously wrong. 
             */
            if (stamp == null) {
                stamp = new Date(0);
            }
        }
        
        
        DelayInformation delayInformation = new DelayInformation(stamp);
        delayInformation.setFrom(parser.getAttributeValue("", "from"));
        String reason = parser.nextText();

        /*
         * parser.nextText() returns empty string if there is no reason.
         * DelayInformation API specifies that null should be returned in that
         * case.
         */
        reason = reason.isEmpty() ? null : reason;
        delayInformation.setReason(reason);
        
        return delayInformation;
    }
}
