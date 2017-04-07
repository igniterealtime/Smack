/**
 *
 * Copyright 2014 Vyacheslav Blinov
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

package org.jivesoftware.smackx.debugger.slf4j;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.XMPPConnection;
import org.slf4j.Logger;

class SLF4JLoggingConnectionListener implements ConnectionListener {
    private final XMPPConnection connection;
    private final Logger logger;

    public SLF4JLoggingConnectionListener(XMPPConnection connection, Logger logger) {
        this.connection = Validate.notNull(connection);
        this.logger = Validate.notNull(logger);
    }

    @Override
    public void connected(XMPPConnection connection) {
        logger.debug("({}) Connection connected", connection.hashCode());
    }

    @Override
    public void authenticated(XMPPConnection connection, boolean resumed) {
        logger.debug("({}) Connection authenticated as {}", connection.hashCode(), connection.getUser());
    }

    @Override
    public void connectionClosed() {
        logger.debug("({}) Connection closed", connection.hashCode());
    }

    @Override
    public void connectionClosedOnError(Exception e) {
        logger.debug("({}) Connection closed due to an exception: {}", connection.hashCode(), e);
    }

    @Override
    public void reconnectionFailed(Exception e) {
        logger.debug("({}) Reconnection failed due to an exception: {}", connection.hashCode(), e);
    }

    @Override
    public void reconnectionSuccessful() {
        logger.debug("({}) Connection reconnected", connection.hashCode());
    }

    @Override
    public void reconnectingIn(int seconds) {
        logger.debug("({}) Connection will reconnect in {}", connection.hashCode(), seconds);
    }
}
