/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.reference;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.reference.element.ReferenceElement;
import org.jivesoftware.smackx.reference.provider.ReferenceProvider;

import org.junit.Test;

public class ReferenceTest extends SmackTestSuite {

    @Test
    public void providerMentionTest() throws Exception {
        String xml = "<reference xmlns='urn:xmpp:reference:0' " +
                        "begin='72' " +
                        "end='78' " +
                        "type='mention' " +
                        "uri='xmpp:juliet@capulet.lit' />";
        ReferenceElement element = new ReferenceElement(72, 78, ReferenceElement.Type.mention, null,
                "xmpp:juliet@capulet.lit");
        assertXMLEqual(xml, element.toXML().toString());
        assertEquals(72, (int) element.getBegin());
        assertEquals(78, (int) element.getEnd());
        assertEquals(ReferenceElement.Type.mention, element.getType());
        assertNull(element.getAnchor());
        assertEquals("xmpp:juliet@capulet.lit", element.getUri());

        ReferenceElement parsed = ReferenceProvider.TEST_PROVIDER.parse(TestUtils.getParser(xml));
        assertXMLEqual(xml, parsed.toXML().toString());
    }

    /**
     * TODO: The uri might not be following the XMPP schema.
     * That shouldn't matter though.
     * @throws Exception
     */
    @Test
    public void providerDataTest() throws Exception {
        String xml = "<reference xmlns='urn:xmpp:reference:0' " +
                "type='data' " +
                "uri='xmpp:fdp.shakespeare.lit?;node=fdp/submitted/stan.isode.net/accidentreport;item=ndina872be' />";
        ReferenceElement element = new ReferenceElement(null, null, ReferenceElement.Type.data, null,
                "xmpp:fdp.shakespeare.lit?;node=fdp/submitted/stan.isode.net/accidentreport;item=ndina872be");
        assertXMLEqual(xml, element.toXML().toString());

        assertNull(element.getBegin());
        assertNull(element.getEnd());
        assertNull(element.getAnchor());
        assertEquals(ReferenceElement.Type.data, element.getType());
        assertEquals("xmpp:fdp.shakespeare.lit?;node=fdp/submitted/stan.isode.net/accidentreport;item=ndina872be", element.getUri());

        ReferenceElement parsed = ReferenceProvider.TEST_PROVIDER.parse(TestUtils.getParser(xml));
        assertXMLEqual(xml, parsed.toXML().toString());
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginGreaterEndIllegalTest() {
        new ReferenceElement(100, 10, ReferenceElement.Type.mention, null, "test@test.test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void beginSmallerZeroTest() {
        new ReferenceElement(-1, 12, ReferenceElement.Type.data, null, "test@test.test");
    }

    @Test(expected = IllegalArgumentException.class)
    public void endSmallerZeroTest() {
        new ReferenceElement(12, -2, ReferenceElement.Type.mention, null, "test@test.test");
    }

    @Test(expected = NullPointerException.class)
    public void typeArgumentNullTest() {
        new ReferenceElement(1, 2, null, null, "test@test.test");
    }

    /*
     * TODO: Later maybe remove this test in case the uri attribute becomes optional.
    @Test(expected = NullPointerException.class)
    public void uriArgumentNullTest() {
        new ReferenceElement(1, 2, ReferenceElement.Type.mention, null, null);
    }
    */
}
