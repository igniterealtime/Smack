/**
 *
 * Copyright 2017 Florian Schmaus.
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

/**
 * A listener for the {@link ReconnectionManager}. Use
 * {@link ReconnectionManager#addReconnectionListener(ReconnectionListener)} to add new listeners to the reconnection
 * manager.
 *
 * @since 4.2.2
 */
public interface ReconnectionListener {

    /**
     * The connection will retry to reconnect in the specified number of seconds.
     * <p>
     * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()} returns true, i.e.
     * only when the reconnection manager is enabled for the connection.
     * </p>
     *
     * @param seconds remaining seconds before attempting a reconnection.
     */
    public void reconnectingIn(int seconds);

    /**
     * An attempt to connect to the server has failed. The connection will keep trying reconnecting to the server in a
     * moment.
     * <p>
     * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()} returns true, i.e.
     * only when the reconnection manager is enabled for the connection.
     * </p>
     *
     * @param e the exception that caused the reconnection to fail.
     */
    public void reconnectionFailed(Exception e);
}
