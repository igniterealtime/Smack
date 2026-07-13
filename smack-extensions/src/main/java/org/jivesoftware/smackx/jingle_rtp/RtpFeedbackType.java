/*
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx.jingle_rtp;

import org.jivesoftware.smackx.jingle_rtp.element.RtcpFb;

/**
 * An enumeration containing allowed types for {@link RtcpFb}.
 * XEP-0293: Jingle RTP Feedback Negotiation 1.0.2 (2022-08-26)
 *
 * @author Emil Ivov
 * @see <a href="https://xmpp.org/extensions/xep-0176.html#protocol-syntax">XEP-0176 § 5.3 Syntax</a>
 */
public enum RtpFeedbackType {
    // ack sub-type:
    ack,

    // Reference Picture Selection Indication
    rpsi,

    // Application-specific: Application Layer Feedback Messages
    app,

    // nack sub-type:
    nack,

    // Slice Loss Indication
    sli,

    // Picture Loss Indication
    pli,
    // rpsi,
    // app,

    // Receiver Active Indicator
    rai,

    // ccm sub-type: Codec Control Messages
    ccm,

    // Freeze Picture Request/Release
    fir,

    // Temporary Maximum Media Stream Bit Rate Request
    tmmbr,

    // Temporal-Spatial Trade-off Request
    tstr,

    // Video Back Channel Message
    vbcm,

    // app sub-type depends on the application:
    // app;
    ;

    RtpFeedbackType() {
    }

    public static RtpFeedbackType fromString(String name) {
        for (RtpFeedbackType t : RtpFeedbackType.values()) {
            if (t.toString().equals(name)) {
                return t;
            }
        }
        throw new IllegalArgumentException("Illegal type: " + name);
    }
}
