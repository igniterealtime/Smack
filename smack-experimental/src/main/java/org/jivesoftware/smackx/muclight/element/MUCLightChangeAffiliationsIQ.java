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

import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MultiUserChatLight;
import org.jivesoftware.smackx.muclight.element.MUCLightElements.UserWithAffiliationElement;

import org.jxmpp.jid.Jid;

/**
 * MUCLight change affiliations IQ class.
 *
 * @author Fernando Ramirez
 *
 */
public class MUCLightChangeAffiliationsIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.AFFILIATIONS;

    private HashMap<Jid, MUCLightAffiliation> affiliations;

    /**
     * MUCLight change affiliations IQ constructor.
     *
     * @param room TODO javadoc me please
     * @param affiliations TODO javadoc me please
     */
    public MUCLightChangeAffiliationsIQ(Jid room, HashMap<Jid, MUCLightAffiliation> affiliations) {
        super(ELEMENT, NAMESPACE);
        this.setType(Type.set);
        this.setTo(room);
        this.affiliations = affiliations;
    }

    /**
     * Get the affiliations.
     *
     * @return the affiliations
     */
    public HashMap<Jid, MUCLightAffiliation> getAffiliations() {
        return affiliations;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();

        if (affiliations != null) {
            Iterator<Map.Entry<Jid, MUCLightAffiliation>> it = affiliations.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Jid, MUCLightAffiliation> pair = it.next();
                xml.append(new UserWithAffiliationElement(pair.getKey(), pair.getValue()));
            }
        }

        return xml;
    }

}
