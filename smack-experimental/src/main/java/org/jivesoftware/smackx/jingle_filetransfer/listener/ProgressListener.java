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
package org.jivesoftware.smackx.jingle_filetransfer.listener;

import org.jivesoftware.smackx.jingle.element.JingleReason;

/**
 * User interface for jingle file transfer progress status.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public interface ProgressListener {

    /**
     * Notify file transfer has started the byte-stream sending/receiving.
     */
    void onStarted();

    /**
     * Number of bytes sent or received.
     *
     * @param rwBytes progressive byte count for byte-stream sent/received
     */
    void progress(int rwBytes);

    /**
     * Notify user the byte stream send/receive has been completed successfully.
     * Client may still sent session-terminate with error; use JingleReason to determine success/failure
     */
    void onFinished();

    /**
     * Notify user of an error occurred while doing file transfer.
     * @param reason JingleReason error
     */
    void onError(JingleReason reason);
}
