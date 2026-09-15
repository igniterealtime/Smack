/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.sasl.ht;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.callback.CallbackHandler;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.SaslTokenMechanism;
import org.jivesoftware.smack.util.ByteUtils;
import org.jivesoftware.smack.util.TLSUtils;

public abstract class SaslHtMechanism extends SASLMechanism implements SaslTokenMechanism {

    public enum HashAlgorithm {
        SHA_256("SHA-256", "HmacSHA256"),
        SHA_512("SHA-512", "HmacSHA512"),
        SHA3_256("SHA3-256", "HmacSHA3-256"),
        SHA3_512("SHA3-512", "HmacSHA3-512");

        private final String ianaName;
        private final String hmacAlgorithm;

        HashAlgorithm(String ianaName, String hmacAlgorithm) {
            this.ianaName = ianaName;
            this.hmacAlgorithm = hmacAlgorithm;
        }

        public String getIanaName() {
            return ianaName;
        }

        public String getHmacAlgorithm() {
            return hmacAlgorithm;
        }
    }

    public enum ChannelBindingType {
        NONE("NONE"),
        ENDP("ENDP");

        private final String suffix;

        ChannelBindingType(String suffix) {
            this.suffix = suffix;
        }

        public String getSuffix() {
            return suffix;
        }
    }

    private static final byte[] INITIATOR_PREFIX = "Initiator".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] RESPONDER_PREFIX = "Responder".getBytes(StandardCharsets.US_ASCII);

    private enum State {
        INITIAL,
        AUTH_SENT,
        VALID_SERVER_RESPONSE,
    }

    private final HashAlgorithm hashAlgorithm;
    private final ChannelBindingType channelBindingType;
    private final int priority;

    private State state = State.INITIAL;
    private String token;

    protected SaslHtMechanism(HashAlgorithm hashAlgorithm, ChannelBindingType channelBindingType, int priority) {
        this.hashAlgorithm = Objects.requireNonNull(hashAlgorithm, "hashAlgorithm must not be null");
        this.channelBindingType = Objects.requireNonNull(channelBindingType, "channelBindingType must not be null");
        this.priority = priority;
    }

    @Override
    public String getName() {
        return "HT-" + hashAlgorithm.getIanaName() + "-" + channelBindingType.getSuffix();
    }

    @Override
    public int getPriority() {
        return priority;
    }

    @Override
    public boolean requiresPassword() {
        return false;
    }

    @Override
    public boolean authzidSupported() {
        return false;
    }

    public HashAlgorithm getHashAlgorithm() {
        return hashAlgorithm;
    }

    public ChannelBindingType getChannelBindingType() {
        return channelBindingType;
    }

    @Override
    public void setToken(String token) {
        this.token = token;
    }

    @Override
    public String getToken() {
        return token;
    }

    public String getChannelBindingNotSupportedReason(XMPPConnection connection) {
        switch (channelBindingType) {
        case NONE:
            return null;
        case ENDP:
            if (connection != null && connection.isConnected() && !connection.isSecureConnection()) {
                return "channel binding type 'tls-server-end-point' (RFC 5929) requires a secure (TLS) connection";
            }
            return null;
        default:
            return "unsupported channel binding type: " + channelBindingType;
        }
    }

    public boolean isChannelBindingSupported(XMPPConnection connection) {
        return getChannelBindingNotSupportedReason(connection) == null;
    }

    @Override
    public SASLMechanism instanceForAuthentication(XMPPConnection connection, ConnectionConfiguration connectionConfiguration) {
        SaslHtMechanism htMechanism = (SaslHtMechanism) super.instanceForAuthentication(connection, connectionConfiguration);
        htMechanism.token = this.token;
        return htMechanism;
    }

    @Override
    protected void authenticateInternal(CallbackHandler cbh) {
        throw new UnsupportedOperationException("CallbackHandler is not supported for SASL-HT");
    }

    @Override
    protected byte[] getAuthenticationText() throws SmackSaslException {
        if (this.token == null) {
            throw new SmackSaslException("No token available for SASL-HT mechanism " + getName());
        }
        String tokenToUse = this.token;

        byte[] cbData = getChannelBindingData();
        byte[] tokenBytes = tokenToUse.getBytes(StandardCharsets.UTF_8);
        byte[] initiatorData = ByteUtils.concat(INITIATOR_PREFIX, cbData);
        byte[] initiatorHashedToken = computeHmac(tokenBytes, initiatorData);

        if (org.jivesoftware.smack.util.StringUtils.isNullOrEmpty(authenticationId)) {
            throw new SmackSaslException("No authenticationId (username) provided for SASL-HT mechanism " + getName());
        }
        byte[] authcidBytes = authenticationId.getBytes(StandardCharsets.UTF_8);
        state = State.AUTH_SENT;
        return ByteUtils.concat(authcidBytes, new byte[] { 0 }, initiatorHashedToken);
    }

    @Override
    protected byte[] evaluateChallenge(byte[] challenge) throws SmackSaslException {
        if (token == null) {
            throw new SmackSaslException("Token is missing during server challenge verification");
        }
        byte[] cbData = getChannelBindingData();
        byte[] tokenBytes = token.getBytes(StandardCharsets.UTF_8);
        byte[] responderData = ByteUtils.concat(RESPONDER_PREFIX, cbData);
        byte[] expectedResponderMsg = computeHmac(tokenBytes, responderData);

        if (!MessageDigest.isEqual(challenge, expectedResponderMsg)) {
            throw new SmackSaslException("SASL-HT mutual authentication failed: responder-msg mismatch");
        }
        state = State.VALID_SERVER_RESPONSE;
        return null;
    }

    @Override
    public void checkIfSuccessfulOrThrow() throws SmackSaslException {
        if (state != State.VALID_SERVER_RESPONSE) {
            throw new SmackSaslException("SASL-HT (" + getName() + ") is missing valid server response");
        }
    }

    protected byte[] getChannelBindingData() throws SmackSaslException {
        switch (channelBindingType) {
        case NONE:
            return new byte[0];
        case ENDP:
            try {
                return TLSUtils.getChannelBindingTlsServerEndPoint(sslSession);
            } catch (Exception e) {
                throw new SmackSaslException("Failed to obtain tls-server-end-point channel binding data", e);
            }
        default:
            throw new SmackSaslException("Unsupported channel binding type: " + channelBindingType);
        }
    }

    private byte[] computeHmac(byte[] key, byte[] data) throws SmackSaslException {
        try {
            Mac mac = Mac.getInstance(hashAlgorithm.getHmacAlgorithm());
            mac.init(new SecretKeySpec(key, hashAlgorithm.getHmacAlgorithm()));
            return mac.doFinal(data);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new SmackSaslException("Failed to calculate " + hashAlgorithm.getHmacAlgorithm() + " HMAC", e);
        }
    }
}
