/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smack.tbr;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.tbr.element.TBRGetTokensIQ;
import org.jivesoftware.smack.tbr.element.TBRTokensIQ;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;

public class TBRGetTokensIQTest {

    String getTokensIQExample = "<iq to='alice@wonderland.com' id='123' type='get'>"
            + "<query xmlns='erlang-solutions.com:xmpp:token-auth:0'/>" + "</iq>";

    String tokensResponseExample = "<iq to='alice@wonderland.com/resource' from='alice@wonderland.com' id='123' type='result'>"
            + "<items xmlns='erlang-solutions.com:xmpp:token-auth:0'>"
            + "<access_token>YWNjZXNzAGFsaWNlQHdvbmRlcmxhbmQuY29tL01pY2hhbC1QaW90cm93c2tpcy1NYWNCb29rLVBybwA2MzYyMTg4Mzc2NAA4M2QwNzNiZjBkOGJlYzVjZmNkODgyY2ZlMzkyZWM5NGIzZjA4ODNlNDI4ZjQzYjc5MGYxOWViM2I2ZWJlNDc0ODc3MDkxZTIyN2RhOGMwYTk2ZTc5ODBhNjM5NjE1Zjk=</access_token>"
            + "<refresh_token>cmVmcmVzaABhbGljZUB3b25kZXJsYW5kLmNvbS9NaWNoYWwtUGlvdHJvd3NraXMtTWFjQm9vay1Qcm8ANjM2MjMwMDYxODQAMQAwZGQxOGJjODhkMGQ0N2MzNTBkYzAwYjcxZjMyZDVmOWIwOTljMmI1ODU5MmNhN2QxZGFmNWFkNGM0NDQ2ZGU2MWYxYzdhNTJjNDUyMGI5YmIxNGIxNTMwMTE4YTM1NTc=</refresh_token>"
            + "</items>" + "</iq>";

    @Test
    public void checkGetTokensIQ() throws Exception {
        TBRGetTokensIQ tbrGetTokensIQ = new TBRGetTokensIQ(JidCreate.from("alice@wonderland.com"));
        tbrGetTokensIQ.setStanzaId("123");

        Assert.assertEquals(getTokensIQExample, tbrGetTokensIQ.toXML().toString());
    }

    @Test
    public void checkTokensResponseParse() throws Exception {
        IQ iq = (IQ) PacketParserUtils.parseStanza(tokensResponseExample);
        TBRTokensIQ tokensIQ = (TBRTokensIQ) iq;
        tokensIQ.setStanzaId("123");

        Assert.assertEquals(
                "YWNjZXNzAGFsaWNlQHdvbmRlcmxhbmQuY29tL01pY2hhbC1QaW90cm93c2tpcy1NYWNCb29rLVBybwA2MzYyMTg4Mzc2NAA4M2QwNzNiZjBkOGJlYzVjZmNkODgyY2ZlMzkyZWM5NGIzZjA4ODNlNDI4ZjQzYjc5MGYxOWViM2I2ZWJlNDc0ODc3MDkxZTIyN2RhOGMwYTk2ZTc5ODBhNjM5NjE1Zjk=",
                tokensIQ.getAccessToken());
        Assert.assertEquals(
                "cmVmcmVzaABhbGljZUB3b25kZXJsYW5kLmNvbS9NaWNoYWwtUGlvdHJvd3NraXMtTWFjQm9vay1Qcm8ANjM2MjMwMDYxODQAMQAwZGQxOGJjODhkMGQ0N2MzNTBkYzAwYjcxZjMyZDVmOWIwOTljMmI1ODU5MmNhN2QxZGFmNWFkNGM0NDQ2ZGU2MWYxYzdhNTJjNDUyMGI5YmIxNGIxNTMwMTE4YTM1NTc=",
                tokensIQ.getRefreshToken());
        Assert.assertEquals(tokensResponseExample, tokensIQ.toXML().toString());
    }

}
