/**
 *
 * Copyright 2016 Fernando Ramirez
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
 * Block contact IQ class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0191.html">XEP-0191: Blocking
 *      Command</a>
 */
public class BlockContactsIQ extends IQ {

    /**
     * block element.
     */
    public static final String ELEMENT = "block";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = BlockingCommandManager.NAMESPACE;

    private final List<Jid> jids;

    /**
     * Block list IQ constructor.
     * 
     * @param jids
     */
    public BlockContactsIQ(List<Jid> jids) {
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.jids = Collections.unmodifiableList(jids);
    }

    /**
     * Get the JID.
     * 
     * @return the list of JIDs
     */
    public List<Jid> getJids() {
        return jids;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        if (jids != null) {
            for (Jid jid : jids) {
                xml.halfOpenElement("item");
                xml.attribute("jid", jid);
                xml.closeEmptyElement();
            }
        }

        return xml;
    }

}
