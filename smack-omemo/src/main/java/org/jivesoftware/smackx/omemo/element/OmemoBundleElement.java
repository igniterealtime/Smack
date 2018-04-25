/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Class that represents an OMEMO Bundle element.
 * TODO: Move functionality to here.
 *
 * @author Paul Schaub
 */
public abstract class OmemoBundleElement implements ExtensionElement {

    public static final String BUNDLE = "bundle";
    public static final String SIGNED_PRE_KEY_PUB = "signedPreKeyPublic";
    public static final String SIGNED_PRE_KEY_ID = "signedPreKeyId";
    public static final String SIGNED_PRE_KEY_SIG = "signedPreKeySignature";
    public static final String IDENTITY_KEY = "identityKey";
    public static final String PRE_KEYS = "prekeys";
    public static final String PRE_KEY_PUB = "preKeyPublic";
    public static final String PRE_KEY_ID = "preKeyId";

    @Override
    public abstract XmlStringBuilder toXML(String enclosingNamespace);

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof OmemoBundleElement)) {
            return false;
        }

        OmemoBundleElement otherOmemoBundleElement = (OmemoBundleElement) other;
        return toXML(null).equals(otherOmemoBundleElement.toXML(null));
    }

    @Override
    public int hashCode() {
        return this.toXML(null).hashCode();
    }
}
