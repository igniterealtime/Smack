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

import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.blocking.BlockingCommandManager;
import org.jxmpp.jid.Jid;

/**
 * Block list IQ class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0191.html">XEP-0191: Blocking
 *      Command</a>
 */
public class BlockListIQ extends IQ {

    /**
     * block list element.
     */
    public static final String ELEMENT = "blocklist";

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
    public BlockListIQ(List<Jid> jids) {
        super(ELEMENT, NAMESPACE);
        this.jids = jids;
    }

    /**
     * Block list IQ constructor.
     */
    public BlockListIQ() {
        this(null);
    }

    /**
     * Get the JIDs.
     * 
     * @return the JIDs
     */
    public List<Jid> getJids() {
        return jids;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {

        if (jids == null) {
            xml.setEmptyElement();

        } else {
            xml.rightAngleBracket();

            for (Jid jid : jids) {
                xml.halfOpenElement("item");
                xml.attribute("jid", jid);
                xml.closeEmptyElement();
            }
        }

        return xml;
    }

}
