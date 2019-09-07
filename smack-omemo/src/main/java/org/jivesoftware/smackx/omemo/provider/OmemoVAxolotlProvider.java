/**
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

import static org.jivesoftware.smackx.omemo.element.OmemoElement.ATTR_PAYLOAD;
import static org.jivesoftware.smackx.omemo.element.OmemoElement.NAME_ENCRYPTED;

import java.io.IOException;
import java.util.ArrayList;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.omemo.element.OmemoElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement;

/**
 * Smack ExtensionProvider that parses incoming OMEMO Message element into OmemoMessageElement objects.
 *
 * @author Paul Schaub
 */
public class OmemoVAxolotlProvider extends ExtensionElementProvider<OmemoElement_VAxolotl> {

    @Override
    public OmemoElement_VAxolotl parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        boolean inEncrypted = true;
        int sid = -1;
        ArrayList<OmemoKeyElement> keys = new ArrayList<>();
        byte[] iv = null;
        byte[] payload = null;

        while (inEncrypted) {
            XmlPullParser.Event tag = parser.next();
            String name = parser.getName();
            switch (tag) {
                case START_ELEMENT:
                    switch (name) {
                        case OmemoHeaderElement.ELEMENT:
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals(OmemoHeaderElement.ATTR_SID)) {
                                    sid = Integer.parseInt(parser.getAttributeValue(i));
                                }
                            }
                            break;
                        case OmemoKeyElement.ELEMENT:
                            boolean prekey = false;
                            int rid = -1;
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals(OmemoKeyElement.ATTR_PREKEY)) {
                                    prekey = Boolean.parseBoolean(parser.getAttributeValue(i));
                                } else if (parser.getAttributeName(i).equals(OmemoKeyElement.ATTR_RID)) {
                                    rid = Integer.parseInt(parser.getAttributeValue(i));
                                }
                            }
                            keys.add(new OmemoKeyElement(Base64.decode(parser.nextText()), rid, prekey));
                            break;
                        case OmemoHeaderElement.ATTR_IV:
                            iv = Base64.decode(parser.nextText());
                            break;
                        case ATTR_PAYLOAD:
                            payload = Base64.decode(parser.nextText());
                            break;
                    }
                    break;
                case END_ELEMENT:
                    if (name.equals(NAME_ENCRYPTED)) {
                        inEncrypted = false;
                    }
                    break;
                default:
                    // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                    break;
            }
        }
        OmemoHeaderElement_VAxolotl header = new OmemoHeaderElement_VAxolotl(sid, keys, iv);
        return new OmemoElement_VAxolotl(header, payload);
    }
}
