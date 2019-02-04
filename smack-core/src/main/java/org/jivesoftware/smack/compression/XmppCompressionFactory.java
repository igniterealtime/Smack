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
package org.jivesoftware.smack.compression;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XmppInputOutputFilter;

public abstract class XmppCompressionFactory implements Comparable<XmppCompressionFactory> {

    private final String method;
    private final int priority;

    protected XmppCompressionFactory(String method, int priority) {
        this.method = method;
        this.priority = priority;
    }

    public final String getCompressionMethod() {
        return method;
    }

    public final int getPriority() {
        return priority;
    }

    @Override
    public final int compareTo(XmppCompressionFactory other) {
        return Integer.compare(getPriority(), other.getPriority());
    }

    public abstract XmppInputOutputFilter fabricate(ConnectionConfiguration configuration);

}
