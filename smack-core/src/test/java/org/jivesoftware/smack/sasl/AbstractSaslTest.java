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

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;

public class AbstractSaslTest {

    protected final XMPPConnection xmppConnection = new DummyConnection();
    protected final SASLMechanism saslMechanism;

    protected AbstractSaslTest(SASLMechanism saslMechanism) {
        this.saslMechanism = saslMechanism.instanceForAuthentication(xmppConnection);
    }

}
