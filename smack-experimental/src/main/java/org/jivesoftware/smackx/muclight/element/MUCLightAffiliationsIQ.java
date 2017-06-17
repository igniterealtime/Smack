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
 * MUC Light affiliations response IQ class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightAffiliationsIQ extends IQ {

    public static final String ELEMENT = QUERY_ELEMENT;
    public static final String NAMESPACE = MultiUserChatLight.NAMESPACE + MultiUserChatLight.AFFILIATIONS;

    private final String version;
    private HashMap<Jid, MUCLightAffiliation> affiliations;

    /**
     * MUC Light affiliations response IQ constructor.
     * 
     * @param version
     * @param affiliations
     */
    public MUCLightAffiliationsIQ(String version, HashMap<Jid, MUCLightAffiliation> affiliations) {
        super(ELEMENT, NAMESPACE);
        this.version = version;
        this.affiliations = affiliations;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.rightAngleBracket();
        xml.optElement("version", version);

        Iterator<Map.Entry<Jid, MUCLightAffiliation>> it = affiliations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Jid, MUCLightAffiliation> pair = it.next();
            xml.element(new UserWithAffiliationElement(pair.getKey(), pair.getValue()));
        }

        return xml;
    }

    /**
     * Returns the version.
     * 
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * Returns the room affiliations.
     * 
     * @return the affiliations of the room
     */
    public HashMap<Jid, MUCLightAffiliation> getAffiliations() {
        return affiliations;
    }

}
