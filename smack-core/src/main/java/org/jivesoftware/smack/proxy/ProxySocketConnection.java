/**
 *
 * Copyright 2015-2019 Florian Schmaus.
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
package org.jivesoftware.smack.proxy;

import java.io.IOException;
import java.net.Socket;

public interface ProxySocketConnection {

    /**
     * Initiate a connection to the given host on the given port. Note that the caller is responsible for closing the
     * socket in case this method throws.
     *
     * @param socket the socket to use to initiate the connection to the proxy.
     * @param host the host to connect to.
     * @param port the port to connect to.
     * @param timeout the timeout in milliseconds.
     * @throws IOException in case an I/O error occurs.
     */
    void connect(Socket socket, String host, int port, int timeout)
                    throws IOException;

}
