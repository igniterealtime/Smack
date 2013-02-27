/**
 * Copyright 2012-2013 Florian Schmaus
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

import java.lang.ref.WeakReference;
import java.util.Set;

import org.jivesoftware.smack.Connection;

class ServerPingTask implements Runnable {

    // This has to be a weak reference because IIRC all threads are roots
    // for objects and we have a new thread here that should hold a strong
    // reference to connection so that it can be GCed.
    private WeakReference<Connection> weakConnection;

    private int delta = 1000; // 1 seconds
    private int tries = 3; // 3 tries

    protected ServerPingTask(Connection connection) {
        this.weakConnection = new WeakReference<Connection>(connection);
    }

    public void run() {
        Connection connection = weakConnection.get();
        if (connection == null) {
            // connection has been collected by GC
            // which means we can stop the thread by breaking the loop
            return;
        }
        if (connection.isAuthenticated()) {
            PingManager pingManager = PingManager.getInstanceFor(connection);
            boolean res = false;

            for (int i = 0; i < tries; i++) {
                if (i != 0) {
                    try {
                        Thread.sleep(delta);
                    } catch (InterruptedException e) {
                        // We received an interrupt
                        // This only happens if we should stop pinging
                        return;
                    }
                }
                res = pingManager.pingMyServer();
                // stop when we receive a pong back
                if (res) {
                    pingManager.lastSuccessfulPingByTask = System.currentTimeMillis();
                    break;
                }
            }
            if (!res) {
                Set<PingFailedListener> pingFailedListeners = pingManager.getPingFailedListeners();
                for (PingFailedListener l : pingFailedListeners) {
                    l.pingFailed();
                }
            } else {
                // Ping was successful, wind-up the periodic task again
                pingManager.maybeSchedulePingServerTask();
            }
        }
    }
}
