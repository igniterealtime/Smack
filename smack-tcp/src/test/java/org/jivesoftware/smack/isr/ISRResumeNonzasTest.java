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

import org.jivesoftware.smack.isr.element.InstantStreamResumption.InstResume;
import org.jivesoftware.smack.isr.element.InstantStreamResumption.InstResumed;
import org.jivesoftware.smack.isr.provider.ParseInstantStreamResumption;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class ISRResumeNonzasTest {

    private static final String resumeNonza = "<inst-resume " + "xmlns='urn:xmpp:isr:0' " + "previd='some-long-sm-id' "
            + "h='42'>" + "<hmac>" + "<hash xmlns='urn:xmpp:hashes:1' algo='sha-256'>" + "initator-hmac" + "</hash>"
            + "</hmac>" + "</inst-resume>";

    private static final String resumedNonza = "<inst-resumed " + "xmlns='urn:xmpp:isr:0' "
            + "key='006b1a29-c549-41c7-a12c-2a931822f8c0' " + "h='21'>" + "<hmac>"
            + "<hash xmlns='urn:xmpp:hashes:1' algo='sha-256'>" + "responder-hmac" + "</hash>" + "</hmac>"
            + "</inst-resumed>";

    @Test
    public void checkResumeNonza() throws Exception {
        InstResume instResume = new InstResume("some-long-sm-id", 42, "initator-hmac", "sha-256");
        Assert.assertEquals(resumeNonza, instResume.toXML().toString());
    }

    @Test
    public void checkResumedNonza() throws Exception {
        InstResumed instResumed = new InstResumed("006b1a29-c549-41c7-a12c-2a931822f8c0", 21, "responder-hmac",
                "sha-256");
        Assert.assertEquals(resumedNonza, instResumed.toXML().toString());
    }

    @Test
    public void checkParseResumedNonza() throws Exception {
        XmlPullParser xmlPullParser = PacketParserUtils.getParserFor(resumedNonza);
        InstResumed instResumed = ParseInstantStreamResumption.resumed(xmlPullParser);
        Assert.assertEquals(resumedNonza, instResumed.toXML().toString());
    }

}
