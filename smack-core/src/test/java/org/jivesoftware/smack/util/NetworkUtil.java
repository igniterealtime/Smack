/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NetworkUtil {

    private static final Logger LOGGER = Logger.getLogger(NetworkUtil.class.getName());

    public static ServerSocket getSocketOnLoopback() {
        final InetAddress loopbackAddress = InetAddress.getLoopbackAddress();
        final int portMin = 1024;
        final int portMax = (1 << 16) - 1;
        final int backlog = 1;

        ServerSocket serverSocket = null;
        for (int port = portMin; port <= portMax; port++) {
            try {
                serverSocket = new ServerSocket(port, backlog, loopbackAddress);
                break;
            } catch (BindException e) {
                LOGGER.log(Level.FINEST, "Could not bind port " + port + ", trying next", e);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        }

        if (serverSocket == null) {
            throw new IllegalStateException();
        }

        return serverSocket;
    }
}
