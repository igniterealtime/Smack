/**
 *
 * Copyright © 2018 Paul Schaub
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

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class BodyProviderTest extends SmackTestSuite {

    @Test
    public void bodyProviderTest() throws Exception {
        String xml = "<body xmlns='jabber:client' xml:lang='en'>This is a message.</body>";
        XmlPullParser parser = TestUtils.getParser(xml);
        Message.Body body = BodyProvider.TEST_INSTANCE.parse(parser);
        assertEquals("This is a message.", body.getMessage());
        assertEquals("en", body.getLanguage());
    }
}
