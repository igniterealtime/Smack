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
package org.jivesoftware.smack.debugger;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.ExceptionUtil;

/**
 * Very simple debugger that prints to the an abstract sink.
 */
public abstract class SimpleAbstractDebugger extends AbstractDebugger {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.S");

    public SimpleAbstractDebugger(XMPPConnection connection) {
        super(connection);
    }

    protected abstract void logSink(String message);

    @Override
    protected void log(String logMessage) {
        String formatedDate = DATE_FORMATTER.format(LocalDateTime.now());

        logSink(formatedDate + ' ' + logMessage);
    }

    @Override
    protected void log(String logMessage, Throwable throwable) {
        String stacktrace = ExceptionUtil.getStackTrace(throwable);
        log(logMessage + '\n' + stacktrace);
    }

}
