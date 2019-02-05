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

import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.BUNDLE;
import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.IDENTITY_KEY;
import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.PRE_KEYS;
import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.PRE_KEY_ID;
import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.PRE_KEY_PUB;
import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.SIGNED_PRE_KEY_ID;
import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.SIGNED_PRE_KEY_PUB;
import static org.jivesoftware.smackx.omemo.element.OmemoBundleElement.SIGNED_PRE_KEY_SIG;
import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.util.HashMap;

import org.jivesoftware.smack.provider.ExtensionElementProvider;

import org.jivesoftware.smackx.omemo.element.OmemoBundleElement_VAxolotl;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Smack ExtensionProvider that parses OMEMO bundle element into OmemoBundleElement objects.
 *
 * @author Paul Schaub
 */
public class OmemoBundleVAxolotlProvider extends ExtensionElementProvider<OmemoBundleElement_VAxolotl> {
    @Override
    public OmemoBundleElement_VAxolotl parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        boolean stop = false;
        boolean inPreKeys = false;

        int signedPreKeyId = -1;
        String signedPreKey = null;
        String signedPreKeySignature = null;
        String identityKey = null;
        HashMap<Integer, String> preKeys = new HashMap<>();

        while (!stop) {
            int tag = parser.next();
            String name = parser.getName();
            switch (tag) {
                case START_TAG:
                    // <signedPreKeyPublic>
                    if (name.equals(SIGNED_PRE_KEY_PUB)) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals(SIGNED_PRE_KEY_ID)) {
                                int id = Integer.parseInt(parser.getAttributeValue(i));
                                signedPreKey = parser.nextText();
                                signedPreKeyId = id;
                            }
                        }
                    }
                    // <bundleGetSignedPreKeySignature>
                    else if (name.equals(SIGNED_PRE_KEY_SIG)) {
                        signedPreKeySignature = parser.nextText();
                    }
                    // <deserializeIdentityKey>
                    else if (name.equals(IDENTITY_KEY)) {
                        identityKey = parser.nextText();
                    }
                    // <deserializeECPublicKeys>
                    else if (name.equals(PRE_KEYS)) {
                        inPreKeys = true;
                    }
                    // <preKeyPublic preKeyId='424242'>
                    else if (inPreKeys && name.equals(PRE_KEY_PUB)) {
                        for (int i = 0; i < parser.getAttributeCount(); i++) {
                            if (parser.getAttributeName(i).equals(PRE_KEY_ID)) {
                                preKeys.put(Integer.parseInt(parser.getAttributeValue(i)),
                                        parser.nextText());
                            }
                        }
                    }
                    break;
                case END_TAG:
                    if (name.equals(BUNDLE)) {
                        stop = true;
                    }
                    break;
            }
        }
        return new OmemoBundleElement_VAxolotl(signedPreKeyId, signedPreKey, signedPreKeySignature, identityKey, preKeys);
    }
}
