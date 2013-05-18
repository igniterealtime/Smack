/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;

import org.jivesoftware.smackx.bytestreams.BytestreamSession;

/**
 * Socks5BytestreamSession class represents a SOCKS5 Bytestream session.
 * 
 * @author Henning Staib
 */
public class Socks5BytestreamSession implements BytestreamSession {

    /* the underlying socket of the SOCKS5 Bytestream */
    private final Socket socket;

    /* flag to indicate if this session is a direct or mediated connection */
    private final boolean isDirect;

    protected Socks5BytestreamSession(Socket socket, boolean isDirect) {
        this.socket = socket;
        this.isDirect = isDirect;
    }

    /**
     * Returns <code>true</code> if the session is established through a direct connection between
     * the initiator and target, <code>false</code> if the session is mediated over a SOCKS proxy.
     * 
     * @return <code>true</code> if session is a direct connection, <code>false</code> if session is
     *         mediated over a SOCKS5 proxy
     */
    public boolean isDirect() {
        return this.isDirect;
    }

    /**
     * Returns <code>true</code> if the session is mediated over a SOCKS proxy, <code>false</code>
     * if this session is established through a direct connection between the initiator and target.
     * 
     * @return <code>true</code> if session is mediated over a SOCKS5 proxy, <code>false</code> if
     *         session is a direct connection
     */
    public boolean isMediated() {
        return !this.isDirect;
    }

    public InputStream getInputStream() throws IOException {
        return this.socket.getInputStream();
    }

    public OutputStream getOutputStream() throws IOException {
        return this.socket.getOutputStream();
    }

    public int getReadTimeout() throws IOException {
        try {
            return this.socket.getSoTimeout();
        }
        catch (SocketException e) {
            throw new IOException("Error on underlying Socket");
        }
    }

    public void setReadTimeout(int timeout) throws IOException {
        try {
            this.socket.setSoTimeout(timeout);
        }
        catch (SocketException e) {
            throw new IOException("Error on underlying Socket");
        }
    }

    public void close() throws IOException {
        this.socket.close();
    }

}
