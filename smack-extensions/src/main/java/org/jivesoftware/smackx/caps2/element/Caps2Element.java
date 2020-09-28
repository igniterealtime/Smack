/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smackx.caps2.element;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class Caps2Element implements ExtensionElement {

    public static final String NAMESPACE = "urn:xmpp:caps";
    public static final String ELEMENT = "c";

    private Set<Caps2HashElement> hashes;

    public Caps2Element(Caps2HashElement hashElement) {
        hashes = new HashSet<>();
        hashes.add(hashElement);
    }

    public Caps2Element(Set<Caps2HashElement> hashElementSet) {
        hashes = hashElementSet;
    }

    public Set<Caps2HashElement> getHashes() {
        return hashes;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
        xml.rightAngleBracket();

        Iterator<Caps2HashElement> iterator = hashes.iterator();
        while (iterator.hasNext()) {
            xml.append(iterator.next());
        }

        xml.closeElement(ELEMENT);
        return xml;
    }

    public static class Caps2HashElement implements ExtensionElement {

        public static final String NAMESPACE = "urn:xmpp:hashes:2";
        public static final String ELEMENT = "hash";

        private final String algorithm;
        private final String hash;

        public Caps2HashElement (String algorithm, String hash) {
            this.algorithm = algorithm;
            this.hash = hash;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getHash() {
            return hash;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.attribute("algo", algorithm);
            xml.rightAngleBracket();
            xml.append(hash);
            xml.closeElement(ELEMENT);
            return xml;
        }
    }
}
