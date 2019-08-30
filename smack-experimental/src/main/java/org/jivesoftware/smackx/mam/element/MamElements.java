/**
 *
 * Copyright © 2016 Florian Schmaus and Fernando Ramirez
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
package org.jivesoftware.smackx.mam.element;

import java.util.List;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.forward.packet.Forwarded;

import org.jxmpp.jid.Jid;

/**
 * MAM elements.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez and Florian Schmaus
 *
 */
public class MamElements {

    public static final String NAMESPACE = "urn:xmpp:mam:1";

    /**
     * MAM result extension class.
     *
     * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
     *      Archive Management</a>
     *
     */
    public static class MamResultExtension implements ExtensionElement {

        /**
         * result element.
         */
        public static final String ELEMENT = "result";

        /**
         * id of the result.
         */
        private final String id;

        /**
         * the forwarded element.
         */
        private final Forwarded forwarded;

        /**
         * the query id.
         */
        private String queryId;

        /**
         * MAM result extension constructor.
         *
         * @param queryId TODO javadoc me please
         * @param id TODO javadoc me please
         * @param forwarded TODO javadoc me please
         */
        public MamResultExtension(String queryId, String id, Forwarded forwarded) {
            if (StringUtils.isEmpty(id)) {
                throw new IllegalArgumentException("id must not be null or empty");
            }
            if (forwarded == null) {
                throw new IllegalArgumentException("forwarded must no be null");
            }
            this.id = id;
            this.forwarded = forwarded;
            this.queryId = queryId;
        }

        /**
         * Get the id.
         *
         * @return the id
         */
        public String getId() {
            return id;
        }

        /**
         * Get the forwarded element.
         *
         * @return the forwarded element
         */
        public Forwarded getForwarded() {
            return forwarded;
        }

        /**
         * Get query id.
         *
         * @return the query id
         */
        public final String getQueryId() {
            return queryId;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public final String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.xmlnsAttribute(NAMESPACE);
            xml.optAttribute("queryid", getQueryId());
            xml.optAttribute("id", getId());
            xml.rightAngleBracket();

            xml.element(getForwarded());

            xml.closeElement(this);
            return xml;
        }

        public static MamResultExtension from(Message message) {
            return (MamResultExtension) message.getExtension(ELEMENT, NAMESPACE);
        }

    }

    /**
     * Always JID list element class for the MamPrefsIQ.
     *
     */
    public static class AlwaysJidListElement implements Element {

        /**
         * list of JIDs.
         */
        private final List<Jid> alwaysJids;

        /**
         * Always JID list element constructor.
         *
         * @param alwaysJids TODO javadoc me please
         */
        AlwaysJidListElement(List<Jid> alwaysJids) {
            this.alwaysJids = alwaysJids;
        }

        @Override
        public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.openElement("always");

            for (Jid jid : alwaysJids) {
                xml.element("jid", jid);
            }

            xml.closeElement("always");
            return xml;
        }
    }

    /**
     * Never JID list element class for the MamPrefsIQ.
     *
     */
    public static class NeverJidListElement implements Element {

        /**
         * list of JIDs
         */
        private List<Jid> neverJids;

        /**
         * Never JID list element constructor.
         *
         * @param neverJids TODO javadoc me please
         */
        public NeverJidListElement(List<Jid> neverJids) {
            this.neverJids = neverJids;
        }

        @Override
        public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.openElement("never");

            for (Jid jid : neverJids) {
                xml.element("jid", jid);
            }

            xml.closeElement("never");
            return xml;
        }
    }

}
