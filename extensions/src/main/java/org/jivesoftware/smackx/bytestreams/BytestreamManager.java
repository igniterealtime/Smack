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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;

/**
 * BytestreamManager provides a generic interface for bytestream managers.
 * <p>
 * There are two implementations of the interface. See {@link Socks5BytestreamManager} and
 * {@link InBandBytestreamManager}.
 * 
 * @author Henning Staib
 */
public interface BytestreamManager {

    /**
     * Adds {@link BytestreamListener} that is called for every incoming bytestream request unless
     * there is a user specific {@link BytestreamListener} registered.
     * <p>
     * See {@link Socks5BytestreamManager#addIncomingBytestreamListener(BytestreamListener)} and
     * {@link InBandBytestreamManager#addIncomingBytestreamListener(BytestreamListener)} for further
     * details.
     * 
     * @param listener the listener to register
     */
    public void addIncomingBytestreamListener(BytestreamListener listener);

    /**
     * Removes the given listener from the list of listeners for all incoming bytestream requests.
     * 
     * @param listener the listener to remove
     */
    public void removeIncomingBytestreamListener(BytestreamListener listener);

    /**
     * Adds {@link BytestreamListener} that is called for every incoming bytestream request unless
     * there is a user specific {@link BytestreamListener} registered.
     * <p>
     * Use this method if you are awaiting an incoming bytestream request from a specific user.
     * <p>
     * See {@link Socks5BytestreamManager#addIncomingBytestreamListener(BytestreamListener, String)}
     * and {@link InBandBytestreamManager#addIncomingBytestreamListener(BytestreamListener, String)}
     * for further details.
     * 
     * @param listener the listener to register
     * @param initiatorJID the JID of the user that wants to establish a bytestream
     */
    public void addIncomingBytestreamListener(BytestreamListener listener, String initiatorJID);

    /**
     * Removes the listener for the given user.
     * 
     * @param initiatorJID the JID of the user the listener should be removed
     */
    public void removeIncomingBytestreamListener(String initiatorJID);

    /**
     * Establishes a bytestream with the given user and returns the session to send/receive data
     * to/from the user.
     * <p>
     * Use this method to establish bytestreams to users accepting all incoming bytestream requests
     * since this method doesn't provide a way to tell the user something about the data to be sent.
     * <p>
     * To establish a bytestream after negotiation the kind of data to be sent (e.g. file transfer)
     * use {@link #establishSession(String, String)}.
     * <p>
     * See {@link Socks5BytestreamManager#establishSession(String)} and
     * {@link InBandBytestreamManager#establishSession(String)} for further details.
     * 
     * @param targetJID the JID of the user a bytestream should be established
     * @return the session to send/receive data to/from the user
     * @throws XMPPException if an error occurred while establishing the session
     * @throws IOException if an IO error occurred while establishing the session
     * @throws InterruptedException if the thread was interrupted while waiting in a blocking
     *         operation
     */
    public BytestreamSession establishSession(String targetJID) throws XMPPException, IOException,
                    InterruptedException;

    /**
     * Establishes a bytestream with the given user and returns the session to send/receive data
     * to/from the user.
     * <p>
     * See {@link Socks5BytestreamManager#establishSession(String)} and
     * {@link InBandBytestreamManager#establishSession(String)} for further details.
     * 
     * @param targetJID the JID of the user a bytestream should be established
     * @param sessionID the session ID for the bytestream request
     * @return the session to send/receive data to/from the user
     * @throws XMPPException if an error occurred while establishing the session
     * @throws IOException if an IO error occurred while establishing the session
     * @throws InterruptedException if the thread was interrupted while waiting in a blocking
     *         operation
     */
    public BytestreamSession establishSession(String targetJID, String sessionID)
                    throws XMPPException, IOException, InterruptedException;

}
