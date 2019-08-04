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

import java.io.IOException;
import java.util.HashMap;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.omemo.element.OmemoBundleElement_VAxolotl;

/**
 * Smack ExtensionProvider that parses OMEMO bundle element into OmemoBundleElement objects.
 *
 * @author Paul Schaub
 */
public class OmemoBundleVAxolotlProvider extends ExtensionElementProvider<OmemoBundleElement_VAxolotl> {
    @Override
    public OmemoBundleElement_VAxolotl parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        boolean stop = false;
        boolean inPreKeys = false;

        int signedPreKeyId = -1;
        String signedPreKey = null;
        String signedPreKeySignature = null;
        String identityKey = null;
        HashMap<Integer, String> preKeys = new HashMap<>();

        while (!stop) {
            XmlPullParser.Event tag = parser.next();
            String name = parser.getName();
            switch (tag) {
                case START_ELEMENT:
                    final int attributeCount = parser.getAttributeCount();
                    // <signedPreKeyPublic>
                    if (SIGNED_PRE_KEY_PUB.equals(name)) {
                        for (int i = 0; i < attributeCount; i++) {
                            if (SIGNED_PRE_KEY_ID.equals(parser.getAttributeName(i))) {
                                int id = Integer.parseInt(parser.getAttributeValue(i));
                                signedPreKey = StringUtils.removeNewLines(parser.nextText());
                                signedPreKeyId = id;
                            }
                        }
                    }
                    // <bundleGetSignedPreKeySignature>
                    else if (SIGNED_PRE_KEY_SIG.equals(name)) {
                        signedPreKeySignature = StringUtils.removeNewLines(parser.nextText());
                    }
                    // <deserializeIdentityKey>
                    else if (IDENTITY_KEY.equals(name)) {
                        identityKey = StringUtils.removeNewLines(parser.nextText());
                    }
                    // <deserializeECPublicKeys>
                    else if (PRE_KEYS.equals(name)) {
                        inPreKeys = true;
                    }
                    // <preKeyPublic preKeyId='424242'>
                    else if (inPreKeys && PRE_KEY_PUB.equals(name)) {
                        for (int i = 0; i < attributeCount; i++) {
                            if (PRE_KEY_ID.equals(parser.getAttributeName(i))) {
                                preKeys.put(Integer.parseInt(parser.getAttributeValue(i)),
                                        StringUtils.removeNewLines(parser.nextText()));
                            }
                        }
                    }
                    break;
                case END_ELEMENT:
                    if (BUNDLE.equals(name)) {
                        stop = true;
                    }
                    break;
                default:
                    // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                    break;
            }
        }
        return new OmemoBundleElement_VAxolotl(signedPreKeyId, signedPreKey, signedPreKeySignature, identityKey, preKeys);
    }
}
