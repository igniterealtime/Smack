/**
 *
 * Copyright © 2014-2017 Florian Schmaus
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
package org.jivesoftware.smackx.jingle.element;

import java.util.HashMap;
import java.util.Map;

/**
 * The "action" in the jingle packet, as an enum.
 * 
 * 
 * @author Florian Schmaus
 */
public enum JingleAction {

    content_accept,
    content_add,
    content_modify,
    content_reject,
    content_remove,
    description_info,
    security_info,
    session_accept,
    session_info,
    session_initiate,
    session_terminate,
    transport_accept,
    transport_info,
    transport_reject,
    transport_replace,
    ;

    private static final Map<String, JingleAction> map = new HashMap<>(
                    JingleAction.values().length);
    static {
        for (JingleAction jingleAction : JingleAction.values()) {
            map.put(jingleAction.toString(), jingleAction);
        }
    }

    private final String asString;

    JingleAction() {
        asString = this.name().replace('_', '-');
    }

    @Override
    public String toString() {
        return asString;
    }

    public static JingleAction fromString(String string) {
        JingleAction jingleAction = map.get(string);
        if (jingleAction == null) {
            throw new IllegalArgumentException("Unknown jingle action: " + string);
        }
        return jingleAction;
    }
}
