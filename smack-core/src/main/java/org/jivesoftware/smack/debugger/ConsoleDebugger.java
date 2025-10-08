/*
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack.debugger;

import org.jivesoftware.smack.XMPPConnection;

/**
 * Very simple debugger that prints to the console (stdout) the sent and received stanzas. Use
 * this debugger with caution since printing to the console is an expensive operation that may
 * even block the thread since only one thread may print at a time.
 * <p>
 * It is possible to not only print the raw sent and received stanzas but also the interpreted
 * packets by Smack. By default,interpreted packets won't be printed. To enable this feature
 * just change the <code>printInterpreted</code> static variable to <code>true</code>.
 * </p>
 *
 * @author Gaston Dombiak
 */
public class ConsoleDebugger extends SimpleAbstractDebugger {

    public ConsoleDebugger(XMPPConnection connection) {
        super(connection);
    }

    @Override
    protected void logSink(String message) {
        // CHECKSTYLE:OFF
        System.out.println(message);
        // CHECKSTYLE:ON
    }

    public static final class Factory implements SmackDebuggerFactory {

        public static final SmackDebuggerFactory INSTANCE = new Factory();

        private Factory() {
        }

        @Override
        public SmackDebugger create(XMPPConnection connection) throws IllegalArgumentException {
            return new ConsoleDebugger(connection);
        }

    }
}
