/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.packet.SaslNonza;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public final class SaslSuccessProvider extends NonzaProvider<SaslNonza.Success> {

    public static final SaslSuccessProvider INSTANCE = new SaslSuccessProvider();

    private SaslSuccessProvider() {
    }

    @Override
    public SaslNonza.Success parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                    throws IOException, XmlPullParserException {
        String data = parser.nextText();
        return new SaslNonza.Success(data);
    }

}
