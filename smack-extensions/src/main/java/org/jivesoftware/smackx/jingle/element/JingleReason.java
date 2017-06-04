/**
 *
 * Copyright 2017 Florian Schmaus
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

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The Jingle 'reason' element.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0166.html#def-reason">XEP-0166 § 7.4</a>
 *
 */
public class JingleReason implements NamedElement {

    public static final String ELEMENT = "reason";

    public enum Reason {
        alternative_session,
        busy,
        cancel,
        connectivity_error,
        decline,
        expired,
        failed_application,
        failed_transport,
        general_error,
        gone,
        incompatible_parameters,
        media_error,
        security_error,
        success,
        timeout,
        unsupported_applications,
        unsupported_transports,
        ;

        private static final Map<String, Reason> LUT = new HashMap<>(Reason.values().length);

        static {
            for (Reason reason : Reason.values()) {
                LUT.put(reason.toString(), reason);
            }
        }

        private final String asString;

        private Reason() {
            asString = name().replace('_', '-');
        }

        @Override
        public String toString() {
            return asString;
        }

        public static Reason fromString(String string) {
            Reason reason = LUT.get(string);
            if (reason == null) {
                throw new IllegalArgumentException("Unknown reason: " + string);
            }
            return reason;
        }
    }

    private final Reason reason;

    public JingleReason(Reason reason) {
        this.reason = reason;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();

        xml.emptyElement(reason.asString);

        xml.closeElement(this);
        return xml;
    }

}
