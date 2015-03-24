/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.muc.packet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;

/**
 * IQ stanza(/packet) that serves for kicking users, granting and revoking voice, banning users, 
 * modifying the ban list, granting and revoking membership and granting and revoking 
 * moderator privileges. All these operations are scoped by the 
 * 'http://jabber.org/protocol/muc#admin' namespace.
 * 
 * @author Gaston Dombiak
 */
public class MUCAdmin extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MUCInitialPresence.NAMESPACE + "#admin";

    private final List<MUCItem> items = new ArrayList<MUCItem>();

    public MUCAdmin() {
        super(ELEMENT, NAMESPACE);
    }

    /**
     * Returns a List of item childs that holds information about roles, affiliation,
     * jids and nicks.
     * 
     * @return a List of item childs that holds information about roles, affiliation,
     *          jids and nicks.
     */
    public List<MUCItem> getItems() {
        synchronized (items) {
            return Collections.unmodifiableList(new ArrayList<MUCItem>(items));
        }
    }

    /**
     * Adds an item child that holds information about roles, affiliation, jids and nicks.
     * 
     * @param item the item child that holds information about roles, affiliation, jids and nicks.
     */
    public void addItem(MUCItem item) {
        synchronized (items) {
            items.add(item);
        }
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        synchronized (items) {
            for (MUCItem item : items) {
                xml.append(item.toXML());
            }
        }

        return xml;
    }
}
