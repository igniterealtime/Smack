/**
 *
 * Copyright 2019-2023 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.packet.IQ.Type;

public interface IqView extends StanzaView {

    /**
     * Returns the type of the IQ packet.
     *
     * @return the type of the IQ packet.
     */
    IQ.Type getType();

    /**
     * Return true if this IQ is a request IQ, i.e. an IQ of type {@link Type#get} or {@link Type#set}.
     *
     * @return true if IQ type is 'get' or 'set', false otherwise.
     * @since 4.1
     */
    default boolean isRequestIQ() {
        IQ.Type type = getType();
        return type == IQ.Type.get || type == IQ.Type.set;
    }

    /**
     * Return true if this IQ is a request, i.e. an IQ of type {@link Type#result} or {@link Type#error}.
     *
     * @return true if IQ type is 'result' or 'error', false otherwise.
     * @since 4.4
     */
    default boolean isResponseIQ() {
        return !isRequestIQ();
    }
}
