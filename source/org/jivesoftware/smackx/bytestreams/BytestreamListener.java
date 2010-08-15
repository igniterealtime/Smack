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

import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamListener;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamListener;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;

/**
 * BytestreamListener are notified if a remote user wants to initiate a bytestream. Implement this
 * interface to handle incoming bytestream requests.
 * <p>
 * BytestreamListener can be registered at the {@link Socks5BytestreamManager} or the
 * {@link InBandBytestreamManager}.
 * <p>
 * There are two ways to add this listener. See
 * {@link BytestreamManager#addIncomingBytestreamListener(BytestreamListener)} and
 * {@link BytestreamManager#addIncomingBytestreamListener(BytestreamListener, String)} for further
 * details.
 * <p>
 * {@link Socks5BytestreamListener} or {@link InBandBytestreamListener} provide a more specific
 * interface of the BytestreamListener.
 * 
 * @author Henning Staib
 */
public interface BytestreamListener {

    /**
     * This listener is notified if a bytestream request from another user has been received.
     * 
     * @param request the incoming bytestream request
     */
    public void incomingBytestreamRequest(BytestreamRequest request);

}
