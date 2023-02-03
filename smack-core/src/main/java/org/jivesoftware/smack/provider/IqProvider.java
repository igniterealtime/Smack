/**
 *
 * Copyright 2019-2022 Florian Schmaus
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
import java.text.ParseException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * An abstract class for parsing custom {@link IQ} packets. Each IqProvider must be registered with the {@link
 * ProviderManager} for it to be used. Every implementation of this abstract class <b>must</b> have a public,
 * no-argument constructor.
 * <h2>Custom IQ Provider Example</h2>
 * <p>
 * Let us assume you want to write a provider for a new, unsupported IQ in Smack.
 * </p>
 * <pre>{@code
 * <iq type='set' from='juliet@capulet.example/balcony' to='romeo@montage.example'>
 *   <myiq xmlns='example:iq:foo' token='secret'>
 *     <user age='42'>John Doe</user>
 *     <location>New York</location>
 *   </myiq>
 * </iq>
 * }</pre>
 * The custom IQ provider may look like the follows
 * <pre>{@code
 * public class MyIQProvider extends IQProvider<MyIQ> {
 *
 *   {@literal @}Override
 *   public MyIQ parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
 *     // Define the data we are trying to collect with sane defaults
 *     int age = -1;
 *     String user = null;
 *     String location = null;
 *
 *     // Start parsing loop
 *     outerloop: while(true) {
 *       XmlPullParser.Event eventType = parser.next();
 *       switch(eventType) {
 *       case START_ELEMENT:
 *         String elementName = parser.getName();
 *         switch (elementName) {
 *         case "user":
 *           age = ParserUtils.getIntegerAttribute(parser, "age");
 *           user = parser.nextText();
 *           break;
 *         case "location"
 *           location = parser.nextText();
 *           break;
 *         }
 *         break;
 *       case END_ELEMENT:
 *         // Abort condition: if the are on a end tag (closing element) of the same depth
 *         if (parser.getDepth() == initialDepth) {
 *           break outerloop;
 *         }
 *         break;
 *       default:
 *         // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
 *         break;
 *       }
 *     }
 *
 *     // Construct the IQ instance at the end of parsing, when all data has been collected
 *     return new MyIQ(user, age, location);
 *   }
 * }
 * }</pre>
 *
 * @param <I> the {@link IQ} that is parsed by implementations.
 */
public abstract class IqProvider<I extends IQ> extends AbstractProvider<I> {

    public final I parse(XmlPullParser parser, IqData iqCommon)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return parse(parser, iqCommon, null);
    }

    public final I parse(XmlPullParser parser, IqData iqData, XmlEnvironment outerXmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        final XmlEnvironment xmlEnvironment = XmlEnvironment.from(parser, outerXmlEnvironment);

        I i = wrapExceptions(() -> parse(parser, initialDepth, iqData, xmlEnvironment));

        // Parser should be at end tag of the consumed/parsed element
        ParserUtils.forwardToEndTagOfDepth(parser, initialDepth);
        return i;
    }

    public abstract I parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException, ParseException;

}
