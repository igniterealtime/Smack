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
package org.jivesoftware.smack.debugger;

import org.jivesoftware.smack.XMPPConnection;

import java.io.Reader;
import java.io.Writer;
import java.util.logging.Logger;

/**
 * Very simple debugger that prints to the console (stdout) the sent and received stanzas. Use
 * this debugger with caution since printing to the console is an expensive operation that may
 * even block the thread since only one thread may print at a time.<p>
 * <p/>
 * It is possible to not only print the raw sent and received stanzas but also the interpreted
 * packets by Smack. By default interpreted packets won't be printed. To enable this feature
 * just change the <tt>printInterpreted</tt> static variable to <tt>true</tt>.
 *
 * @author Gaston Dombiak
 */
public class JulDebugger extends AbstractDebugger {

    private static final Logger LOGGER = Logger.getLogger(JulDebugger.class.getName());

    public JulDebugger(XMPPConnection connection, Writer writer, Reader reader) {
        super(connection, writer, reader);
    }

    @Override
    protected void log(String logMessage) {
        LOGGER.fine(logMessage);
    }

}
