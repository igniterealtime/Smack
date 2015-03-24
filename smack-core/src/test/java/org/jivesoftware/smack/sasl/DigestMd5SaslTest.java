/**
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.sasl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.util.StringUtils;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class DigestMd5SaslTest extends AbstractSaslTest {

    protected static final String challenge = "realm=\"xmpp.org\",nonce=\"aTUr3GXqUtyy2B7HVDW6C+gQs+j+0EhWWjoBKkkg\",qop=\"auth\",charset=utf-8,algorithm=md5-sess";
    protected static final byte[] challengeBytes = StringUtils.toBytes(challenge);

    public DigestMd5SaslTest(SASLMechanism saslMechanism) {
        super(saslMechanism);
    }

    protected void runTest() throws NotConnectedException, SmackException, InterruptedException, XmppStringprepException {
        saslMechanism.authenticate("florian", "irrelevant", JidCreate.domainBareFrom("xmpp.org"), "secret");

        byte[] response = saslMechanism.evaluateChallenge(challengeBytes);
        String responseString = new String(response);
        String[] responseParts = responseString.split(",");
        Map<String, String> responsePairs = new HashMap<String, String>();
        for (String part : responseParts) {
            String[] keyValue = part.split("=");
            assertTrue(keyValue.length == 2);
            String key = keyValue[0];
            String value = keyValue[1].replace("\"", "");
            responsePairs.put(key, value);
        }
        assertMapValue("username", "florian", responsePairs);
        assertMapValue("realm", "xmpp.org", responsePairs);
        assertMapValue("digest-uri", "xmpp/xmpp.org", responsePairs);
        assertMapValue("qop", "auth", responsePairs);
    }

    private static void assertMapValue(String key, String value, Map<String, String> map) {
        assertEquals(map.get(key), value);
    }
}
