/**
 *
 * Copyright 2015-2020 Florian Schmaus
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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jxmpp.jid.JidTestUtil;

public class SmackIntegrationTestUnitTestUtil {

    public static DummySmackIntegrationTestFramework getFrameworkForUnitTest(
                    Class<? extends AbstractSmackIntTest> unitTest)
                    throws KeyManagementException, NoSuchAlgorithmException {
        // @formatter:off
        Configuration configuration = Configuration.builder()
                        .setService(JidTestUtil.DOMAIN_BARE_JID_1)
                        .setUsernamesAndPassword("dummy1", "dummy1pass", "dummy2", "dummy2pass", "dummy3", "dummy3pass")
                        .setDefaultConnection(DummySmackIntegrationTestFramework.DUMMY_CONNECTION_NICKNAME)
                        .addEnabledTest(unitTest)
                        .build();
        // @formatter:on
        try {
            return new DummySmackIntegrationTestFramework(configuration);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                | SmackException | IOException | XMPPException | InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

}
