/**
 *
 * Copyright 2014-2015 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.util.concurrent.ThreadFactory;

/**
 * SmackExecutorThreadFactory creates daemon threads with a particular name. Note that you should
 * not use anonymous inner classes for thread factories in order to prevent threads from leaking.
 */
public final class SmackExecutorThreadFactory implements ThreadFactory {
    private final int connectionCounterValue;
    private final String name;
    private int count = 0;

    public SmackExecutorThreadFactory(int connectionCounterValue, String name) {
        this.connectionCounterValue = connectionCounterValue;
        this.name = name;
    }

    @Override
    public Thread newThread(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setName("Smack-" + name + ' ' + count++ + " (" + connectionCounterValue + ")");
        thread.setDaemon(true);
        return thread;
    }
}
