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

import java.io.IOException;
import java.math.BigInteger;
import java.security.NoSuchAlgorithmException;

import javax.security.auth.callback.CallbackHandler;

import org.ietf.jgss.*;
import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.sasl.SASLMechanism;

import org.jivesoftware.smack.util.SHA1;
import org.jivesoftware.smack.util.stringencoder.Base32;

import org.bouncycastle.asn1.ASN1ObjectIdentifier;

/**
 * Use of GSS-API mechanism in SASL by defining a new SASL mechanism family called GS2.
 * This mechanism offers a number of improvements over the previous "SASL/GSSAPI" mechanism.
 * In general, it uses fewer messages for authentication phase in some cases, and supports negotiable use of channel binding.
 * Only GSS-API mechanisms that support channel binding and mutual authentication are supported.
 * <br>
 * The absence of `PLUS` suffix in the name `GSS-API` suggests that the server doesn't support channel binding.
 * <br>
 * SASL implementations can use the GSS_Inquire_SASLname_for_mech call
 * to query for the SASL mechanism name of a GSS-API mechanism.
 * <br>
 * If the GSS_Inquire_SASLname_for_mech interface is not used, the GS2
 * implementation needs some other mechanism to map mechanism Object
 * Identifiers (OIDs) to SASL names internally.
 * <br>
 * In this case, the implementation can only support the mechanisms for which it knows the
 * SASL name.
 *
 * @author adiaholic
 */
public class GssApiMechanism extends SASLMechanism{

    public static final String NAME = "GSS-API";

    public static final String GS2_PREFIX = "GS2-";

    private Oid objectIdentifiers = null;

    private GssApiManager gssApiManager = null;

    GssApiMechanism(Oid oid, XMPPConnection connection) {
        super();
        this.connection = connection;
        gssApiManager = GssApiManager.getInstanceFor(connection);
        setObjectIdentifier(oid);
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackSaslException {
        try {
            return gssApiManager.authenticate();
        } catch (GSSException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected byte[] evaluateChallenge(byte[] challenge) throws SmackSaslException {
        return gssApiManager.acceptServerToken(challenge);
    }

    /*
     * The SASL Mechanism name is concatenation of the string "GS2-" and the
     * Base32 encoding of the first 55 bits of the binary SHA-1 hash string
     * computed over the ASN.1 DER encoding
     * <br>
     * Note : Some older GSS-API mechanisms were not specified with a SASL GS2 mechanism name.
     */
    public String generateSASLMechanismNameFromGSSApiOIDs (String objectIdentifier) throws NoSuchAlgorithmException, IOException {

        // To generate a SHA-1 hash string over ASN1.DER.
        byte[] ASN1_DER = getASN1DERencoding(objectIdentifier);
        String sha1_hash = SHA1.hex(ASN1_DER);

        // Obtain first 55 bits of the SHA-1 hash.
        String binary55Bits = getbinary55Bits(sha1_hash);

        // Make groups of 5 bits each
        String[] binaryGroups = new String[11];
        for (int i = 0 ; i < binary55Bits.length() / 5 ; i++) {
            String binaryGroup = "";
            for (int j = 0 ; j < 5 ; j++) {
                binaryGroup += binary55Bits.charAt(5 * i + j);
            }
            // int decimalForGroup = Integer.parseInt(binaryGroup,2);

            binaryGroups[i] = binaryGroup;
        }

        // Base32 encoding for the above binaryGroups
        String base32Encoding = "";
        for (int i = 0 ; i < 11 ; i++) {
            int decimalValue = Integer.parseInt(binaryGroups[i], 2);
            base32Encoding += Base32.encodeIntValue(decimalValue);
        }
        return GS2_PREFIX + base32Encoding;
    }

    private static String getbinary55Bits(String sha1_hash) {
        // Get first 7 octets.
        String first7Octets = sha1_hash.substring(0, 14);

        // Convert first 7 octets of the sha1 hash into binary.
        String binaryOctets = new BigInteger(first7Octets, 16).toString(2);

        // Return first 55 bits of the binary hash.
        return binaryOctets.substring(0, 55);
    }

    public byte[] getASN1DERencoding(String objectIdentifier) throws IOException {
        ASN1ObjectIdentifier asn1ObjectIdentifier = new ASN1ObjectIdentifier(objectIdentifier).intern();
        byte[] encoded = asn1ObjectIdentifier.getEncoded();
        return encoded;
    }

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackSaslException {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    protected void checkIfSuccessfulOrThrow() throws SmackSaslException {
    }

    @Override
    protected GssApiMechanism newInstance() {
        return new GssApiMechanism(null,null);
    }

    public void setObjectIdentifier(Oid oid) {
        this.objectIdentifiers = oid;
    }

}
