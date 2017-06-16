/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smack.isr.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.sm.packet.StreamManagement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * XEP-xxxx: Instant Stream Resumption.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx: Instant
 *      Stream Resumption</a>
 * 
 */
public class InstantStreamResumption {

    public static final String NAMESPACE = "urn:xmpp:isr:0";

    public static final String NAMESPACE_PREFIX = "isr";

    /**
     * Instant Stream Resumption feature.
     * 
     * @author Fernando Ramirez
     * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx:
     *      Instant Stream Resumption</a>
     * 
     */
    public static final class InstantStreamResumptionFeature implements ExtensionElement {

        public static final String ELEMENT = "isr";
        public static final InstantStreamResumptionFeature INSTANCE = new InstantStreamResumptionFeature();

        private InstantStreamResumptionFeature() {
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
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.closeEmptyElement();
            return xml;
        }
    }

    /**
     * Instant Stream Resumption enabled nonza.
     * 
     * @author Fernando Ramirez
     * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx:
     *      Instant Stream Resumption</a>
     * 
     */
    public static class Enabled implements Nonza {

        public static final String ELEMENT = "enabled";

        private final String key;
        private final String location;

        /**
         * Enabled nonza constructor.
         * 
         * @param key
         * @param location
         */
        public Enabled(String key, String location) {
            this.key = key;
            this.location = location;
        }

        /**
         * Enabled nonza constructor.
         * 
         * @param key
         */
        public Enabled(String key) {
            this(key, null);
        }

        /**
         * Get the key.
         * 
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * Get the location.
         * 
         * @return the location
         */
        public String getLocation() {
            return location;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return StreamManagement.NAMESPACE;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(ELEMENT);
            xml.xmlnsAttribute(StreamManagement.NAMESPACE);
            xml.attribute("xmlns:isr", NAMESPACE);
            xml.attribute("isr:key", key);
            xml.optAttribute("isr:location", location);
            xml.closeEmptyElement();
            return xml;
        }

    }

    /**
     * Instant Stream Resumption resume nonza.
     * 
     * @author Fernando Ramirez
     * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx:
     *      Instant Stream Resumption</a>
     * 
     */
    public static class InstResume implements Nonza {

        public static final String ELEMENT = "inst-resume";

        private final String prevId;
        private final long handledCount;
        private final String hash;
        private final String algo;

        /**
         * Instant resume nonza constructor.
         * 
         * @param prevId
         * @param handledCount
         * @param hash
         * @param algo
         */
        public InstResume(String prevId, long handledCount, String hash, String algo) {
            this.prevId = prevId;
            this.handledCount = handledCount;
            this.hash = hash;
            this.algo = algo;
        }

        /**
         * Get the previous id.
         * 
         * @return the previous id
         */
        public String getPrevId() {
            return prevId;
        }

        /**
         * Get the handled count.
         * 
         * @return the handled count
         */
        public long getHandledCount() {
            return handledCount;
        }

        /**
         * Get the hash.
         * 
         * @return the hash
         */
        public String getHash() {
            return hash;
        }

        /**
         * Get the algorithm.
         * 
         * @return the algorithm
         */
        public String getAlgo() {
            return algo;
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
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("previd", prevId);
            xml.optAttribute("h", Long.toString(handledCount));
            xml.rightAngleBracket();

            xml.openElement("hmac");

            xml.element(new HashElement(hash, algo));

            xml.closeElement("hmac");
            xml.closeElement(this);
            return xml;
        }

    }

    /**
     * Instant Stream Resumption resumed nonza.
     * 
     * @author Fernando Ramirez
     * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx:
     *      Instant Stream Resumption</a>
     * 
     */
    public static class InstResumed implements Nonza {

        public static final String ELEMENT = "inst-resumed";

        private final String key;
        private final long handledCount;
        private final String hash;
        private final String algo;

        /**
         * Instant resumed nonza constructor.
         * 
         * @param key
         * @param handledCount
         * @param hash
         * @param algo
         */
        public InstResumed(String key, long handledCount, String hash, String algo) {
            this.key = key;
            this.handledCount = handledCount;
            this.hash = hash;
            this.algo = algo;
        }

        /**
         * Instant resumed nonza constructor.
         * 
         * @param key
         * @param hash
         * @param algo
         */
        public InstResumed(String key, String hash, String algo) {
            this(key, -1, hash, algo);
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        /**
         * Get the key.
         * 
         * @return the key
         */
        public String getKey() {
            return key;
        }

        /**
         * Get the handled count.
         * 
         * @return the handled count
         */
        public long getHandledCount() {
            return handledCount;
        }

        /**
         * Get the hash.
         * 
         * @return the hash
         */
        public String getHash() {
            return hash;
        }

        /**
         * Get the algorithm.
         * 
         * @return the algorithm
         */
        public String getAlgo() {
            return algo;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.optAttribute("key", key);
            if (handledCount > 0) {
                xml.optAttribute("h", Long.toString(handledCount));
            }
            xml.rightAngleBracket();

            xml.openElement("hmac");

            xml.element(new HashElement(hash, algo));

            xml.closeElement("hmac");
            xml.closeElement(this);
            return xml;
        }

    }

    /**
     * Hash element.
     * 
     * @author Fernando Ramirez
     *
     */
    public static class HashElement implements ExtensionElement {

        public static final String ELEMENT = "hash";
        public static final String NAMESPACE = "urn:xmpp:hashes:1";

        private final String hash;
        private final String algo;

        /**
         * Hash element constructor.
         * 
         * @param hash
         * @param algo
         */
        public HashElement(String hash, String algo) {
            this.hash = hash;
            this.algo = algo;
        }

        /**
         * Get the hash.
         * 
         * @return the hash.
         */
        public String getHash() {
            return hash;
        }

        /**
         * Get the algorithm.
         * 
         * @return the algorithm
         */
        public String getAlgo() {
            return algo;
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
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.attribute("algo", algo);
            xml.rightAngleBracket();
            xml.escape(hash);
            xml.closeElement(this);
            return xml;
        }

    }

    /**
     * Instant Stream Resumption failed nonza.
     * 
     * @author Fernando Ramirez
     * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx:
     *      Instant Stream Resumption</a>
     * 
     */
    public static class Failed implements Nonza {

        public static final String ELEMENT = "failed";

        private final long handledCount;

        /**
         * Failed nonza constructor.
         * 
         * @param handledCount
         */
        public Failed(long handledCount) {
            this.handledCount = handledCount;
        }

        /**
         * Failed nonza constructor.
         */
        public Failed() {
            this(-1);
        }

        /**
         * Get the handled count.
         * 
         * @return the handled count
         */
        public long getHandledCount() {
            return handledCount;
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
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            if (handledCount > 0) {
                xml.optAttribute("h", Long.toString(handledCount));
            }
            xml.closeEmptyElement();
            return xml;
        }

    }

}
