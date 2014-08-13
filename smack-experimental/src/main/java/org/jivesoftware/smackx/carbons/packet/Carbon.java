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
package org.jivesoftware.smackx.carbons.packet;

import org.jivesoftware.smack.packet.IQ;

/**
 * Carbon IQs
 */
public class Carbon {
    public static final String NAMESPACE = "urn:xmpp:carbons:2";

    public static class Enable extends IQ {
        public static final String ELEMENT = "enable";

        public Enable() {
            setType(Type.set);
        }

        @Override
        public String getChildElementXML() {
            return "<" + ELEMENT + " xmlns='" + NAMESPACE + "'/>";
        }
    }

    public static class Disable extends IQ {
        public static final String ELEMENT = "disable";

        public Disable() {
            setType(Type.set);
        }

        @Override
        public String getChildElementXML() {
            return "<" + ELEMENT + " xmlns='" + NAMESPACE + "'/>";
        }
    }
}
