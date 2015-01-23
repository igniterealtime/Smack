/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.jivesoftware.smack.iqrequest;

import org.jivesoftware.smack.packet.IQ;

/**
 * IQ request handler are responsible for handling incoming IQ requests. They can be registered with
 * {@link org.jivesoftware.smack.XMPPConnection#registerIQRequestHandler(IQRequestHandler)}.
 *
 * @see AbstractIqRequestHandler
 */
public interface IQRequestHandler {

    public enum Mode {
        /**
         * Process requests synchronously, i.e. in the order they arrive. Uses a single thread, which means that the other
         * requests have to wait until all previous synchronous requests have been handled. Use {@link #async} if
         * possible for performance reasons.
         */
        sync,

        /**
         * Process IQ requests asynchronously, i.e. concurrent. This does not guarantee that requests are processed in the
         * same order they arrive.
         */
        async,
    }

    public IQ handleIQRequest(IQ iqRequest);

    public Mode getMode();

    public IQ.Type getType();

    public String getElement();

    public String getNamespace();
}
