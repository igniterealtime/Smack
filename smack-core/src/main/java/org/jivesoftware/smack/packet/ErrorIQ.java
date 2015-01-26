/**
 *
 * Copyright © 2014 Florian Schmaus
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

import org.jivesoftware.smack.util.Objects;

public class ErrorIQ extends SimpleIQ {

    public static final String ELEMENT = XMPPError.ERROR;

    /**
     * Constructs a new error IQ.
     * <p>
     * According to RFC 6120 § 8.3.1 "4. An error stanza MUST contain an <error/> child element.", so the xmppError argument is mandatory.
     * </p>
     * @param xmppError the XMPPError (required).
     */
    public ErrorIQ(XMPPError xmppError) {
        super(ELEMENT, null);
        Objects.requireNonNull(xmppError, "XMPPError must not be null");
        setType(IQ.Type.error);
        setError(xmppError);
    }

}
