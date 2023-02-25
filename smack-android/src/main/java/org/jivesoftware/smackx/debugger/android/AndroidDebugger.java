/**
 *
 * Copyright © 2014-2021 Florian Schmaus
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
package org.jivesoftware.smackx.debugger.android;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.AbstractDebugger;

import android.util.Log;

/**
 * Very simple debugger that prints to the android log the sent and received stanzas.
 * <p>
 * Only use this debugger if really required, Android has a good java.util.logging
 * implementation, therefore {@link org.jivesoftware.smack.debugger.JulDebugger} is preferred.
 * </p>
 * It is possible to not only print the raw sent and received stanzas but also the interpreted
 * packets by Smack. By default interpreted packets won't be printed. To enable this feature
 * just change the <code>printInterpreted</code> static variable to <code>true</code>.
 *
 */
public class AndroidDebugger extends AbstractDebugger {

    /**
     * Constructs a new Smack debugger for Android.
     *
     * @param connection the connection to debug.
     */
    public AndroidDebugger(XMPPConnection connection) {
        super(connection);
    }

    @Override
    protected void log(String logMessage) {
                Log.d("SMACK", logMessage);
    }

    @Override
    protected void log(String logMessage, Throwable throwable) {
                Log.d("SMACK", logMessage, throwable);
    }
}
