/**
 *
 * Copyright 2017-2021 Florian Schmaus, 2018 Paul Schaub.
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

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.ox.element.SigncryptElement;

/**
 * {@link org.jivesoftware.smack.provider.ExtensionElementProvider} implementation for the {@link SigncryptElement}.
 */
public class SigncryptElementProvider extends OpenPgpContentElementProvider<SigncryptElement> {

    public static final SigncryptElementProvider INSTANCE = new SigncryptElementProvider();

    @Override
    public SigncryptElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        OpenPgpContentElementData data = parseOpenPgpContentElementData(parser, initialDepth);
        return new SigncryptElement(data.to, data.rpad, data.timestamp, data.payload);
    }

}
