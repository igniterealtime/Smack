/**
 *
 * Copyright 2021 Florian Schmaus.
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.junit.jupiter.api.Test;

public class AbstractProviderTest {

    private static final ExtensionElementProvider<ExtensionElement> NUMBER_FORMAT_THROWING_PROVIDER = new ExtensionElementProvider<ExtensionElement>() {
        @Override
        public ExtensionElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            throw new NumberFormatException();
        }
    };

    private static final String MESSAGE = "dummy message";
    private static final int VALUE = 14768234;
    private static final ExtensionElementProvider<ExtensionElement> PARSE_EXCEPTION_THROWING_PROVIDER = new ExtensionElementProvider<ExtensionElement>() {
        @Override
        public ExtensionElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            throw new ParseException(MESSAGE, VALUE);
        }
    };

    @Test
    public void testWrapsNumberFormatException() throws XmlPullParserException, IOException {
        XmlPullParser parser = SmackTestUtil.createDummyParser();
        assertThrows(AbstractProvider.NumberFormatParseException.class,
                        () -> NUMBER_FORMAT_THROWING_PROVIDER.parse(parser));
    }

    @Test
    public void testWrapsParseException() throws XmlPullParserException, IOException {
        XmlPullParser parser = SmackTestUtil.createDummyParser();
        AbstractProvider.TextParseException testParseException = assertThrows(AbstractProvider.TextParseException.class,
                        () -> PARSE_EXCEPTION_THROWING_PROVIDER.parse(parser));
        ParseException parseException = testParseException.getParseException();
        assertEquals(MESSAGE, parseException.getMessage());
        assertEquals(VALUE, parseException.getErrorOffset());
    }
}
