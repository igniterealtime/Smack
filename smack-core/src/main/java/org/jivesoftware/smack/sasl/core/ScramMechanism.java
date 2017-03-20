/**
 *
 * Copyright 2014-2017 Florian Schmaus
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
package org.jivesoftware.smack.sasl.core;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.ByteUtils;
import org.jivesoftware.smack.util.SHA1;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jxmpp.util.cache.Cache;
import org.jxmpp.util.cache.LruCache;

public abstract class ScramMechanism extends SASLMechanism {

    private static final int RANDOM_ASCII_BYTE_COUNT = 32;
    private static final byte[] CLIENT_KEY_BYTES = toBytes("Client Key");
    private static final byte[] SERVER_KEY_BYTES = toBytes("Server Key");
    private static final byte[] ONE = new byte[] { 0, 0, 0, 1 };

    private static final ThreadLocal<SecureRandom> SECURE_RANDOM = new ThreadLocal<SecureRandom>() {
        @Override
        protected SecureRandom initialValue() {
            return new SecureRandom();
        }
    };

    private static final Cache<String, Keys> CACHE = new LruCache<String, Keys>(10);

    private final ScramHmac scramHmac;

    protected ScramMechanism(ScramHmac scramHmac) {
        this.scramHmac = scramHmac;
    }

    private enum State {
        INITIAL,
        AUTH_TEXT_SENT,
        RESPONSE_SENT,
        VALID_SERVER_RESPONSE,
    }

    /**
     * The state of the this instance of SASL SCRAM-SHA1 authentication.
     */
    private State state = State.INITIAL;

    /**
     * The client's random ASCII which is used as nonce
     */
    private String clientRandomAscii;

    private String clientFirstMessageBare;
    private byte[] serverSignature;

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        throw new UnsupportedOperationException("CallbackHandler not (yet) supported");
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        clientRandomAscii = getRandomAscii();
        String saslPrepedAuthcId = saslPrep(authenticationId);
        clientFirstMessageBare = "n=" + escape(saslPrepedAuthcId) + ",r=" + clientRandomAscii;
        String clientFirstMessage = getGS2Header() + clientFirstMessageBare;
        state = State.AUTH_TEXT_SENT;
        return toBytes(clientFirstMessage);
    }

    @Override
    public String getName() {
        String name = "SCRAM-" + scramHmac.getHmacName();
        return name;
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {
        if (state != State.VALID_SERVER_RESPONSE) {
            throw new SmackException("SCRAM-SHA1 is missing valid server response");
        }
    }

    @Override
    public boolean authzidSupported() {
        return true;
    }

    @Override
    protected byte[] evaluateChallenge(byte[] challenge) throws SmackException {
        String challengeString;
        try {
            // TODO: Where is it specified that this is an UTF-8 encoded string?
            challengeString = new String(challenge, StringUtils.UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }

        switch (state) {
        case AUTH_TEXT_SENT:
            final String serverFirstMessage = challengeString;
            Map<Character, String> attributes = parseAttributes(challengeString);

            // Handle server random ASCII (nonce)
            String rvalue = attributes.get('r');
            if (rvalue == null) {
                throw new SmackException("Server random ASCII is null");
            }
            if (rvalue.length() <= clientRandomAscii.length()) {
                throw new SmackException("Server random ASCII is shorter then client random ASCII");
            }
            String receivedClientRandomAscii = rvalue.substring(0, clientRandomAscii.length());
            if (!receivedClientRandomAscii.equals(clientRandomAscii)) {
                throw new SmackException("Received client random ASCII does not match client random ASCII");
            }

            // Handle iterations
            int iterations;
            String iterationsString = attributes.get('i');
            if (iterationsString == null) {
                throw new SmackException("Iterations attribute not set");
            }
            try {
                iterations = Integer.parseInt(iterationsString);
            }
            catch (NumberFormatException e) {
                throw new SmackException("Exception parsing iterations", e);
            }

            // Handle salt
            String salt = attributes.get('s');
            if (salt == null) {
                throw new SmackException("SALT not send");
            }

            // Parsing and error checking is done, we can now begin to calculate the values

            // First the client-final-message-without-proof
            String channelBinding = "c=" + Base64.encodeToString(getCBindInput());
            String clientFinalMessageWithoutProof = channelBinding + ",r=" + rvalue;

            // AuthMessage := client-first-message-bare + "," + server-first-message + "," +
            // client-final-message-without-proof
            byte[] authMessage = toBytes(clientFirstMessageBare + ',' + serverFirstMessage + ','
                            + clientFinalMessageWithoutProof);

            // RFC 5802 § 5.1 "Note that a client implementation MAY cache ClientKey&ServerKey … for later reauthentication …
            // as it is likely that the server is going to advertise the same salt value upon reauthentication."
            // Note that we also mangle the mechanism's name into the cache key, since the cache is used by multiple
            // mechanisms.
            final String cacheKey = password + ',' + salt + ',' + getName();
            byte[] serverKey, clientKey;
            Keys keys = CACHE.lookup(cacheKey);
            if (keys == null) {
                // SaltedPassword := Hi(Normalize(password), salt, i)
                byte[] saltedPassword = hi(saslPrep(password), Base64.decode(salt), iterations);

                // ServerKey := HMAC(SaltedPassword, "Server Key")
                serverKey = hmac(saltedPassword, SERVER_KEY_BYTES);

                // ClientKey := HMAC(SaltedPassword, "Client Key")
                clientKey = hmac(saltedPassword, CLIENT_KEY_BYTES);

                keys = new Keys(clientKey, serverKey);
                CACHE.put(cacheKey, keys);
            }
            else {
                serverKey = keys.serverKey;
                clientKey = keys.clientKey;
            }

            // ServerSignature := HMAC(ServerKey, AuthMessage)
            serverSignature = hmac(serverKey, authMessage);

            // StoredKey := H(ClientKey)
            byte[] storedKey = SHA1.bytes(clientKey);

            // ClientSignature := HMAC(StoredKey, AuthMessage)
            byte[] clientSignature = hmac(storedKey, authMessage);

            // ClientProof := ClientKey XOR ClientSignature
            byte[] clientProof = new byte[clientKey.length];
            for (int i = 0; i < clientProof.length; i++) {
                clientProof[i] = (byte) (clientKey[i] ^ clientSignature[i]);
            }

            String clientFinalMessage = clientFinalMessageWithoutProof + ",p=" + Base64.encodeToString(clientProof);
            state = State.RESPONSE_SENT;
            return toBytes(clientFinalMessage);
        case RESPONSE_SENT:
            String clientCalculatedServerFinalMessage = "v=" + Base64.encodeToString(serverSignature);
            if (!clientCalculatedServerFinalMessage.equals(challengeString)) {
                throw new SmackException("Server final message does not match calculated one");
            }
            state = State.VALID_SERVER_RESPONSE;
            break;
        default:
            throw new SmackException("Invalid state");
        }
        return null;
    }

    private final String getGS2Header() {
        String authzidPortion = "";
        if (authorizationId != null) {
            authzidPortion = "a=" + authorizationId;
        }

        String cbName = getChannelBindingName();
        assert(StringUtils.isNotEmpty(cbName));

        return cbName + ',' + authzidPortion + ",";
    }

    private final byte[] getCBindInput() throws SmackException {
        byte[] cbindData = getChannelBindingData();
        byte[] gs2Header = toBytes(getGS2Header());

        if (cbindData == null) {
            return gs2Header;
        }

        return ByteUtils.concact(gs2Header, cbindData);
    }

    protected String getChannelBindingName() {
        // Check if we are using TLS and if a "-PLUS" variant of this mechanism is enabled. Assuming that the "-PLUS"
        // variants always have precedence before the non-"-PLUS" variants this means that the server did not announce
        // the "-PLUS" variant, as otherwise we would have tried it.
        if (sslSession != null && connectionConfiguration.isEnabledSaslMechanism(getName() + "-PLUS")) {
            // Announce that we support Channel Binding, i.e., the '-PLUS' flavor of this SASL mechanism, but that we
            // believe the server does not.
            return "y";
        }
        return "n";
    }

    /**
     * 
     * @return the Channel Binding data.
     * @throws SmackException
     */
    protected byte[] getChannelBindingData() throws SmackException {
        return null;
    }

    private static Map<Character, String> parseAttributes(String string) throws SmackException {
        if (string.length() == 0) {
            return Collections.emptyMap();
        }

        String[] keyValuePairs = string.split(",");
        Map<Character, String> res = new HashMap<Character, String>(keyValuePairs.length, 1);
        for (String keyValuePair : keyValuePairs) {
            if (keyValuePair.length() < 3) {
                throw new SmackException("Invalid Key-Value pair: " + keyValuePair);
            }
            char key = keyValuePair.charAt(0);
            if (keyValuePair.charAt(1) != '=') {
                throw new SmackException("Invalid Key-Value pair: " + keyValuePair);
            }
            String value = keyValuePair.substring(2);
            res.put(key, value);
        }

        return res;
    }

    /**
     * Generate random ASCII.
     * <p>
     * This method is non-static and package-private for unit testing purposes.
     * </p>
     * @return A String of 32 random printable ASCII characters.
     */
    String getRandomAscii() {
        int count = 0;
        char[] randomAscii = new char[RANDOM_ASCII_BYTE_COUNT];
        final Random random = SECURE_RANDOM.get();
        while (count < RANDOM_ASCII_BYTE_COUNT) {
            int r = random.nextInt(128);
            char c = (char) r;
            // RFC 5802 § 5.1 specifies 'r:' to exclude the ',' character and to be only printable ASCII characters
            if (!isPrintableNonCommaAsciiChar(c)) {
                continue;
            }
            randomAscii[count++] = c;
        }
        return new String(randomAscii);
    }

    private static boolean isPrintableNonCommaAsciiChar(char c) {
        if (c == ',') {
            return false;
        }
        // RFC 5802 § 7. 'printable': Contains all chars within 0x21 (33d) to 0x2b (43d) and 0x2d (45d) to 0x7e (126)
        // aka. "Printable ASCII except ','". Since we already filter the ASCII ',' (0x2c, 44d) above, we only have to
        // ensure that c is within [33, 126].
        return c > 32 && c < 127;
    }

    /**
     * Escapes usernames or passwords for SASL SCRAM-SHA1.
     * <p>
     * According to RFC 5802 § 5.1 'n:'
     * "The characters ',' or '=' in usernames are sent as '=2C' and '=3D' respectively."
     * </p>
     *
     * @param string
     * @return the escaped string
     */
    private static String escape(String string) {
        StringBuilder sb = new StringBuilder((int) (string.length() * 1.1));
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            switch (c) {
            case ',':
                sb.append("=2C");
                break;
            case '=':
                sb.append("=3D");
                break;
            default:
                sb.append(c);
                break;
            }
        }
        return sb.toString();
    }

    /**
     * RFC 5802 § 2.2 HMAC(key, str)
     * 
     * @param key
     * @param str
     * @return the HMAC-SHA1 value of the input.
     * @throws SmackException 
     */
    private byte[] hmac(byte[] key, byte[] str) throws SmackException {
        try {
            return scramHmac.hmac(key, str);
        }
        catch (InvalidKeyException e) {
            throw new SmackException(getName() + " Exception", e);
        }
    }

    /**
     * RFC 5802 § 2.2 Hi(str, salt, i)
     * <p>
     * Hi() is, essentially, PBKDF2 [RFC2898] with HMAC() as the pseudorandom function
     * (PRF) and with dkLen == output length of HMAC() == output length of H().
     * </p>
     * 
     * @param normalizedPassword the normalized password.
     * @param salt
     * @param iterations
     * @return the result of the Hi function.
     * @throws SmackException 
     */
    private byte[] hi(String normalizedPassword, byte[] salt, int iterations) throws SmackException {
        byte[] key;
        try {
            // According to RFC 5802 § 2.2, the resulting string of the normalization is also in UTF-8.
            key = normalizedPassword.getBytes(StringUtils.UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError();
        }
        // U1 := HMAC(str, salt + INT(1))
        byte[] u = hmac(key, ByteUtils.concact(salt, ONE));
        byte[] res = u.clone();
        for (int i = 1; i < iterations; i++) {
            u = hmac(key, u);
            for (int j = 0; j < u.length; j++) {
                res[j] ^= u[j];
            }
        }
        return res;
    }

    private static class Keys {
        private final byte[] clientKey;
        private final byte[] serverKey;

        public Keys(byte[] clientKey, byte[] serverKey) {
            this.clientKey = clientKey;
            this.serverKey = serverKey;
        }
    }
}
