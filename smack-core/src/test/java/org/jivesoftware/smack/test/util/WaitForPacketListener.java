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
package org.jivesoftware.smack.test.util;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Stanza;

public class WaitForPacketListener implements PacketListener {

    private CountDownLatch latch = new CountDownLatch(1);

    @Override
    public void processPacket(Stanza packet) throws NotConnectedException {
        reportInvoked();
    }

    protected void reportInvoked() {
        latch.countDown();
    }

    public void waitAndReset() {
        waitUntilInvocationOrTimeout();
        reset();
    }

    public void waitUntilInvocationOrTimeout() {
        try {
            boolean res = latch.await(300, TimeUnit.SECONDS);
            if (!res) {
                throw new IllegalStateException("Latch timed out before it reached zero");
            }
        }
        catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
    }

    public void reset() {
        latch = new CountDownLatch(1);
    }
}
