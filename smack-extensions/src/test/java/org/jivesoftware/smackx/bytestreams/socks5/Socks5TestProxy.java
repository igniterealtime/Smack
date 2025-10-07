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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.io.IOException;
import java.net.ServerSocket;

import org.jivesoftware.smack.test.util.NetworkUtil;

/**
 * Simple SOCKS5 proxy for testing purposes. It is almost the same as the Socks5Proxy class but the
 * port can be configured more easy and it all connections are allowed.
 *
 * @author Henning Staib
 */
public final class Socks5TestProxy extends Socks5Proxy implements AutoCloseable {

    public Socks5TestProxy(ServerSocket serverSocket) {
        super(serverSocket);
    }

    public Socks5TestProxy() throws IOException {
        this(NetworkUtil.getSocketOnLoopback());
    }

    @Override
    public void close() {
        stop();
    }

}
