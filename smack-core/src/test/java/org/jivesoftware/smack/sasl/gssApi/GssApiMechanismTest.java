/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.sasl.gssApi;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import org.ietf.jgss.GSSException;
import org.ietf.jgss.Oid;
import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;

public class GssApiMechanismTest extends SmackTestSuite {

    public static final String OID = "1.2.840.113554.1.2.2";
    public static final String ASN_1_DER_of_OID_in_HEX = "06092A864886F712010202";

    public static final String SHA1_of_ASN_1_DER_in_HEX = "82d27325766bd6c845aa9325516afcff04b04360";

    public static final String first_7_octets_to_binary_drop_last_bit = "1000001011010010011100110010010101110110011010111101011";
    public static final String binary_in_group_of_5 = "10000 01011 01001 00111 00110 01001 01011 10110 01101 01111 01011";
    public static final String decimal_of_each_group = "16 11 9 7 6 9 11 22 13 15 11";
    public static final String base32Encoding = "QLJHGJLWNPL";
    public static final String MECHANISM_NAME = "GS2-" + base32Encoding;

    @Test
    public void generateASN1DERTest() throws IOException {
        GssApiMechanism gssApiMechanism = new GssApiMechanism(null,null);
        byte[] asn1DERencoding = gssApiMechanism.getASN1DERencoding(OID);

        String asn1_der_of_oid_in_hex = "";

        for (byte b : asn1DERencoding) {
            asn1_der_of_oid_in_hex += String.format("%02X", b);
        }
        assertEquals(ASN_1_DER_of_OID_in_HEX, asn1_der_of_oid_in_hex);
    }

    @Test
    public void getSubsequentOctetsTest() throws GSSException {
        GssApiMechanism gssApiMechanism = new GssApiMechanism(new Oid(OID),null);
        // System.out.println("First 2 octets : " + gssApiMechanism.generateFirstOctet());
    }

    @Test
    public void generateMechanismName() throws IOException, NoSuchAlgorithmException, GSSException {
        GssApiMechanism gssApiMechanism = new GssApiMechanism(new Oid(OID),null);
        String mechanismName = gssApiMechanism.generateSASLMechanismNameFromGSSApiOIDs(OID);
        assertEquals(MECHANISM_NAME, mechanismName);
    }

    @Test
    public void getLevelObjectIdentiferCompoentTest() throws GSSException {
        GssApiMechanism gssApiMechanism = new GssApiMechanism(new Oid(OID),null);
        // String component = gssApiMechanism.getLevelObjectIdentiferComponent(3);
        // assertEquals("840",component);
    }

    @Test
    public void getSubsequentObjectIdentifierComponentTest() throws GSSException {
        GssApiMechanism gssApiMechanism = new GssApiMechanism(new Oid(OID),null);
        // String subsequentCompoents = gssApiMechanism.getSubsequentOctets();
        // System.out.println(subsequentCompoents + " : are the subsequent octets");
    }

    public static final String USERNAME = "user";
    public static final String PASSWORD = "pencil";

    @Test
    public void GssApiManagerTest() throws GSSException, SmackException.NotConnectedException, InterruptedException, SmackException.SmackSaslException {



    }
}
