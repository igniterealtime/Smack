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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.hoxt.packet.AbstractHttpOverXmpp;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppReq;
import org.jivesoftware.smackx.hoxt.packet.HttpOverXmppResp;
import org.jivesoftware.smackx.shim.packet.Header;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

        AbstractHttpOverXmppProvider provider = new HttpOverXmppRespProvider();
        XmlPullParser parser = TestUtils.getParser(string, "resp");

        IQ iq = provider.parseIQ(parser);
        assertTrue(iq instanceof HttpOverXmppResp);
        AbstractHttpOverXmpp.AbstractBody body = ((HttpOverXmppResp) iq).getResp();

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

        AbstractHttpOverXmppProvider provider = new HttpOverXmppReqProvider();
        XmlPullParser parser = TestUtils.getParser(string, "req");

        IQ iq = provider.parseIQ(parser);
        assertTrue(iq instanceof HttpOverXmppReq);
        AbstractHttpOverXmpp.AbstractBody body = ((HttpOverXmppReq) iq).getReq();

        checkHeaders(body.getHeaders(), expectedHeaders);
    }

    @Test
    public void isTextDataParsedCorrectly() throws Exception {
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
                string, "resp").getData().getChild();
        assertEquals(expectedText, text.getText());
    }

    @Test
    public void isXmlDataParsedCorrectly() throws Exception {
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
                string, "resp").getData().getChild();
        assertEquals(expectedXml, xmlProviderValue.getText());
    }

    @Test
    public void isBase64DataParsedCorrectly() throws Exception {
        String base64Data = "iVBORw0KGgoAAAANSUhEUgAAASwAAAGQCAYAAAAUdV17AAAAAXNSR0 ... tVWJd+e+y1AAAAABJRU5ErkJggg==";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data><base64>"
                + base64Data
                + "</base64></data></resp>";
        AbstractHttpOverXmpp.Base64 base64ProviderValue = (AbstractHttpOverXmpp.Base64) parseAbstractBody(
                string, "resp").getData().getChild();
        assertEquals(base64Data, base64ProviderValue.getText());
    }

    @Test
    public void isChunkedBase64DataParsedCorrectly() throws Exception {
        String streamId = "Stream0001";
        String chunkBase64Data = "  <chunkedBase64 streamId='" + streamId + "'/>";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data>"
                + chunkBase64Data
                + "</data></resp>";
        AbstractHttpOverXmpp.ChunkedBase64 chunkedBase64Value = (AbstractHttpOverXmpp.ChunkedBase64) parseAbstractBody(
                string, "resp").getData().getChild();
        assertEquals(streamId, chunkedBase64Value.getStreamId());
    }

    @Test
    public void isIbbDataParsedCorrectly() throws Exception {
        String sid = "Stream0002";
        String ibbData = "  <ibb sid='" + sid + "'/>";
        String string = "<resp xmlns='urn:xmpp:http' version='1.1' statusCode='200' statusMessage='OK'>"
                + "<headers xmlns='http://jabber.org/protocol/shim'><header name='Server'>Clayster</header></headers>"
                + "<data>"
                + ibbData
                + "</data></resp>";
        AbstractHttpOverXmpp.Ibb ibbValue = (AbstractHttpOverXmpp.Ibb) parseAbstractBody(
                string, "resp").getData().getChild();
        assertEquals(sid, ibbValue.getSid());
    }

    private AbstractHttpOverXmpp.AbstractBody parseAbstractBody(String string, String tag) throws Exception {
        AbstractHttpOverXmppProvider provider = new HttpOverXmppRespProvider();
        XmlPullParser parser = TestUtils.getParser(string, tag);

        IQ iq = provider.parseIQ(parser);
        assertTrue(iq instanceof HttpOverXmppResp);
        AbstractHttpOverXmpp.AbstractBody body = ((HttpOverXmppResp) iq).getResp();
        return body;
    }

    private void checkHeaders(HeadersExtension headers, Map<String, String> expectedHeaders) {
        Collection<Header> collection = headers.getHeaders();

        assertEquals(collection.size(), expectedHeaders.size());

        for (Header header : collection) {
            assertTrue(expectedHeaders.containsKey(header.getName()));
            assertEquals(expectedHeaders.get(header.getName()), header.getValue());
        }
    }
}
