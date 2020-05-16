/**
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smack.internal;

import java.io.IOException;

import org.jivesoftware.smack.util.ExtendedAppendable;

public abstract class AbstractStats {

    public final void appendStatsTo(Appendable appendable) throws IOException {
        appendStatsTo(new ExtendedAppendable(appendable));
    }

    public abstract void appendStatsTo(ExtendedAppendable appendable) throws IOException;

    private transient String toStringCache;

    @Override
    public final String toString() {
        if (toStringCache != null) {
            return toStringCache;
        }

        StringBuilder sb = new StringBuilder();
        try {
            appendStatsTo(sb);
        } catch (IOException e) {
            // Should never happen.
            throw new AssertionError(e);
        }

        toStringCache =  sb.toString();

        return toStringCache;
    }

}
