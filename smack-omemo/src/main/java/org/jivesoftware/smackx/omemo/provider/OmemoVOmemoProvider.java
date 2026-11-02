/*
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.element.OmemoElement_VOmemo;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement_VOmemo;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement_VOmemo;
import org.jivesoftware.smackx.omemo.element.OmemoKeysElement_VOmemo;

import org.jxmpp.JxmppContext;

/**
 * Smack ExtensionProvider that parses incoming OMEMO Message element for omemo:2 into OmemoMessageElement objects.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoVOmemoProvider extends ExtensionElementProvider<OmemoElement_VOmemo> {

    @Override
    public OmemoElement_VOmemo parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext) throws XmlPullParserException, IOException {
        int sid = -1;

        List<OmemoKeysElement_VOmemo> omemoKeys = new ArrayList<>();
        byte[] iv = null;
        byte[] payload = null;

        outerloop:
        while (true) {
            XmlPullParser.Event tag = parser.next();
            switch (tag) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case OmemoHeaderElement.ELEMENT:
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        if (parser.getAttributeName(i).equals(OmemoHeaderElement.ATTR_SID)) {
                            sid = Integer.parseInt(parser.getAttributeValue(i));
                        }
                    }
                    break;
                case OmemoKeysElement_VOmemo.ELEMENT:
                    String jid = parser.getAttributeValue(OmemoKeysElement_VOmemo.ATTR_JID);
                    List<OmemoKeyElement_VOmemo> keys = parseKeyChildElement(parser, parser.getDepth());
                    OmemoKeysElement_VOmemo keysElement = new OmemoKeysElement_VOmemo(jid, keys);
                    omemoKeys.add(keysElement);
                    break;
                case OmemoHeaderElement.ATTR_IV:
                    iv = Base64.decode(parser.nextText());
                    break;
                case OmemoElement.ATTR_PAYLOAD:
                    payload = Base64.decode(parser.nextText());
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

        OmemoHeaderElement_VOmemo header = new OmemoHeaderElement_VOmemo(sid, omemoKeys, iv);
        return new OmemoElement_VOmemo(header, payload);
    }

    public static List<OmemoKeyElement_VOmemo> parseKeyChildElement(XmlPullParser parser, int initialDepth)
            throws XmlPullParserException, IOException {
        List<OmemoKeyElement_VOmemo> keyElements = new ArrayList<>();

        outerloop:
        while (true) {
            XmlPullParser.Event tag = parser.next();
            switch (tag) {
            case START_ELEMENT:
                String name = parser.getName();
                if (name.equals(OmemoKeyElement.ELEMENT)) {
                    boolean prekey = false;
                    int rid = -1;
                    for (int i = 0; i < parser.getAttributeCount(); i++) {
                        if (parser.getAttributeName(i).equals(OmemoKeyElement_VOmemo.ATTR_PREKEY)) {
                            prekey = Boolean.parseBoolean(parser.getAttributeValue(i));
                        }
                        else if (parser.getAttributeName(i).equals(OmemoKeyElement.ATTR_RID)) {
                            rid = Integer.parseInt(parser.getAttributeValue(i));
                        }
                    }
                    keyElements.add(new OmemoKeyElement_VOmemo(Base64.decode(parser.nextText()), rid, prekey));
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
        return keyElements;
    }
}
