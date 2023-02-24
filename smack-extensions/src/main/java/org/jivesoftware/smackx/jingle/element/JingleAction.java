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
 * @see <a href="https://xmpp.org/extensions/xep-0166.html#concepts-session">XEP-0166 § 5.1 Overall Session Management</a>
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 */
public enum JingleAction {

    /**
     * The <code>content-accept</code> action is used to accept a <code>content-add</code> action received
     * from another party.
     */
    content_accept,

    /**
     * The <code>content-add</code> action is used to add one or more new content definitions to the
     * session. The sender MUST specify only the added content definition(s), not the added content
     * definition(s) plus the existing content definition(s). Therefore it is the responsibility of
     * the recipient to maintain a local copy of the current content definition(s). If the recipient
     * wishes to include the new content definition in the session, it MUST send a <code>content-accept</code>
     * action to the other party; if not, it MUST send a <code>content-reject</code> action to the other party.
     */
    content_add,

    /**
     * The <code>content-modify</code> action is used to change the direction of an existing content
     * definition through modification of the 'senders' attribute. If the recipient deems the
     * directionality of a <code>content-modify</code> action to be unacceptable, it MAY reply with a
     * contrary <code>content-modify</code> action, terminate the session, or simply refuse to send or
     * accept application data in the new direction. In any case, the recipient MUST NOT send a
     * <code>content-accept</code> action in response to the <code>content-modify</code>.
     */
    content_modify,

    /**
     * The <code>content-reject</code> action is used to reject a <code>content-add</code> action received
     * from another party.
     */
    content_reject,

    /**
     * The <code>content-remove</code> action is used to remove one or more content definitions from the
     * session. The sender MUST specify only the removed content definition(s), not the removed
     * content definition(s) plus the remaining content definition(s). Therefore it is the
     * responsibility of the recipient to maintain a local copy of the current content
     * definition(s). Upon receiving a content-remove from the other party, the recipient MUST NOT
     * send a <code>content-accept</code> and MUST NOT continue to negotiate the transport method or
     * send application data related to that content definition.
     *
     * If the <code>content-remove</code> results in zero content definitions for the session, the
     * entity that receives the <code>content-remove</code> SHOULD send a <code>session-terminate</code>
     * action to the other party (since a session with no content definitions is void).
     */
    content_remove,

    /**
     * The <code>description-info</code> action is used to send informational hints about parameters
     * related to the application type, such as the suggested height and width of a video display
     * area or suggested configuration for an audio stream.
     */
    description_info,

    /**
     * The <code>security-info</code> action is used to send information related to establishment or
     * maintenance of security preconditions.
     */
    security_info,

    /**
     * The <code>session-accept</code> action is used to definitively accept a session negotiation
     * (implicitly this action also serves as a <code>content-accept</code>). A <code>session-accept</code>
     * action indicates a willingness to proceed with the session (which might necessitate further
     * negotiation before media can be exchanged). The <code>session-accept</code> action indicates
     * acceptance only of the content definition(s) whose disposition type is "session" (the default
     * value of the <code>content</code> element's 'disposition' attribute), not any content definition(s)
     * whose disposition type is something other than "session" (e.g., "early-session" for early media).
     *
     * In the <code>session-accept</code> stanza, the <code>jingle</code> element MUST contain one or more
     * <code>content</code> elements, each of which MUST contain one <code>description</code> element and one <code>transport</code> element.
     */
    session_accept,

    /**
     * The <code>session-info</code> action is used to send information related to establishment or
     * maintenance of security preconditions.
     */
    session_info,

    /**
     * The <code>session-initiate</code> action is used to request negotiation of a new Jingle session.
     * When sending a <code>session-initiate</code> with one <code>content</code> element, the value of the
     * <code>content</code> element's 'disposition' attribute MUST be "session" (if there are multiple
     * <code>content</code> elements then at least one MUST have a disposition of "session"); if this rule is
     * violated, the responder MUST return a <code>bad-request</code> error to the initiator.
     */
    session_initiate,

    /**
     * The <code>session-terminate</code> action is used to end an existing session.
     */
    session_terminate,

    /**
     * The <code>transport-accept</code> action is used to accept a <code>transport-replace</code> action
     * received from another party.
     */
    transport_accept,

    /**
     * The <code>transport-info</code> action is used to exchange transport candidates; it is mainly
     * used in Jingle ICE-UDP but might be used in other transport specifications.
     */
    transport_info,

    /**
     * The <code>transport-reject</code> action is used to reject a <code>transport-replace</code> action
     * received from another party.
     */
    transport_reject,

    /**
     * The <code>transport-replace</code> action is used to redefine a transport method, typically for
     * fallback to a different method (e.g. changing from ICE-UDP to Raw UDP for a datagram transport,
     * or changing from SOCKS5 Bytestreams to In-Band Bytestreams for a streaming transport). If the
     * recipient wishes to use the new transport definition, it MUST send a transport-accept action to
     * the other party; if not, it MUST send a transport-reject action to the other party.
     */
    transport_replace,

    /**
     * The "source-add" action used in Jitsi-Meet.
     */
    source_add,

    /**
     * The "source-remove" action used in Jitsi-Meet.
     */
    source_remove,
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

    /**
     * Returns the name of this <code>JingleAction</code> (e.g. "session-initiate" or "transport-accept").
     * The name returned by this method is meant for use directly in the XMPP XML string.
     *
     * @return Returns the name of this <code>JingleAction</code> (e.g. "session-initiate" or "transport-accept").
     */
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
