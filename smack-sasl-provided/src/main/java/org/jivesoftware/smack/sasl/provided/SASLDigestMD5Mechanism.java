/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack.sasl.provided;

import java.io.UnsupportedEncodingException;

import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.ByteUtils;
import org.jivesoftware.smack.util.MD5;
import org.jivesoftware.smack.util.StringUtils;

public class SASLDigestMD5Mechanism extends SASLMechanism {

    public static final String NAME = DIGESTMD5;

    private static final String INITAL_NONCE = "00000001";

    /**
     * The only 'qop' value supported by this implementation
     */
    private static final String QOP_VALUE = "auth";

    private enum State {
        INITIAL,
        RESPONSE_SENT,
        VALID_SERVER_RESPONSE,
    }

    private static boolean verifyServerResponse = true;

    public static void setVerifyServerResponse(boolean verifyServerResponse) {
        SASLDigestMD5Mechanism.verifyServerResponse = verifyServerResponse;
    }

    /**
     * The state of the this instance of SASL DIGEST-MD5 authentication.
     */
    private State state = State.INITIAL;

    private String nonce;
    private String cnonce;
    private String digestUri;
    private String hex_hashed_a1;

    @Override
    protected void authenticateInternal(CallbackHandler cbh) throws SmackException {
        throw new UnsupportedOperationException("CallbackHandler not (yet) supported");
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackException {
        // DIGEST-MD5 has no initial response, return null
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public int getPriority() {
        return 210;
    }

    @Override
    public SASLDigestMD5Mechanism newInstance() {
        return new SASLDigestMD5Mechanism();
    }

    @Override
    public boolean authzidSupported() {
      return true;
    }


    @Override
    public void checkIfSuccessfulOrThrow() throws SmackException {
        if (verifyServerResponse && state != State.VALID_SERVER_RESPONSE) {
            throw new SmackException(NAME + " no valid server response");
        }
    }

    @Override
    protected byte[] evaluateChallenge(byte[] challenge) throws SmackException {
        if (challenge.length == 0) {
            throw new SmackException("Initial challenge has zero length");
        }
        String challengeString;
        try {
            challengeString = new String(challenge, StringUtils.UTF8);
        }
        catch (UnsupportedEncodingException e) {
            throw new AssertionError(e);
        }
        String[] challengeParts = challengeString.split(",");
        byte[] response = null;
        switch (state) {
        case INITIAL:
            for (String part : challengeParts) {
                String[] keyValue = part.split("=");
                assert (keyValue.length == 2);
                String key = keyValue[0];
                // RFC 2831 § 7.1 about the formating of the digest-challenge:
                // "The full form is "<n>#<m>element" indicating at least <n> and
                // at most <m> elements, each separated by one or more commas
                // (",") and OPTIONAL linear white space (LWS)."
                // Which means the key value may be preceded by whitespace,
                // which is what we remove: *Only the preceding whitespace*.
                key = key.replaceFirst("^\\s+", "");
                String value = keyValue[1];
                if ("nonce".equals(key)) {
                    if (nonce != null) {
                        throw new SmackException("Nonce value present multiple times");
                    }
                    nonce = value.replace("\"", "");
                }
                else if ("qop".equals(key)) {
                    value = value.replace("\"", "");
                    if (!value.equals("auth")) {
                        throw new SmackException("Unsupported qop operation: " + value);
                    }
                }
            }
            if (nonce == null) {
                // RFC 2831 2.1.1 about nonce "This directive is required and MUST appear exactly
                // once; if not present, or if multiple instances are present, the client should
                // abort the authentication exchange."
                throw new SmackException("nonce value not present in initial challenge");
            }
            // RFC 2831 2.1.2.1 defines A1, A2, KD and response-value
            byte[] a1FirstPart = MD5.bytes(authenticationId + ':' + serviceName + ':'
                            + password);
            cnonce = StringUtils.randomString(32);
            byte[] a1 = ByteUtils.concact(a1FirstPart, toBytes(':' + nonce + ':' + cnonce));
            digestUri = "xmpp/" + serviceName;
            hex_hashed_a1 = StringUtils.encodeHex(MD5.bytes(a1));
            String responseValue = calcResponse(DigestType.ClientResponse);
            // @formatter:off
            // See RFC 2831 2.1.2 digest-response
            String authzid;
            if (authorizationId == null) {
              authzid = "";
            } else {
              authzid = ",authzid=\"" + authorizationId + '"';
            }
            String saslString = "username=\"" + quoteBackslash(authenticationId) + '"'
                               + authzid
                               + ",realm=\"" + serviceName + '"'
                               + ",nonce=\"" + nonce + '"'
                               + ",cnonce=\"" + cnonce + '"'
                               + ",nc=" + INITAL_NONCE
                               + ",qop=auth"
                               + ",digest-uri=\"" + digestUri + '"'
                               + ",response=" + responseValue
                               + ",charset=utf-8";
            // @formatter:on
            response = toBytes(saslString);
            state = State.RESPONSE_SENT;
            break;
        case RESPONSE_SENT:
            if (verifyServerResponse) {
                String serverResponse = null;
                for (String part : challengeParts) {
                    String[] keyValue = part.split("=");
                    assert (keyValue.length == 2);
                    String key = keyValue[0];
                    String value = keyValue[1];
                    if ("rspauth".equals(key)) {
                        serverResponse = value;
                        break;
                    }
                }
                if (serverResponse == null) {
                    throw new SmackException("No server response received while performing " + NAME
                                    + " authentication");
                }
                String expectedServerResponse = calcResponse(DigestType.ServerResponse);
                if (!serverResponse.equals(expectedServerResponse)) {
                    throw new SmackException("Invalid server response  while performing " + NAME
                                    + " authentication");
                }
            }
            state = State.VALID_SERVER_RESPONSE;
            break;
        default:
            throw new IllegalStateException();
        }
        return response;
    }

    private enum DigestType {
        ClientResponse,
        ServerResponse
    }

    private String calcResponse(DigestType digestType) {
        StringBuilder a2 = new StringBuilder();
        if (digestType == DigestType.ClientResponse) {
            a2.append("AUTHENTICATE");
        }
        a2.append(':');
        a2.append(digestUri);
        String hex_hashed_a2 = StringUtils.encodeHex(MD5.bytes(a2.toString()));

        StringBuilder kd_argument = new StringBuilder();
        kd_argument.append(hex_hashed_a1);
        kd_argument.append(':');
        kd_argument.append(nonce);
        kd_argument.append(':');
        kd_argument.append(INITAL_NONCE);
        kd_argument.append(':');
        kd_argument.append(cnonce);
        kd_argument.append(':');
        kd_argument.append(QOP_VALUE);
        kd_argument.append(':');
        kd_argument.append(hex_hashed_a2);
        byte[] kd = MD5.bytes(kd_argument.toString());
        String responseValue = StringUtils.encodeHex(kd);
        return responseValue;
    }

    /**
     * Quote the backslash in the given String. Replaces all occurrences of "\" with "\\".
     * <p>
     * According to RFC 2831 § 7.2 a quoted-string consists either of qdtext or quoted-pair. And since quoted-pair is a
     * backslash followed by a char, every backslash in qdtext must be quoted, since it otherwise would be treated as
     * qdtext.
     * </p>
     *
     * @param string the input string.
     * @return the input string where the every backslash is quoted.
     */
    public static String quoteBackslash(String string) {
        return string.replace("\\", "\\\\");
    }
}
