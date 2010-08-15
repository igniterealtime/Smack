/**
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
package org.jivesoftware.smackx.bytestreams;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;

/**
 * BytestreamSession provides an interface for established bytestream sessions.
 * <p>
 * There are two implementations of the interface. See {@link Socks5BytestreamSession} and
 * {@link InBandBytestreamSession}.
 * 
 * @author Henning Staib
 */
public interface BytestreamSession {

    /**
     * Returns the InputStream associated with this session to send data.
     * 
     * @return the InputStream associated with this session to send data
     * @throws IOException if an error occurs while retrieving the input stream
     */
    public InputStream getInputStream() throws IOException;

    /**
     * Returns the OutputStream associated with this session to receive data.
     * 
     * @return the OutputStream associated with this session to receive data
     * @throws IOException if an error occurs while retrieving the output stream
     */
    public OutputStream getOutputStream() throws IOException;

    /**
     * Closes the bytestream session.
     * <p>
     * Closing the session will also close the input stream and the output stream associated to this
     * session.
     * 
     * @throws IOException if an error occurs while closing the session
     */
    public void close() throws IOException;

    /**
     * Returns the timeout for read operations of the input stream associated with this session. 0
     * returns implies that the option is disabled (i.e., timeout of infinity). Default is 0.
     * 
     * @return the timeout for read operations
     * @throws IOException if there is an error in the underlying protocol
     */
    public int getReadTimeout() throws IOException;

    /**
     * Sets the specified timeout, in milliseconds. With this option set to a non-zero timeout, a
     * read() call on the input stream associated with this session will block for only this amount
     * of time. If the timeout expires, a java.net.SocketTimeoutException is raised, though the
     * session is still valid. The option must be enabled prior to entering the blocking operation
     * to have effect. The timeout must be > 0. A timeout of zero is interpreted as an infinite
     * timeout. Default is 0.
     * 
     * @param timeout the specified timeout, in milliseconds
     * @throws IOException if there is an error in the underlying protocol
     */
    public void setReadTimeout(int timeout) throws IOException;

}
