/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle;

/**
 * Created by vanitas on 27.07.17.
 * Enum State is used by <code>JingleIncomingFileOffer</code> and <code>JingleOutgoingFileOffer</code>
 * file transfer progress status update
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public interface JingleDescriptionController {

    enum State {
        pending,            // Not yet accepted by us/peer
        negotiating,        // Accepted, but still negotiating transports etc.
        active,             // Bytestream initialized and active
        cancelled,          // We/Peer cancelled the transmission
        ended               // Successfully ended
    }

    State getState();
}
