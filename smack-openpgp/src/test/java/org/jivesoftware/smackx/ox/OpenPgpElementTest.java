/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.provider.OpenPgpContentElementProvider;
import org.jivesoftware.smackx.ox.provider.OpenPgpElementProvider;

import org.junit.Test;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class OpenPgpElementTest extends SmackTestSuite {

    private final Set<Jid> recipients;

    // 2014-07-10T15:06:00.000+00:00
    private static final Date testDate = new Date(1405004760000L);

    public OpenPgpElementTest() throws XmppStringprepException {
        super();
        Set<Jid> jids = new HashSet<>();
        jids.add(JidCreate.bareFrom("alice@wonderland.lit"));
        jids.add(JidCreate.bareFrom("bob@builder.lit"));
        this.recipients = Collections.unmodifiableSet(jids);
    }

    @Test
    public void providerTest() throws Exception {
        String expected =
                "<openpgp xmlns='urn:xmpp:openpgp:0'>" +
                        "BASE64_OPENPGP_MESSAGE" +
                        "</openpgp>";

        OpenPgpElement element = new OpenPgpElement("BASE64_OPENPGP_MESSAGE");

        assertXMLEqual(expected, element.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(expected);
        OpenPgpElement parsed = OpenPgpElementProvider.TEST_INSTANCE.parse(parser);
        assertEquals(element.getEncryptedBase64MessageContent(), parsed.getEncryptedBase64MessageContent());
    }

    @Test
    public void simplifiedConstructorTest() {
        ArrayList<ExtensionElement> payload = new ArrayList<>();
        payload.add(new Message.Body("de", "Hallo Welt!"));
        CryptElement element = new CryptElement(recipients, payload);

        assertNotNull(element.getTimestamp());
    }

    @Test
    public void signElementProviderTest() throws Exception {
        String expected =
                "<sign xmlns='urn:xmpp:openpgp:0'>" +
                        "<to jid='alice@wonderland.lit'/>" +
                        "<to jid='bob@builder.lit'/>" +
                        "<time stamp='2014-07-10T15:06:00.000+00:00'/>" +
                        "<payload>" +
                        "<body xmlns='jabber:client' xml:lang='en'>Hello World!</body>" +
                        "</payload>" +
                        "</sign>";

        List<ExtensionElement> payload = new ArrayList<>();
        payload.add(new Message.Body("en", "Hello World!"));
        SignElement element = new SignElement(recipients, testDate, payload);

        assertXMLEqual(expected, element.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(expected);
        SignElement parsed = (SignElement) OpenPgpContentElementProvider.parseOpenPgpContentElement(parser);

        assertEquals(element.getTimestamp(), parsed.getTimestamp());
        assertEquals(element.getTo(), parsed.getTo());
        assertEquals(element.getExtensions(), parsed.getExtensions());
    }

    @Test
    public void cryptElementProviderTest() throws Exception {
        String expected =
                "<crypt xmlns='urn:xmpp:openpgp:0'>" +
                        "<to jid='alice@wonderland.lit'/>" +
                        "<time stamp='2014-07-10T15:06:00.000+00:00'/>" +
                        "<payload>" +
                        "<body xmlns='jabber:client' xml:lang='en'>The cake is a lie.</body>" +
                        "</payload>" +
                        "<rpad>f0rm1l4n4-mT8y33j!Y%fRSrcd^ZE4Q7VDt1L%WEgR!kv</rpad>" +
                        "</crypt>";
        List<ExtensionElement> payload = new ArrayList<>();
        payload.add(new Message.Body("en", "The cake is a lie."));
        Set<Jid> to = new HashSet<>();
        to.add(JidCreate.bareFrom("alice@wonderland.lit"));
        CryptElement element = new CryptElement(to,
                "f0rm1l4n4-mT8y33j!Y%fRSrcd^ZE4Q7VDt1L%WEgR!kv",
                testDate,
                payload);

        assertXMLEqual(expected, element.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(expected);
        CryptElement parsed = (CryptElement) OpenPgpContentElementProvider.parseOpenPgpContentElement(parser);

        assertEquals(element.getTimestamp(), parsed.getTimestamp());
        assertEquals(element.getTo(), parsed.getTo());
        assertEquals(element.getExtensions(), parsed.getExtensions());
    }

    @Test
    public void signcryptElementProviderTest() throws Exception {
        String expected =
                "<signcrypt xmlns='urn:xmpp:openpgp:0'>" +
                        "<to jid='juliet@example.org'/>" +
                        "<time stamp='2014-07-10T15:06:00.000+00:00'/>" +
                        "<payload>" +
                        "<body xmlns='jabber:client' xml:lang='en'>This is a secret message.</body>" +
                        "</payload>" +
                        "<rpad>f0rm1l4n4-mT8y33j!Y%fRSrcd^ZE4Q7VDt1L%WEgR!kv</rpad>" +
                        "</signcrypt>";

        List<ExtensionElement> payload = new ArrayList<>();
        payload.add(new Message.Body("en", "This is a secret message."));
        Set<Jid> jids = new HashSet<>();
        jids.add(JidCreate.bareFrom("juliet@example.org"));
        SigncryptElement element = new SigncryptElement(jids,
                "f0rm1l4n4-mT8y33j!Y%fRSrcd^ZE4Q7VDt1L%WEgR!kv",
                testDate, payload);

        assertXMLEqual(expected, element.toXML().toString());

        XmlPullParser parser = TestUtils.getParser(expected);
        SigncryptElement parsed = (SigncryptElement) OpenPgpContentElementProvider.parseOpenPgpContentElement(parser);

        assertEquals(element.getTimestamp(), parsed.getTimestamp());
        assertEquals(element.getTo(), parsed.getTo());
        assertEquals(element.getExtensions(), parsed.getExtensions());
        assertEquals(payload.get(0), element.getExtension(Message.Body.NAMESPACE));
        assertEquals(payload.get(0), element.getExtension(Message.Body.ELEMENT, Message.Body.NAMESPACE));
    }

    @Test(expected = XmlPullParserException.class)
    public void openPgpContentElementProvider_invalidElementTest() throws IOException, XmlPullParserException {
        String invalidElementXML = "<payload>" +
                "<body xmlns='jabber:client' xml:lang='en'>This is a secret message.</body>" +
                "</payload>";
        OpenPgpContentElementProvider.parseOpenPgpContentElement(invalidElementXML);
    }

}
