/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.omemo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;

import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement_VAxolotl;
import org.jivesoftware.smackx.omemo.provider.OmemoDeviceListVAxolotlProvider;

import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

/**
 * Test serialization and parsing of DeviceListElement.
 */
public class OmemoDeviceListVAxolotlElementTest extends SmackTestSuite {

    @Test
    public void serializationTest() throws Exception {
        HashSet<Integer> ids = new HashSet<>();
        ids.add(1234);
        ids.add(9876);

        OmemoDeviceListElement_VAxolotl element = new OmemoDeviceListElement_VAxolotl(ids);
        String xml = element.toXML().toString();

        XmlPullParser parser = TestUtils.getParser(xml);
        OmemoDeviceListElement_VAxolotl parsed = new OmemoDeviceListVAxolotlProvider().parse(parser);

        assertTrue("Parsed element must equal the original.", parsed.getDeviceIds().equals(element.getDeviceIds()));
        assertEquals("Generated XML must match.",
                "<list xmlns='eu.siacs.conversations.axolotl'>" +
                            "<device id='1234'/>" +
                            "<device id='9876'/>" +
                        "</list>",
                xml);
    }
}
