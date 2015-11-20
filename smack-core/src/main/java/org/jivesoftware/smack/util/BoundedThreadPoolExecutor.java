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
package org.jivesoftware.smack.util;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class BoundedThreadPoolExecutor extends ThreadPoolExecutor {

    private final Semaphore semaphore;

    public BoundedThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime,
                    TimeUnit unit, int bound, ThreadFactory threadFactory) {
        // One could think that the array blocking queue bound should be "bound - 1" because the bound protected by the
        // Semaphore also includes the "slot" in the worker thread executing the Runnable. But using that as bound could
        // actually cause a RejectedExecutionException as the queue could fill up while the worker thread remains
        // unscheduled and is thus not removing any entries.
        super(corePoolSize, maximumPoolSize, keepAliveTime,
                        unit, new ArrayBlockingQueue<Runnable>(bound), threadFactory);
        semaphore = new Semaphore(bound);
    }

    public void executeBlocking(final Runnable command) throws InterruptedException {
        semaphore.acquire();
        try {
            execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        command.run();
                    } finally {
                        semaphore.release();
                    }
                }
            });
        } catch (Exception e) {
            semaphore.release();
            if (e instanceof RejectedExecutionException) {
                throw (RejectedExecutionException) e;
            } else {
                throw new RuntimeException(e);
            }
        }
    }
}
