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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public abstract class IqProvider<I extends IQ> extends AbstractProvider<I> {

    public final I parse(XmlPullParser parser, IqBuilder iqCommon)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return parse(parser, iqCommon, null);
    }

    public final I parse(XmlPullParser parser, IqBuilder iqData, XmlEnvironment outerXmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        final XmlEnvironment xmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);

        return parse(parser, initialDepth, iqData, xmlEnvironment);
    }

    public abstract I parse(XmlPullParser parser, int initialDepth, IqBuilder iqData, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException;

}
