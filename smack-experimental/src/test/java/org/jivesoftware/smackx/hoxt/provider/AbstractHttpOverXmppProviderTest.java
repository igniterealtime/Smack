/**
 *
 * Copyright 2014 Andriy Tsykholyas
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
package org.jivesoftware.smackx.hoxt.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.SmackTestUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.hoxt.packet.AbstractHttpOverXmpp;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppReq;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppResp;
import org.jivesoftware.smackx.shim.packet.Header;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Tests correct headers and data parsing in 'req' and 'resp' elements.
 */
public class AbstractHttpOverXmppProviderTest {

    @Test
    public void areRespHeadersParsedCorrectly() throws Exception {
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'>"
                + "<header name='Date'>Fri, 03 May 2013 13:52:10 GMT-4</header>"
                + "<header name='Allow'>OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE</header>"
                + "<header name='Content-Length'>0</header>"
                + "</headers>"
                + "</resp>";
        Map<String, String> expectedHeaders = new HashMap<String, String>();
        expectedHeaders.put("Date", "Fri, 03 May 2013 13:52:10 GMT-4");
        expectedHeaders.put("Allow", "OPTIONS, GET, HEAD, POST, PUT, DELETE, TRACE");
        expectedHeaders.put("Content-Length", "0");

        HttpOverXmppRespProvider provider = new HttpOverXmppRespProvider();
        XmlPullParser parser = PacketParserUtils.getParserFor(string);

        IQ iq = provider.parse(parser);
        assertTrue(iq instanceof HttpOverXmppResp);
        HttpOverXmppResp body = (HttpOverXmppResp) iq;

        checkHeaders(body.getHeaders(), expectedHeaders);
    }

    @Test
    public void areReqHeadersParsedCorrectly() throws Exception {
        String string = "<req xmlns='urn:xmpp:http' method='GET' resource='/rdf/xep' version='1.1'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'>"
                + "<header name='Host'>clayster.com</header>"
                + "</headers>"
                + "</req>";
        Map<String, String> expectedHeaders = new HashMap<String, String>();
        expectedHeaders.put("Host", "clayster.com");

        HttpOverXmppReqProvider provider = new HttpOverXmppReqProvider();
        XmlPullParser parser = PacketParserUtils.getParserFor(string);

        IQ iq = provider.parse(parser);
        assertTrue(iq instanceof HttpOverXmppReq);
        HttpOverXmppReq body = (HttpOverXmppReq) iq;

        checkHeaders(body.getHeaders(), expectedHeaders);
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void isTextDataParsedCorrectly(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        String expectedText = "@prefix dc: <http://purl.org/dc/elements/1.1/>."
                + "@base <http://clayster.com/>."
                + "<xep> dc:title \"HTTP over XMPP\";"
                + "dc:creator <PeterWaher>;"
                + "dc:publisher <XSF>.";
        String encodedText = "@prefix dc: &lt;http://purl.org/dc/elements/1.1/&gt;."
                + "@base &lt;http://clayster.com/&gt;."
                + "&lt;xep&gt; dc:title \"HTTP over XMPP\";"
                + "dc:creator &lt;PeterWaher&gt;;"
                + "dc:publisher &lt;XSF&gt;.";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data><text>"
                + encodedText
                + "</text></data></resp>";

        AbstractHttpOverXmpp.Text text = (AbstractHttpOverXmpp.Text) parseAbstractBody(
                string, "resp", parserKind).getData().getChild();
        assertEquals(expectedText, text.getText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void isXmlDataParsedCorrectly(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        String expectedXml = "<sparql><head><variable name=\"title\"/><variable name=\"creator\"/>" // no xmlns here
                + "</head><results><result>"
                + "<binding name=\"title\">"
                + "<literal>HTTP over XMPP</literal>"
                + "</binding>"
                + "<binding name=\"creator\">"
                + "<uri>http://clayster.com/PeterWaher</uri>"
                + "</binding>"
                + "</result>"
                + "</results>"
                + "</sparql>";
        String encodedXml = "<sparql xmlns=\"http://www.w3.org/2005/sparql-results#\"><head><variable name=\"title\"/><variable name=\"creator\"/>"
                + "</head><results><result>"
                + "<binding name=\"title\">"
                + "<literal>HTTP over XMPP</literal>"
                + "</binding>"
                + "<binding name=\"creator\">"
                + "<uri>http://clayster.com/PeterWaher</uri>"
                + "</binding>"
                + "</result>"
                + "</results>"
                + "</sparql>";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data><xml>"
                + encodedXml
                + "</xml></data></resp>";
        AbstractHttpOverXmpp.Xml xmlProviderValue = (AbstractHttpOverXmpp.Xml) parseAbstractBody(
                string, "resp", parserKind).getData().getChild();
        assertEquals(expectedXml, xmlProviderValue.getText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void isBase64DataParsedCorrectly(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        String base64Data = "iVBORw0KGgoAAAANSUhEUgAAASwAAAGQCAYAAAAUdV17AAAAAXNSR0 ... tVWJd+e+y1AAAAABJRU5ErkJggg==";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data><base64>"
                + base64Data
                + "</base64></data></resp>";
        AbstractHttpOverXmpp.Base64 base64ProviderValue = (AbstractHttpOverXmpp.Base64) parseAbstractBody(
                string, "resp", parserKind).getData().getChild();
        assertEquals(base64Data, base64ProviderValue.getText());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void isChunkedBase64DataParsedCorrectly(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        String streamId = "Stream0001";
        String chunkBase64Data = "  <chunkedBase64 streamId='" + streamId + "'/>";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data>"
                + chunkBase64Data
                + "</data></resp>";
        AbstractHttpOverXmpp.ChunkedBase64 chunkedBase64Value = (AbstractHttpOverXmpp.ChunkedBase64) parseAbstractBody(
                string, "resp", parserKind).getData().getChild();
        assertEquals(streamId, chunkedBase64Value.getStreamId());
    }

    @ParameterizedTest
    @EnumSource(SmackTestUtil.XmlPullParserKind.class)
    public void isIbbDataParsedCorrectly(SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        String sid = "Stream0002";
        String ibbData = "  <ibb sid='" + sid + "'/>";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data>"
                + ibbData
                + "</data></resp>";
        AbstractHttpOverXmpp.Ibb ibbValue = (AbstractHttpOverXmpp.Ibb) parseAbstractBody(
                string, "resp", parserKind).getData().getChild();
        assertEquals(sid, ibbValue.getSid());
    }

    // TODO The method name makes no sense after the HOXT re-design, change to parseHttpOverXmppResp()
    private static HttpOverXmppResp parseAbstractBody(String string, String tag,
                    SmackTestUtil.XmlPullParserKind parserKind)
                    throws XmlPullParserException, IOException, SmackParsingException {
        HttpOverXmppRespProvider provider = new HttpOverXmppRespProvider();
        XmlPullParser parser = SmackTestUtil.getParserFor(string, tag, parserKind);

        IQ iq = provider.parse(parser);
        assertTrue(iq instanceof HttpOverXmppResp);
        return (HttpOverXmppResp) iq;
    }

    private static void checkHeaders(HeadersExtension headers, Map<String, String> expectedHeaders) {
        Collection<Header> collection = headers.getHeaders();

        assertEquals(collection.size(), expectedHeaders.size());

        for (Header header : collection) {
            assertTrue(expectedHeaders.containsKey(header.getName()));
            assertEquals(expectedHeaders.get(header.getName()), header.getValue());
        }
    }
}
