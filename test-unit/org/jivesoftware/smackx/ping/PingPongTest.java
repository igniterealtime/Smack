/**
 * Copyright 2012 Florian Schmaus
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.ping;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smackx.ping.packet.Ping;
import org.jivesoftware.smackx.ping.packet.Pong;
import org.junit.Test;

public class PingPongTest {

    @Test
    public void createPongfromPingTest() {
        Ping ping = new Ping("from@sender.local/resourceFrom", "to@receiver.local/resourceTo");

        // create a pong from a ping
        Pong pong = new Pong(ping);

        assertEquals(pong.getFrom(), ping.getTo());
        assertEquals(pong.getTo(), ping.getFrom());
        assertEquals(pong.getPacketID(), ping.getPacketID());
    }

}
