/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
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
package org.jivesoftware.smackx.jingle;

/**
 * The "action" in the jingle packet, as an enum.
 * 
 * Changed to reflect XEP-166 rev: 20JUN07
 * 
 * @author Jeff Williams
 */
public enum JingleActionEnum {

    UNKNOWN("unknown"),
    CONTENT_ACCEPT("content-accept"),
    CONTENT_ADD("content-add"),
    CONTENT_MODIFY("content-modify"),
    CONTENT_REMOVE("content-remove"),
    SESSION_ACCEPT("session-accept"),
    SESSION_INFO("session-info"),
    SESSION_INITIATE("session-initiate"),
    SESSION_TERMINATE("session-terminate"),
    TRANSPORT_INFO("transport-info");

    private String actionCode;

    private JingleActionEnum(String inActionCode) {
        actionCode = inActionCode;
    }

    /**
     * Returns the String value for an Action.
     */

    public String toString() {
        return actionCode;
    }

    /**
     * Returns the Action enum for a String action value.
     */
    public static JingleActionEnum getAction(String inActionCode) {
        for (JingleActionEnum jingleAction : JingleActionEnum.values()) {
            if (jingleAction.actionCode.equals(inActionCode)) {
                return jingleAction;
            }
        }
        return null;
    }

}
