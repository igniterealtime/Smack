package org.jivesoftware.smackx.bob;

import static org.junit.Assert.assertNotNull;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.junit.Test;

public class BoBExtenstionTest extends SmackTestSuite {

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
    public void checkBoBExtensionIM() throws Exception {
        BoBExtension boBExtension = TestUtils.parseExtensionElement(sampleBoBExtensionIM);
        assertNotNull(boBExtension);
    }
}
