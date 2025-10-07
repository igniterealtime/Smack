/*
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
package org.jivesoftware.smack.xml.stax;

import java.io.IOException;
import java.io.StringReader;

import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.junit.jupiter.api.Test;

public class StaxParserTest {

    @Test
    public void factoryTest() throws XmlPullParserException, IOException {
        StaxXmlPullParserFactory staxXmlPullParserFactory = new StaxXmlPullParserFactory();
        XmlPullParser parser = staxXmlPullParserFactory.newXmlPullParser(new StringReader("<element/>"));
        parser.next();
    }

}
