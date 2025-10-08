/*
 *
 * Copyright 2018-2024 Florian Schmaus
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
package org.jivesoftware.smack;

import java.util.Date;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.util.Async;

public class ScheduledAction implements Delayed {

    enum Kind {
        NonBlocking,
        Blocking,
    }

    private final Runnable action;
    final Date releaseTime;
    final SmackReactor smackReactor;
    final Kind kind;

    ScheduledAction(Runnable action, Date releaseTime, SmackReactor smackReactor, Kind kind) {
        this.action = action;
        this.releaseTime = releaseTime;
        this.smackReactor = smackReactor;
        this.kind = kind;
    }

    /**
     * Cancels this scheduled action.
     *
     * @return <code>true</code> if the scheduled action was still pending and got removed, <code>false</code> otherwise.
     */
    public boolean cancel() {
        return smackReactor.cancel(this);
    }

    @SuppressWarnings("JavaUtilDate")
    public boolean isDue() {
        Date now = new Date();
        return now.after(releaseTime);
    }

    @SuppressWarnings("JavaUtilDate")
    public long getTimeToDueMillis() {
        long now = System.currentTimeMillis();
        return releaseTime.getTime() - now;
    }

    @Override
    public int compareTo(Delayed otherDelayed) {
        if (this == otherDelayed) {
            return 0;
        }

        long thisDelay = getDelay(TimeUnit.MILLISECONDS);
        long otherDelay = otherDelayed.getDelay(TimeUnit.MILLISECONDS);

        return Long.compare(thisDelay, otherDelay);
    }

    @Override
    public long getDelay(TimeUnit unit) {
        long delayInMillis = getTimeToDueMillis();
        return unit.convert(delayInMillis, TimeUnit.MILLISECONDS);
    }

    void run() {
        switch (kind) {
        case NonBlocking:
            action.run();
            break;
        case Blocking:
            Async.go(() -> action.run());
            break;
        }
    }
}
