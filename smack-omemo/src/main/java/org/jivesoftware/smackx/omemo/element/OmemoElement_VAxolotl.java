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

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE_V_AXOLOTL;

/**
 * An OMEMO (PreKey)WhisperMessage element.
 *
 * @author Paul Schaub
 */
public class OmemoElement_VAxolotl extends OmemoElement {

    public static final String NAMESPACE = OMEMO_NAMESPACE_V_AXOLOTL;

    /**
     * Create a new OmemoMessageElement from a header and a payload.
     *
     * @param header  header of the message
     * @param payload payload
     */
    public OmemoElement_VAxolotl(OmemoHeaderElement_VAxolotl header, byte[] payload) {
        super(header, payload);
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }
}
