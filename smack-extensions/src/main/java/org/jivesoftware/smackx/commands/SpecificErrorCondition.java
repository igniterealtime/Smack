/**
 *
 * Copyright 2005-2007 Jive Software.
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
package org.jivesoftware.smackx.commands;

public enum SpecificErrorCondition {

    /**
     * The responding JID cannot accept the specified action.
     */
    badAction("bad-action"),

    /**
     * The responding JID does not understand the specified action.
     */
    malformedAction("malformed-action"),

    /**
     * The responding JID cannot accept the specified language/locale.
     */
    badLocale("bad-locale"),

    /**
     * The responding JID cannot accept the specified payload (e.g. the data
     * form did not provide one or more required fields).
     */
    badPayload("bad-payload"),

    /**
     * The responding JID cannot accept the specified sessionid.
     */
    badSessionid("bad-sessionid"),

    /**
     * The requesting JID specified a sessionid that is no longer active
     * (either because it was completed, canceled, or timed out).
     */
    sessionExpired("session-expired");

    private final String value;

    SpecificErrorCondition(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
