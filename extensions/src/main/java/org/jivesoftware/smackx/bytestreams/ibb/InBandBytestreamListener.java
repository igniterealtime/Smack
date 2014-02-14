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
package org.jivesoftware.smackx.bytestreams.ibb;

import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamRequest;

/**
 * InBandBytestreamListener are informed if a remote user wants to initiate an In-Band Bytestream.
 * Implement this interface to handle incoming In-Band Bytestream requests.
 * <p>
 * There are two ways to add this listener. See
 * {@link InBandBytestreamManager#addIncomingBytestreamListener(BytestreamListener)} and
 * {@link InBandBytestreamManager#addIncomingBytestreamListener(BytestreamListener, String)} for
 * further details.
 * 
 * @author Henning Staib
 */
public abstract class InBandBytestreamListener implements BytestreamListener {

    
    
    public void incomingBytestreamRequest(BytestreamRequest request) {
        incomingBytestreamRequest((InBandBytestreamRequest) request);
    }

    /**
     * This listener is notified if an In-Band Bytestream request from another user has been
     * received.
     * 
     * @param request the incoming In-Band Bytestream request
     */
    public abstract void incomingBytestreamRequest(InBandBytestreamRequest request);

}
