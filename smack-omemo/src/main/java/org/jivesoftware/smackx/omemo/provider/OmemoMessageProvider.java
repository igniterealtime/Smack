/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.omemo.elements.OmemoMessageElement;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Encrypted.*;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

/**
 * Smack ExtensionProvider that parses incoming OMEMO Message elements into OmemoMessageElement objects.
 *
 * @author Paul Schaub
 */
public class OmemoMessageProvider extends ExtensionElementProvider<OmemoMessageElement> {

    @Override
    public OmemoMessageElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        boolean inEncrypted = true;
        int sid = -1;
        ArrayList<OmemoMessageElement.OmemoHeader.Key> keys = new ArrayList<>();
        byte[] iv = null;
        byte[] payload = null;

        while (inEncrypted) {
            int tag = parser.next();
            String name = parser.getName();
            switch (tag) {
                case START_TAG:
                    switch (name) {
                        case HEADER:
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals(SID)) {
                                    sid = Integer.parseInt(parser.getAttributeValue(i));
                                }
                            }
                            break;
                        case KEY:
                            boolean prekey = false;
                            int rid = -1;
                            for (int i = 0; i < parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals(PREKEY)) {
                                    prekey = Boolean.parseBoolean(parser.getAttributeValue(i));
                                } else if (parser.getAttributeName(i).equals(RID)) {
                                    rid = Integer.parseInt(parser.getAttributeValue(i));
                                }
                            }
                            keys.add(new OmemoMessageElement.OmemoHeader.Key(Base64.decode(parser.nextText()), rid, prekey));
                            break;
                        case IV:
                            iv = Base64.decode(parser.nextText());
                            break;
                        case PAYLOAD:
                            payload = Base64.decode(parser.nextText());
                            break;
                    }
                    break;
                case END_TAG:
                    if (name.equals(ENCRYPTED)) {
                        inEncrypted = false;
                    }
                    break;
            }
        }
        OmemoMessageElement.OmemoHeader header = new OmemoMessageElement.OmemoHeader(sid, keys, iv);
        return new OmemoMessageElement(header, payload);
    }
}
