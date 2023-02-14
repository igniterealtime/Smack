/**
 *
 * Copyright 2018-2021 Florian Schmaus
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

public final class StreamClose extends AbstractStreamClose {

    public static final StreamClose INSTANCE = new StreamClose();

    public static final String STRING = "</" + StreamOpen.ELEMENT + ">";

    private StreamClose() {
    }

    @Override
    public String toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return "</" + getElementName() + '>';
    }

    @Override
    public String getNamespace() {
        // Closing XML tags do never explicitly state their namespace.
        return "(none)";
    }

    @Override
    public String getElementName() {
        return StreamOpen.ELEMENT;
    }

    @Override
    public String toString() {
        return STRING;
    }
}
