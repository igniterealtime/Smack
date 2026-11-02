/*
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

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.omemo.element.OmemoDeviceElement;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement_VOmemo;
import org.jivesoftware.smackx.omemo.provider.OmemoDeviceListVAxolotlProvider;
import org.jivesoftware.smackx.omemo.provider.OmemoDeviceListVOmemoProvider;

import org.junit.Test;

/**
 * Test serialization and parsing of DeviceListElement.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class OmemoDeviceListElementTest extends SmackTestSuite {

    @Test
    public void serializationTest() throws Exception {
        Set<OmemoDeviceElement> devices = new HashSet<>(Set.of(
                new OmemoDeviceElement(1234),
                new OmemoDeviceElement(9876)));

        OmemoDeviceListElement_VAxolotl element = new OmemoDeviceListElement_VAxolotl(devices);
        String xml = element.toXML().toString();

        XmlPullParser parser = TestUtils.getParser(xml);
        OmemoDeviceListElement_VAxolotl parsed = new OmemoDeviceListVAxolotlProvider().parse(parser);

        assertEquals("Parsed element must equal the original.", parsed.getDevices(), element.getDevices());
        assertEquals("Generated XML must match.",
                "<list xmlns='eu.siacs.conversations.axolotl'>" +
                        "<device id='1234'/>" +
                        "<device id='9876'/>" +
                        "</list>",
                xml);

        OmemoDeviceListElement_VOmemo element2 = new OmemoDeviceListElement_VOmemo(devices);
        String xml2 = element2.toXML().toString();

        XmlPullParser parser2 = TestUtils.getParser(xml2);
        OmemoDeviceListElement_VOmemo parsed2 = new OmemoDeviceListVOmemoProvider().parse(parser2);

        assertEquals("Parsed element must equal the original.", parsed2.getDevices(), element2.getDevices());
        assertEquals("Generated XML must match.",
                "<devices xmlns='urn:xmpp:omemo:2'>" +
                        "<device id='1234'/>" +
                        "<device id='9876'/>" +
                        "</devices>",
                xml2);
    }
}
