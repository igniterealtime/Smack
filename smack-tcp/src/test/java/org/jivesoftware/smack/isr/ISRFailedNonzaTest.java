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

import org.jivesoftware.smack.isr.element.InstantStreamResumption.Failed;
import org.jivesoftware.smack.isr.provider.ParseInstantStreamResumption;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class ISRFailedNonzaTest {

    private static final String failedNonza = "<failed xmlns='urn:xmpp:isr:0'/>";
    private static final String failedNonzaWithHandledCount = "<failed xmlns='urn:xmpp:isr:0' h='22'/>";

    @Test
    public void checkISRFailedNonza() throws Exception {
        Failed failed = new Failed();
        Assert.assertEquals(failedNonza, failed.toXML().toString());

        Failed failedWithHandledCount = new Failed(22);
        Assert.assertEquals(failedNonzaWithHandledCount, failedWithHandledCount.toXML().toString());
    }

    @Test
    public void checkParseISRFailedNonza() throws Exception {
        XmlPullParser xmlPullParser = PacketParserUtils.getParserFor(failedNonza);
        Failed failed = ParseInstantStreamResumption.failed(xmlPullParser);
        Assert.assertEquals(failedNonza, failed.toXML().toString());

        XmlPullParser xmlPullParser2 = PacketParserUtils.getParserFor(failedNonzaWithHandledCount);
        Failed failedWithHandledCount = ParseInstantStreamResumption.failed(xmlPullParser2);
        Assert.assertEquals(failedNonzaWithHandledCount, failedWithHandledCount.toXML().toString());
    }

}
