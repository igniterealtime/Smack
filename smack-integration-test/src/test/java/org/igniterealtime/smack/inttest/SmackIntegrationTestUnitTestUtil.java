/**
 *
 * Copyright 2015-2017 Florian Schmaus
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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jxmpp.jid.JidTestUtil;

public class SmackIntegrationTestUnitTestUtil {

    public static DummySmackIntegrationTestFramework getFrameworkForUnitTest(
                    Class<? extends AbstractSmackIntTest> unitTest)
                    throws KeyManagementException, NoSuchAlgorithmException {
        // @formatter:off
        Configuration configuration = Configuration.builder()
                        .setService(JidTestUtil.DOMAIN_BARE_JID_1)
                        .setUsernamesAndPassword("dummy1", "dummy1pass", "dummy2", "dummy2pass", "dummy3", "dummy3pass")
                        .addEnabledTest(unitTest).build();
        // @formatter:on
        return new DummySmackIntegrationTestFramework(configuration);
    }

}
