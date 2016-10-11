/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smack.isr;

import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class ISRUtilsTest {

    private static final String isrFailedNonza = "<failed xmlns='urn:xmpp:isr:0'/>";

    private static final String isrFailedNonzaWithHandledCount = "<failed xmlns='urn:xmpp:isr:0' h='22'/>";

    private static final String isrEnabledNonza = "<enabled xmlns='urn:xmpp:sm:3' xmlns:isr='urn:xmpp:isr:0' "
            + "isr:key='a0b9162d-0981-4c7d-9174-1f55aedd1f52'/>";

    private static final String isrEnabledNonzaWithLocation = "<enabled " + "xmlns='urn:xmpp:sm:3' "
            + "xmlns:isr='urn:xmpp:isr:0' " + "isr:key='a0b9162d-0981-4c7d-9174-1f55aedd1f52' "
            + "isr:location='isr.example.org:5222'/>";

    private static final String isrResumedNonza = "<inst-resumed " + "xmlns='urn:xmpp:isr:0' "
            + "key='006b1a29-c549-41c7-a12c-2a931822f8c0' " + "h='21'>" + "<hmac>"
            + "<hash xmlns='urn:xmpp:hashes:1' algo='sha256'>" + "responder-hmac" + "</hash>" + "</hmac>"
            + "</inst-resumed>";

    private static final String smFailedNonza = "<failed xmlns='urn:xmpp:sm:3'>"
            + "<unexpected-request xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" + "</failed>";

    private static final String smEnabledNonza = "<enabled xmlns='urn:xmpp:sm:3'/>";

    private static final String smEnabledNonzaResume = "<enabled xmlns='urn:xmpp:sm:3' "
            + "id='some-long-sm-id' resume='true'/>";

    private static final String smEnabledNonzaResumeWithLocation = "<enabled xmlns='urn:xmpp:sm:3' "
            + "id='some-long-sm-id' " + "location='[2001:41D0:1:A49b::1]:9222' " + "resume='true'/>";

    private static final String smResumedNonza = "<resumed xmlns='urn:xmpp:sm:3' " + "h='another-sequence-number' "
            + "previd='some-long-sm-id'/>";

    @Test
    public void checkIsISRNonzaValidation() throws Exception {
        XmlPullParser parserISRFailedNonza = PacketParserUtils.getParserFor(isrFailedNonza);
        Assert.assertTrue(ISRUtils.isISRNonza(parserISRFailedNonza));

        XmlPullParser parserISRFailedNonzaWithHandledCount = PacketParserUtils
                .getParserFor(isrFailedNonzaWithHandledCount);
        Assert.assertTrue(ISRUtils.isISRNonza(parserISRFailedNonzaWithHandledCount));

        XmlPullParser parserISREnabledNonza = PacketParserUtils.getParserFor(isrEnabledNonza);
        Assert.assertTrue(ISRUtils.isISRNonza(parserISREnabledNonza));

        XmlPullParser parserISREnabledNonzaWithLocation = PacketParserUtils.getParserFor(isrEnabledNonzaWithLocation);
        Assert.assertTrue(ISRUtils.isISRNonza(parserISREnabledNonzaWithLocation));

        XmlPullParser parserISRResumedNonza = PacketParserUtils.getParserFor(isrResumedNonza);
        Assert.assertTrue(ISRUtils.isISRNonza(parserISRResumedNonza));

        XmlPullParser parserSMFailedNonza = PacketParserUtils.getParserFor(smFailedNonza);
        Assert.assertFalse(ISRUtils.isISRNonza(parserSMFailedNonza));

        XmlPullParser parserSMEnabledNonza = PacketParserUtils.getParserFor(smEnabledNonza);
        Assert.assertFalse(ISRUtils.isISRNonza(parserSMEnabledNonza));

        XmlPullParser parserSMEnabledNonzaResume = PacketParserUtils.getParserFor(smEnabledNonzaResume);
        Assert.assertFalse(ISRUtils.isISRNonza(parserSMEnabledNonzaResume));

        XmlPullParser parserSMEnabledNonzaResumeWithLocation = PacketParserUtils
                .getParserFor(smEnabledNonzaResumeWithLocation);
        Assert.assertFalse(ISRUtils.isISRNonza(parserSMEnabledNonzaResumeWithLocation));

        XmlPullParser parserSMResumedNonza = PacketParserUtils.getParserFor(smResumedNonza);
        Assert.assertFalse(ISRUtils.isISRNonza(parserSMResumedNonza));
    }

    @Test
    public void checkHMACDigest() throws Exception {
        String msg = "Initiator";
        String token = "006b1a29-c549-41c7-a12c-2a931822f8c0";

        String sha256DigestResult = "e5e6241898f631342111958a51b61a1d75d492c782c8620b4efd0d9f172b55ca";
        String sha1DigestResult = "583ff09b098c3f0bb3dbbc8cb0ef279755db7cde";
        String md5DigestResult = "14b8db2ba5cdb9088e92a640485207a3";

        String hmacDigest = HMAC.hmacDigest(msg, token, "SHA256");
        Assert.assertEquals(sha256DigestResult, hmacDigest);

        hmacDigest = HMAC.hmacDigest(msg, token, "sha256");
        Assert.assertEquals(sha256DigestResult, hmacDigest);

        hmacDigest = HMAC.hmacDigest(msg, token, "SHA1");
        Assert.assertEquals(sha1DigestResult, hmacDigest);

        hmacDigest = HMAC.hmacDigest(msg, token, "md5");
        Assert.assertEquals(md5DigestResult, hmacDigest);

        hmacDigest = HMAC.hmacDigest(msg, token, "hMACmd5");
        Assert.assertEquals(md5DigestResult, hmacDigest);

    }

}
