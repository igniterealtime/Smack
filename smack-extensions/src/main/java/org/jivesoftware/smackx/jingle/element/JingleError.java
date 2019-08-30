/**
 *
 * Copyright 2003-2005 Jive Software, 2017 Florian Schmaus.
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

import java.util.Locale;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public final class JingleError implements ExtensionElement {

    public static String NAMESPACE = "urn:xmpp:jingle:errors:1";

    public static final JingleError OUT_OF_ORDER = new JingleError("out-of-order");

    public static final JingleError TIE_BREAK = new JingleError("tie-break");

    public static final JingleError UNKNOWN_SESSION = new JingleError("unknown-session");

    public static final JingleError UNSUPPORTED_INFO = new JingleError("unsupported-info");

    private final String errorName;

    /**
     * Creates a new error with the specified code and errorName.
     *
     * @param errorName a name describing the error.
     */
    private JingleError(final String errorName) {
        this.errorName = errorName;
    }

    /**
     * Returns the name of the Jingle error.
     *
     * @return the name of the error.
     */
    public String getMessage() {
        return errorName;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.closeEmptyElement();
        return xml;
    }

    /**
     * Returns a error instance associated with the String value.
     *
     * @param value the input String.
     * @return the jingle error instance associated with the input String.
     */
    public static JingleError fromString(String value) {
        value = value.toLowerCase(Locale.US);
        switch (value) {
        case "out-of-order":
            return OUT_OF_ORDER;
        case "unknown-session":
            return UNKNOWN_SESSION;
        case "tie-break":
            return TIE_BREAK;
        case "unsupported-info":
            return UNSUPPORTED_INFO;
        default:
            throw new IllegalArgumentException();
        }
    }

    @Override
    public String toString() {
        return getMessage();
    }

    @Override
    public String getElementName() {
        return errorName;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

}
