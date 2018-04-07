/**
 *
 * Copyright 2018 Florian Schmaus
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
package org.jivesoftware.smack.chat2;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;

public abstract class AbstractChatIntegrationTest extends AbstractSmackIntegrationTest {

    protected final ChatManager chatManagerOne;
    protected final ChatManager chatManagerTwo;
    protected final ChatManager chatManagerThree;

    protected AbstractChatIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        chatManagerOne = ChatManager.getInstanceFor(conOne);
        chatManagerTwo = ChatManager.getInstanceFor(conTwo);
        chatManagerThree = ChatManager.getInstanceFor(conThree);
    }

}
