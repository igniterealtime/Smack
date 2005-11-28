package org.jivesoftware.smackx;

import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.packet.VCard;

/**
 * Created by IntelliJ IDEA.
 * User: Gaston
 * Date: Jun 18, 2005
 * Time: 1:29:30 AM
 * To change this template use File | Settings | File Templates.
 */
public class VCardTest extends SmackTestCase {

    public VCardTest(String arg0) {
        super(arg0);
    }

    public void testBigFunctional() throws XMPPException {
        VCard origVCard = new VCard();

        origVCard.setFirstName("kir");
        origVCard.setLastName("max");
        origVCard.setEmailHome("foo@fee.bar");
        origVCard.setJabberId("jabber@id.org");
        origVCard.setOrganization("Jetbrains, s.r.o");
        origVCard.setNickName("KIR");

        origVCard.setField("TITLE", "Mr");
        origVCard.setAddressFieldHome("STREET", "Some street");
        origVCard.setPhoneWork("FAX", "3443233");

        origVCard.save(getConnection(0));

        VCard loaded = new VCard();
        try {
            loaded.load(getConnection(0));
        } catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertEquals("Should load own VCard successfully", origVCard, loaded);

        loaded = new VCard();
        try {
            loaded.load(getConnection(1), getBareJID(0));
        } catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertEquals("Should load another user's VCard successfully", origVCard, loaded);
    }

    protected int getMaxConnections() {
        return 2;
    }
}
