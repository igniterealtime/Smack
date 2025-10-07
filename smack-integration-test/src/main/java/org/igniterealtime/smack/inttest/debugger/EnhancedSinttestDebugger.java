/*
 *
 * Copyright 2024 Florian Schmaus
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
package org.igniterealtime.smack.inttest.debugger;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.debugger.SmackDebuggerFactory;

import org.jivesoftware.smackx.debugger.EnhancedDebugger;
import org.jivesoftware.smackx.debugger.EnhancedDebuggerWindow;

import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.TestRunResult;

public class EnhancedSinttestDebugger implements SinttestDebugger {

    protected static final Logger LOGGER = Logger.getLogger(StandardSinttestDebugger.class.getName());

    @Override
    public SmackDebuggerFactory getSmackDebuggerFactory() {
        return EnhancedDebugger.Factory.INSTANCE;
    }

    @Override
    public void onSinttestFinished(TestRunResult testRunResult) {
        try {
            EnhancedDebuggerWindow.getInstance().waitUntilClosed();
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, e + " while waiting for debugger window to be closed", e);
        }
    }

}
