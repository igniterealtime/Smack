/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.provider;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.START_TAG;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;

import org.jxmpp.util.XmppDateTime;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class PublicKeysListElementProvider extends ExtensionElementProvider<PublicKeysListElement> {

    public static final PublicKeysListElementProvider TEST_INSTANCE = new PublicKeysListElementProvider();

    @Override
    public PublicKeysListElement parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException, ParseException {

        PublicKeysListElement.Builder builder = PublicKeysListElement.builder();

        while (true) {

            int tag = parser.nextTag();
            String name = parser.getName();

            switch (tag) {
                case START_TAG:

                    if (PublicKeysListElement.PubkeyMetadataElement.ELEMENT.equals(name)) {
                        String finger = parser.getAttributeValue(null,
                                PublicKeysListElement.PubkeyMetadataElement.ATTR_V4_FINGERPRINT);
                        String dt = parser.getAttributeValue(null,
                                PublicKeysListElement.PubkeyMetadataElement.ATTR_DATE);
                        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(finger);
                        Date date = XmppDateTime.parseXEP0082Date(dt);
                        builder.addMetadata(new PublicKeysListElement.PubkeyMetadataElement(fingerprint, date));
                    }
                    break;

                case END_TAG:

                    if (name.equals(PublicKeysListElement.ELEMENT)) {
                        return builder.build();
                    }
            }
        }
    }
}
