/*
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jinglemessage;

/**
 * Implements <code>XmlElement</code> for XEP-0353: Jingle Message Initiation 0.8.0 (2026-02-19).
 * @see <a href="https://xmpp.org/extensions/xep-0353.html">XEP-0353: Jingle Message Initiation</a>
 *
 * @author Eng Chong Meng
 */
public enum JingleMessageState {
    initial,
    finish,
    proceed,
    propose,
    reject,
    retract,
    ringing;

    JingleMessageState() {
    }

    @Override
    public String toString() {
        return name();
    }

    public static JingleMessageState fromString(String name) {
        for (JingleMessageState t : JingleMessageState.values()) {
            if (t.toString().equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Illegal state: " + name);
    }
}
