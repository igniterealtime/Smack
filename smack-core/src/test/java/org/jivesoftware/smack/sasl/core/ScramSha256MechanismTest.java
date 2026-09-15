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
package org.jivesoftware.smack.sasl.core;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.sasl.packet.SaslNonza.AuthMechanism;
import org.jivesoftware.smack.sasl.packet.SaslNonza.Response;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;

public class ScramSha256MechanismTest extends SmackTestSuite {

    public static final String USERNAME = "user";
    public static final String PASSWORD = "pencil";
    // RFC 7677 Section 3 Test Vector
    public static final String CLIENT_FIRST_MESSAGE = "n,,n=user,r=rOprNGfwEbeRWgbNEkqO";
    public static final String SERVER_FIRST_MESSAGE = "r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0,s=W22ZaJ0SNY7soEsUEjb6gQ==,i=4096";
    public static final String CLIENT_FINAL_MESSAGE = "c=biws,r=rOprNGfwEbeRWgbNEkqO%hvYDpWUa2RaTCAfuxFIlj)hNlF$k0,p=dHzbZapWIk4jUhN+Ute9ytag9zjfMHgsqmmiz7AndVQ=";
    public static final String SERVER_FINAL_MESSAGE = "v=6rriTRBi23WpRR/wtup+mMhUZUn/dB5nLTJRsjl95G4=";

    @Test
    public void testScramSha256Mechanism() throws NotConnectedException, SmackException, InterruptedException {
        final DummyConnection con = new DummyConnection();
        ScramSha256Mechanism mech = new ScramSha256Mechanism() {
            @Override
            public String getRandomAscii() {
                this.connection = con;
                return "rOprNGfwEbeRWgbNEkqO";
            }
        };

        mech.authenticate(USERNAME, "unusedFoo", JidTestUtil.DOMAIN_BARE_JID_1, PASSWORD, null, null);
        AuthMechanism authMechanism = con.getSentPacket();
        assertEquals(ScramSha256Mechanism.NAME, authMechanism.getMechanism());
        assertEquals(CLIENT_FIRST_MESSAGE, saslLayerString(authMechanism.getAuthenticationText()));

        mech.challengeReceived(Base64.encode(SERVER_FIRST_MESSAGE), false);
        Response response = con.getSentPacket();
        assertEquals(CLIENT_FINAL_MESSAGE, saslLayerString(response.getAuthenticationText()));

        mech.challengeReceived(Base64.encode(SERVER_FINAL_MESSAGE), true);
        mech.checkIfSuccessfulOrThrow();
    }

    private static String saslLayerString(String string) {
        return Base64.decodeToString(string);
    }
}
