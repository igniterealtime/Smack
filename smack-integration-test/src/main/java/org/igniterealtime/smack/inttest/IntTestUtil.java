/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.igniterealtime.smack.inttest;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.iqregister.AccountManager;

public class IntTestUtil {

    public static UsernameAndPassword registerAccount(XMPPConnection connection)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        return registerAccount(connection, StringUtils.randomString(12),
                        StringUtils.randomString(12));
    }

    public static UsernameAndPassword registerAccount(XMPPConnection connection, String username,
                    String password) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        AccountManager accountManager = AccountManager.getInstance(connection);
        if (!accountManager.supportsAccountCreation()) {
            throw new UnsupportedOperationException("Account creation/registation is not supported");
        }
        Set<String> requiredAttributes = accountManager.getAccountAttributes();
        if (requiredAttributes.size() > 4) {
            throw new IllegalStateException("Unkown required attributes");
        }
        Map<String, String> additionalAttributes = new HashMap<>();
        additionalAttributes.put("name", "Smack Integration Test");
        additionalAttributes.put("email", "flow@igniterealtime.org");
        accountManager.createAccount(username, password, additionalAttributes);

        return new UsernameAndPassword(username, password);
    }

    public static final class UsernameAndPassword {
        public final String username;
        public final String password;

        private UsernameAndPassword(String username, String password) {
            this.username = username;
            this.password = password;
        }
    }
}
