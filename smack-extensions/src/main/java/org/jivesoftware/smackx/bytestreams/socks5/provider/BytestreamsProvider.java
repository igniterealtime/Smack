/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.bytestreams.socks5.provider;

import java.io.IOException;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.Mode;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Parses a bytestream packet.
 * 
 * @author Alexander Wenckus
 */
public class BytestreamsProvider extends IQProvider<Bytestream> {

    @Override
    public Bytestream parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException {
        boolean done = false;

        Bytestream toReturn = new Bytestream();

        String id = parser.getAttributeValue("", "sid");
        String mode = parser.getAttributeValue("", "mode");

        // streamhost
        String JID = null;
        String host = null;
        String port = null;

        int eventType;
        String elementName;
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (elementName.equals(Bytestream.StreamHost.ELEMENTNAME)) {
                    JID = parser.getAttributeValue("", "jid");
                    host = parser.getAttributeValue("", "host");
                    port = parser.getAttributeValue("", "port");
                }
                else if (elementName.equals(Bytestream.StreamHostUsed.ELEMENTNAME)) {
                    toReturn.setUsedHost(parser.getAttributeValue("", "jid"));
                }
                else if (elementName.equals(Bytestream.Activate.ELEMENTNAME)) {
                    toReturn.setToActivate(parser.getAttributeValue("", "jid"));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (elementName.equals("streamhost")) {
                    if (port == null) {
                        toReturn.addStreamHost(JID, host);
                    }
                    else {
                        toReturn.addStreamHost(JID, host, Integer.parseInt(port));
                    }
                    JID = null;
                    host = null;
                    port = null;
                }
                else if (elementName.equals("query")) {
                    done = true;
                }
            }
        }

        if (mode == null) {
            toReturn.setMode(Mode.tcp);
        } else {
            toReturn.setMode((Bytestream.Mode.fromName(mode)));
        }
        toReturn.setSessionID(id);
        return toReturn;
    }

}
