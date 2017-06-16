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

import org.jivesoftware.smack.isr.element.InstantStreamResumption.Enabled;
import org.jivesoftware.smack.isr.provider.ParseInstantStreamResumption;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.junit.Assert;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;

public class ISREnabledNonzaTest {

    private static final String enabledNonza = "<enabled xmlns='urn:xmpp:sm:3' xmlns:isr='urn:xmpp:isr:0' "
            + "isr:key='a0b9162d-0981-4c7d-9174-1f55aedd1f52'/>";

    private static final String enabledNonzaWithLocation = "<enabled " + "xmlns='urn:xmpp:sm:3' "
            + "xmlns:isr='urn:xmpp:isr:0' " + "isr:key='a0b9162d-0981-4c7d-9174-1f55aedd1f52' "
            + "isr:location='isr.example.org:5222'/>";

    @Test
    public void checkISREnabledNonza() throws Exception {
        Enabled enabled = new Enabled("a0b9162d-0981-4c7d-9174-1f55aedd1f52");
        Assert.assertEquals(enabledNonza, enabled.toXML().toString());

        Enabled enabledWithLocation = new Enabled("a0b9162d-0981-4c7d-9174-1f55aedd1f52", "isr.example.org:5222");
        Assert.assertEquals(enabledNonzaWithLocation, enabledWithLocation.toXML().toString());
    }

    @Test
    public void checkParseISREnabledNonza() throws Exception {
        XmlPullParser xmlPullParser = PacketParserUtils.getParserFor(enabledNonza);
        Enabled enabled = ParseInstantStreamResumption.enabled(xmlPullParser);
        Assert.assertEquals(enabledNonza, enabled.toXML().toString());

        XmlPullParser xmlPullParser2 = PacketParserUtils.getParserFor(enabledNonzaWithLocation);
        Enabled enabledWithLocation = ParseInstantStreamResumption.enabled(xmlPullParser2);
        Assert.assertEquals(enabledNonzaWithLocation, enabledWithLocation.toXML().toString());
    }

}
