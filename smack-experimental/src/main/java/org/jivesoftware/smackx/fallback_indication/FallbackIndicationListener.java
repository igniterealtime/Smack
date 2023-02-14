/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.fallback_indication;

import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.fallback_indication.element.FallbackIndicationElement;

public interface FallbackIndicationListener {

    /**
     * Listener method that gets called when a {@link Message} containing a {@link FallbackIndicationElement} is received.
     *
     * @param message message
     * @param indicator Fallback Indication
     * @param fallbackBody body that is marked as fallback
     */
    void onFallbackIndicationReceived(Message message, FallbackIndicationElement indicator, String fallbackBody);

}
