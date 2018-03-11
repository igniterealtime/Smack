/**
 *
 * Copyright 2018
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
package org.jivesoftware.smackx.bob;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.bob.provider.BoBExtensionProvider;

import org.junit.Test;

import org.xmlpull.v1.XmlPullParser;

public class BoBExtensionProviderTest extends SmackTestSuite {

    private static final String sampleBoBExtensionIM = "<message from='ladymacbeth@shakespeare.lit/castle' "
            + "to='macbeth@chat.shakespeare.lit' type='groupchat'>"
            + "<body>Yet here's a spot.</body>"
            + "<html xmlns='http://jabber.org/protocol/xhtml-im'>"
            + "<body xmlns='http://www.w3.org/1999/xhtml'>"
            + "<p>Yet here's a spot."
            + "<img alt='A spot'"
            + "src='cid:sha1+8f35fef110ffc5df08d579a50083ff9308fb6242@bob.xmpp.org'/>"
            + "</p></body></html></message>";

    @Test
    public void parseTest() throws Exception {
        XmlPullParser parser = TestUtils.getParser(sampleBoBExtensionIM);
        BoBExtension boBExtension = new BoBExtensionProvider().parse(parser);

        assertEquals("Testing alt attribute", "A spot", boBExtension.getAlt());
        assertEquals("Testing BoBHash cid",
                "cid:sha1+8f35fef110ffc5df08d579a50083ff9308fb6242@bob.xmpp.org",
                boBExtension.getBoBHash().toSrc());
    }
}
