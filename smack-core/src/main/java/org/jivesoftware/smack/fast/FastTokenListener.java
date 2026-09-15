/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.fast;

/**
 * A listener for FAST token lifecycle events.
 */
public interface FastTokenListener {

    /**
     * Invoked when a new or rotated FAST token is received from the server.
     *
     * @param token the received FAST token.
     */
    void onFastTokenReceived(FastToken token);

    /**
     * Invoked when the FAST token is invalidated, deleted, or rejected by the server.
     */
    default void onFastTokenInvalidated() {
    }
}
