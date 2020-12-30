/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.url_address_information;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.url_address_information.element.UrlDataElement;
import org.jivesoftware.smackx.url_address_information.http.element.CookieElement;
import org.jivesoftware.smackx.url_address_information.http.element.HeaderElement;
import org.jivesoftware.smackx.url_address_information.http.element.HttpAuthElement;
import org.jivesoftware.smackx.url_address_information.provider.UrlDataElementProvider;

import org.junit.jupiter.api.Test;

public class UrlDataElementTest extends SmackTestSuite {

    @Test
    public void simpleSerializationTest() throws XmlPullParserException, IOException, SmackParsingException {
        UrlDataElement urlDataElement = new UrlDataElement("http://www.jabber.org/members/index.php",
                null,
                Collections.singletonList(HttpAuthElement.basicAuth()),
                null, null);

        final String expectedXml = "" +
                "<url-data xmlns='http://jabber.org/protocol/url-data' " +
                "xmlns:http='http://jabber.org/protocol/url-data/scheme/http' " +
                "target='http://www.jabber.org/members/index.php'>" +
                "<http:auth scheme='basic'/>" +
                "</url-data>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }

    @Test
    public void additionalAuthParamTest() throws XmlPullParserException, IOException, SmackParsingException {

        UrlDataElement urlDataElement = new UrlDataElement("http://www.jabber.org/members/index.php",
                null,
                Collections.singletonList(HttpAuthElement.basicAuth(
                        "www.jabber.org",
                        "defaultuser",
                        "defaultpwd"
                )),
                null,
                null);

        final String expectedXml = "<url-data xmlns='http://jabber.org/protocol/url-data'\n" +
                "      xmlns:http='http://jabber.org/protocol/url-data/scheme/http'\n" +
                "      target='http://www.jabber.org/members/index.php'>\n" +
                "    <http:auth scheme='basic'>\n" +
                "      <http:auth-param name='realm' value='www.jabber.org'/>\n" +
                "      <http:auth-param name='username' value='defaultuser'/>\n" +
                "      <http:auth-param name='password' value='defaultpwd'/>\n" +
                "    </http:auth>\n" +
                "  </url-data>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }

    @Test
    public void simpleUrlWithSidTest() throws XmlPullParserException, IOException, SmackParsingException {
        UrlDataElement urlDataElement = new UrlDataElement("http://pass.jabber.org:8519/test.txt", "a0");

        final String expectedXml = "<url-data xmlns='http://jabber.org/protocol/url-data'\n" +
                "      sid='a0'\n" +
                "      target='http://pass.jabber.org:8519/test.txt'/>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }

    @Test
    public void simpleUrlNoChildrenTest() throws XmlPullParserException, IOException, SmackParsingException {
        UrlDataElement urlDataElement = new UrlDataElement("http://festhall.outer-planes.net/d20M/announce/latest/", null);

        final String expectedXml = "<url-data\n" +
                "      xmlns='http://jabber.org/protocol/url-data'\n" +
                "      target='http://festhall.outer-planes.net/d20M/announce/latest/'/>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }

    @Test
    public void simpleCookieTest() throws XmlPullParserException, IOException, SmackParsingException {
        UrlDataElement urlDataElement = new UrlDataElement("http://www.jabber.org/members/index.php",
                null,
                null,
                Collections.singletonList(new CookieElement("jsessionid", "1243asd234190sa32ds")),
                null);

        final String expectedXml = "" +
                "<url-data xmlns='http://jabber.org/protocol/url-data'\n" +
                "    xmlns:http='http://jabber.org/protocol/url-data/scheme/http'\n" +
                "    target='http://www.jabber.org/members/index.php'>\n" +
                "  <http:cookie name='jsessionid' value='1243asd234190sa32ds'/>\n" +
                "</url-data>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }

    @Test
    public void additionalParametersCookieTest() throws XmlPullParserException, IOException, SmackParsingException {
        UrlDataElement urlDataElement = new UrlDataElement("http://www.jabber.org/members/index.php",
                null,
                null,
                Collections.singletonList(new CookieElement(
                        "jsessionid",
                        "1243asd234190sa32ds",
                        "jabber.org",
                        1234000,
                        "/members",
                        "Web Session Identifier",
                        "1.0",
                        false
                )),
                null);

        final String expectedXml = "<url-data xmlns='http://jabber.org/protocol/url-data'\n" +
                "      xmlns:http='http://jabber.org/protocol/url-data/scheme/http'\n" +
                "      target='http://www.jabber.org/members/index.php'>\n" +
                "  <http:cookie name='jsessionid'\n" +
                "        domain='jabber.org'\n" +
                "        max-age='1234000'\n" +
                "        path='/members'\n" +
                "        comment='Web Session Identifier'\n" +
                "        version='1.0'\n" +
                "        secure='false'\n" +
                "        value='1243asd234190sa32ds'/>\n" +
                "</url-data>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }

    @Test
    public void simpleHeaderTest() throws XmlPullParserException, IOException, SmackParsingException {
        UrlDataElement urlDataElement = new UrlDataElement(
                "http://www.jabber.org/members/index.php",
                null,
                null,
                null,
                Collections.singletonList(new HeaderElement("Custom-Data", "some custom data")));

        final String expectedXml = "<url-data xmlns='http://jabber.org/protocol/url-data'\n" +
                "      xmlns:http='http://jabber.org/protocol/url-data/scheme/http'\n" +
                "      target='http://www.jabber.org/members/index.php'>\n" +
                "    <http:header name='Custom-Data' value='some custom data'/>\n" +
                "  </url-data>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }

    @Test
    public void multiChildTest() throws XmlPullParserException, IOException, SmackParsingException {
        UrlDataElement urlDataElement = new UrlDataElement(
                "https://blog.jabberhead.tk",
                null,
                Collections.singletonList(HttpAuthElement.basicAuth()),
                Arrays.asList(
                        new CookieElement("jsessionid", "somecookievalue"),
                        new CookieElement("come2darkSide", "weHaveCookies")),
                Arrays.asList(
                        new HeaderElement("Accept", "text/plain"),
                        new HeaderElement("Access-Control-Allow-Origin", "*")));

        final String expectedXml =
                "<url-data xmlns='http://jabber.org/protocol/url-data'\n" +
                        "      xmlns:http='http://jabber.org/protocol/url-data/scheme/http'\n" +
                        "      target='https://blog.jabberhead.tk'>\n" +
                        "    <http:auth scheme='basic'/>\n" +
                        "    <http:cookie name='jsessionid' value='somecookievalue'/>\n" +
                        "    <http:cookie name='come2darkSide' value='weHaveCookies'/>\n" +
                        "    <http:header name='Accept' value='text/plain'/>\n" +
                        "    <http:header name='Access-Control-Allow-Origin' value='*'/>\n" +
                        "  </url-data>";
        assertXmlSimilar(expectedXml, urlDataElement.toXML().toString());

        UrlDataElement parsed = UrlDataElementProvider.INSTANCE.parse(TestUtils.getParser(expectedXml));
        assertEquals(urlDataElement, parsed);
    }
}
