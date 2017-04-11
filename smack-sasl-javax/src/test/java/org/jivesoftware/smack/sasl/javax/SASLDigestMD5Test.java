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
package org.jivesoftware.smack.sasl.javax;

import java.io.UnsupportedEncodingException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.sasl.DigestMd5SaslTest;
import org.junit.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class SASLDigestMD5Test extends DigestMd5SaslTest {

    public SASLDigestMD5Test() {
        super(new SASLDigestMD5Mechanism());
    }

    @Test
    public void testDigestMD5() throws NotConnectedException, SmackException, InterruptedException, XmppStringprepException, UnsupportedEncodingException {
        runTest(false);
    }

    @Test
    public void testDigestMD5Authzid() throws NotConnectedException, SmackException, InterruptedException, XmppStringprepException, UnsupportedEncodingException {
        runTest(true);
    }
}
