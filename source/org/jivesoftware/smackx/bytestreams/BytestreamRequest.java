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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamRequest;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamRequest;

/**
 * BytestreamRequest provides an interface to handle incoming bytestream requests.
 * <p>
 * There are two implementations of the interface. See {@link Socks5BytestreamRequest} and
 * {@link InBandBytestreamRequest}.
 * 
 * @author Henning Staib
 */
public interface BytestreamRequest {

    /**
     * Returns the sender of the bytestream open request.
     * 
     * @return the sender of the bytestream open request
     */
    public String getFrom();

    /**
     * Returns the session ID of the bytestream open request.
     * 
     * @return the session ID of the bytestream open request
     */
    public String getSessionID();

    /**
     * Accepts the bytestream open request and returns the session to send/receive data.
     * 
     * @return the session to send/receive data
     * @throws XMPPException if an error occurred while accepting the bytestream request
     * @throws InterruptedException if the thread was interrupted while waiting in a blocking
     *         operation
     */
    public BytestreamSession accept() throws XMPPException, InterruptedException;

    /**
     * Rejects the bytestream request by sending a reject error to the initiator.
     */
    public void reject();

}
