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
package org.jivesoftware.smackx.muclight.element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.element.MUCLightElements.BlockingElement;
import org.jxmpp.jid.Jid;

/**
 * MUC Light blocking IQ class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightBlockingIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.BLOCKING;

    private final HashMap<Jid, Boolean> rooms;
    private final HashMap<Jid, Boolean> users;

    /**
     * MUC Light blocking IQ constructor.
     * 
     * @param rooms
     * @param users
     */
    public MUCLightBlockingIQ(HashMap<Jid, Boolean> rooms, HashMap<Jid, Boolean> users) {
        super(ELEMENT, NAMESPACE);
        this.rooms = rooms;
        this.users = users;
    }

    /**
     * Get rooms JIDs with booleans (true if allow, false if deny).
     * 
     * @return the rooms JIDs with booleans (true if allow, false if deny)
     */
    public HashMap<Jid, Boolean> getRooms() {
        return rooms;
    }

    /**
     * Get users JIDs with booleans (true if allow, false if deny).
     * 
     * @return the users JIDs with booleans (true if allow, false if deny)
     */
    public HashMap<Jid, Boolean> getUsers() {
        return users;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        if (rooms != null) {
            parseBlocking(xml, rooms, true);
        }

        if (users != null) {
            parseBlocking(xml, users, false);
        }

        return xml;
    }

    private void parseBlocking(IQChildElementXmlStringBuilder xml, HashMap<Jid, Boolean> map, boolean isRoom) {
        Iterator<Map.Entry<Jid, Boolean>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Jid, Boolean> pair = it.next();
            xml.element(new BlockingElement(pair.getKey(), pair.getValue(), isRoom));
        }
    }

}
