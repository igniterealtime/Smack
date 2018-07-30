/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox;

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil;

import org.bouncycastle.openpgp.PGPException;
import org.junit.Test;
import org.pgpainless.key.OpenPgpV4Fingerprint;

public class PubSubDelegateTest extends SmackTestSuite {

    @Test
    public void pubkeyNodeNameTest() throws PGPException {
        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint("486f7065207520646f6e2068617665204f43640a");
        assertEquals("urn:xmpp:openpgp:0:public-keys:486F7065207520646F6E2068617665204F43640A",
                OpenPgpPubSubUtil.PEP_NODE_PUBLIC_KEY(fingerprint));
    }
}
