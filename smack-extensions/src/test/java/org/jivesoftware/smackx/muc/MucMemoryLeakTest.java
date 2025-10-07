/*
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smackx.muc;

import org.jivesoftware.smack.test.util.MemoryLeakTestUtil;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.jxmpp.stringprep.XmppStringprepException;

public class MucMemoryLeakTest extends SmackTestSuite {

    @Test
    @Disabled
    public void mucMemoryLeakTest() throws XmppStringprepException, IllegalArgumentException, InterruptedException {
        MemoryLeakTestUtil.noResourceLeakTest(c -> MultiUserChatManager.getInstanceFor(c));
    }

}
