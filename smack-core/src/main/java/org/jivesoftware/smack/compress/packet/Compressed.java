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
package org.jivesoftware.smack.compress.packet;

import org.jivesoftware.smack.packet.FullStreamElement;

public class Compressed extends FullStreamElement {

    public static final String ELEMENT = "compressed";
    public static final String NAMESPACE = Compress.NAMESPACE;

    public static final Compressed INSTANCE = new Compressed();

    private Compressed() {
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String toXML() {
        return '<' + ELEMENT + " xmlns='" + NAMESPACE + "'/>";
    }
}
