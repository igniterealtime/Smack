/**
 *
 * Copyright 2014 Florian Schmaus
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

public class Async {

    /**
     * Creates a new thread with the given Runnable, marks it daemon, starts it and returns the started thread.
     *
     * @param runnable
     * @return the started thread.
     */
    public static Thread go(Runnable runnable) {
        Thread thread = daemonThreadFrom(runnable);
        thread.start();
        return thread;
    }

    /**
     * Creates a new thread with the given Runnable, marks it daemon, sets the name, starts it and returns the started
     * thread.
     *
     * @param runnable
     * @param threadName the thread name.
     * @return the started thread.
     */
    public static Thread go(Runnable runnable, String threadName) {
        Thread thread = daemonThreadFrom(runnable);
        thread.setName(threadName);
        thread.start();
        return thread;
    }

    public static Thread daemonThreadFrom(Runnable runnable) {
        Thread thread = new Thread(runnable);
        thread.setDaemon(true);
        return thread;
    }
}
