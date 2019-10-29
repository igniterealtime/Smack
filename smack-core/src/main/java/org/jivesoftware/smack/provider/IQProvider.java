/**
 *
 * Copyright 2003-2007 Jive Software.
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
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * <p>
 * <b>Deprecation Notice:</b> This class is deprecated, use {@link IQProvider} instead.
 * </p>
 * An abstract class for parsing custom IQ packets. Each IQProvider must be registered with
 * the ProviderManager class for it to be used. Every implementation of this
 * abstract class <b>must</b> have a public, no-argument constructor.
 *
 * @author Matt Tucker
 */
public abstract class IQProvider<I extends IQ> extends IqProvider<I> {

    public final I parse(XmlPullParser parser) throws IOException, XmlPullParserException, SmackParsingException {
        return parse(parser, (XmlEnvironment) null);
    }

    public final I parse(XmlPullParser parser, XmlEnvironment outerXmlEnvironment) throws IOException, XmlPullParserException, SmackParsingException {
        // XPP3 calling convention assert: Parser should be at start tag
        ParserUtils.assertAtStartTag(parser);

        final int initialDepth = parser.getDepth();
        final XmlEnvironment xmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);

        I e = parse(parser, initialDepth, xmlEnvironment);

        // XPP3 calling convention assert: Parser should be at end tag of the consumed/parsed element
        ParserUtils.forwardToEndTagOfDepth(parser, initialDepth);
        return e;
    }

    @Override
    public final I parse(XmlPullParser parser, int initialDepth, IqBuilder iqData, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        // Old-style IQ parsers do not need IqData.
        return parse(parser, initialDepth, xmlEnvironment);
    }

    public abstract I parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException;

}
