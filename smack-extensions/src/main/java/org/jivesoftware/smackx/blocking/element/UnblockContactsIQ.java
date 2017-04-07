/**
 *
 * Copyright 2016-2017 Fernando Ramirez, Florian Schmaus
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
package org.jivesoftware.smackx.blocking.element;

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jxmpp.jid.Jid;

/**
 * Unblock contact IQ class.
 * 
 * @author Fernando Ramirez
 * @author Florian Schmaus
 * @see <a href="http://xmpp.org/extensions/xep-0191.html">XEP-0191: Blocking
 *      Command</a>
 */
public class UnblockContactsIQ extends IQ {

    /**
     * unblock element.
     */
    public static final String ELEMENT = "unblock";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = BlockingCommandManager.NAMESPACE;

    private final List<Jid> jids;

    /**
     * Unblock contacts IQ constructor.
     * 
     * @param jids
     */
    public UnblockContactsIQ(List<Jid> jids) {
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        if (jids != null) {
            this.jids = Collections.unmodifiableList(jids);
        } else {
            this.jids = null;
        }
    }

    /**
     * Constructs a new unblock IQ which will unblock <b>all</b> JIDs. 
     */
    public UnblockContactsIQ() {
        this(null);
    }

    /**
     * Get the JIDs. This may return null, which means that all JIDs should be or where unblocked.
     * 
     * @return the list of JIDs or <code>null</code>.
     */
    public List<Jid> getJids() {
        return jids;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        if (jids == null) {
            xml.setEmptyElement();
            return xml;
        }

        xml.rightAngleBracket();
        for (Jid jid : jids) {
            xml.halfOpenElement("item");
            xml.attribute("jid", jid);
            xml.closeEmptyElement();
        }

        return xml;
    }

}
