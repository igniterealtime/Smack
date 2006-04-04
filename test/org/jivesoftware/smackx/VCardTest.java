package org.jivesoftware.smackx;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.VCard;
import org.jivesoftware.smackx.provider.VCardProvider;

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
        origVCard.setEmailWork("foo@fee.www.bar");

        origVCard.setJabberId("jabber@id.org");
        origVCard.setOrganization("Jetbrains, s.r.o");
        origVCard.setNickName("KIR");

        origVCard.setField("TITLE", "Mr");
        origVCard.setAddressFieldHome("STREET", "Some street & House");
        origVCard.setAddressFieldWork("STREET", "Some street work");

        origVCard.setPhoneWork("FAX", "3443233");
        origVCard.setPhoneHome("VOICE", "3443233");

        origVCard.save(getConnection(0));

        VCard loaded = new VCard();
        try {
            loaded.load(getConnection(0));
        } catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertEquals("Should load own VCard successfully", origVCard.toString(), loaded.toString());

        loaded = new VCard();
        try {
            loaded.load(getConnection(1), getBareJID(0));
        } catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }

        assertEquals("Should load another user's VCard successfully", origVCard.toString(), loaded.toString());
    }

    public void testNoWorkHomeSpecifier_EMAIL() throws Throwable {
        VCard card = VCardProvider._createVCardFromXml("<vcard><EMAIL><USERID>foo@fee.www.bar</USERID></EMAIL></vcard>");
        assertEquals("foo@fee.www.bar", card.getEmailWork());
    }

    public void testNoWorkHomeSpecifier_TEL() throws Throwable {
        VCard card = VCardProvider._createVCardFromXml("<vcard><TEL><FAX/><NUMBER>3443233</NUMBER></TEL></vcard>");
        assertEquals("3443233", card.getPhoneWork("FAX"));
    }

    public void testNoWorkHomeSpecifier_ADDR() throws Throwable {
        VCard card = VCardProvider._createVCardFromXml("<vcard><ADR><STREET>Some street</STREET><FF>ddss</FF></ADR></vcard>");
        assertEquals("Some street", card.getAddressFieldWork("STREET"));
        assertEquals("ddss", card.getAddressFieldWork("FF"));
    }

    public void testFN() throws Throwable {
        VCard card = VCardProvider._createVCardFromXml("<vcard><FN>kir max</FN></vcard>");
        assertEquals("kir max", card.getField("FN"));
       // assertEquals("kir max", card.getFullName());
    }

    /*
    public void testFullName() throws Throwable {
        VCard card = new VCard();
        card.setFirstName("kir");
       // assertEquals("kir", card.getFullName());

        card.setLastName("maximov");
      //  assertEquals("kir maximov", card.getFullName());

        card.setField("FN", "some name");
       // assertEquals("some name", card.getFullName());
    }
    */

    protected int getMaxConnections() {
        return 2;
    }
}
