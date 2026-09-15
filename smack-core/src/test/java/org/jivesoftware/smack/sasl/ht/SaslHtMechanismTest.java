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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.charset.StandardCharsets;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.util.ByteUtils;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class SaslHtMechanismTest {

    @Test
    public void testSha256NoneAuthenticationText() throws Exception {
        HtSha256NoneMechanism mechanism = new HtSha256NoneMechanism();
        mechanism.setToken("supersecrettoken");

        byte[] authText = mechanism.getInitialResponse("alice", "example.org", JidCreate.domainBareFrom("example.org"), null, null, null);

        // Expected format: "alice" + 0x00 + HMAC-SHA256("supersecrettoken", "Initiator")
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("supersecrettoken".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expectedHmac = mac.doFinal("Initiator".getBytes(StandardCharsets.US_ASCII));
        byte[] expected = ByteUtils.concat("alice".getBytes(StandardCharsets.UTF_8), new byte[] { 0 }, expectedHmac);

        assertArrayEquals(expected, authText);
    }

    @Test
    public void testSha256NoneEvaluateChallengeSuccess() throws Exception {
        HtSha256NoneMechanism mechanism = new HtSha256NoneMechanism();
        mechanism.setToken("supersecrettoken");

        mechanism.getInitialResponse("alice", "example.org", JidCreate.domainBareFrom("example.org"), null, null, null);

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("supersecrettoken".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] serverResponderMsg = mac.doFinal("Responder".getBytes(StandardCharsets.US_ASCII));

        mechanism.evaluateChallenge(serverResponderMsg);
        assertDoesNotThrow(() -> mechanism.checkIfSuccessfulOrThrow());
    }

    @Test
    public void testSha256NoneEvaluateChallengeFailure() throws Exception {
        HtSha256NoneMechanism mechanism = new HtSha256NoneMechanism();
        mechanism.setToken("supersecrettoken");

        mechanism.getInitialResponse("alice", "example.org", JidCreate.domainBareFrom("example.org"), null, null, null);

        byte[] invalidChallenge = "invalid-challenge-response".getBytes(StandardCharsets.UTF_8);

        assertThrows(SmackSaslException.class, () -> mechanism.evaluateChallenge(invalidChallenge));
    }

    @Test
    public void testMechanismPriorities() {
        assertEquals(50, new HtSha3_512EndpointMechanism().getPriority());
        assertEquals(55, new HtSha3_512NoneMechanism().getPriority());

        assertEquals(51, new HtSha512EndpointMechanism().getPriority());
        assertEquals(56, new HtSha512NoneMechanism().getPriority());

        assertEquals(60, new HtSha3_256EndpointMechanism().getPriority());
        assertEquals(65, new HtSha3_256NoneMechanism().getPriority());

        assertEquals(61, new HtSha256EndpointMechanism().getPriority());
        assertEquals(66, new HtSha256NoneMechanism().getPriority());
    }
}
