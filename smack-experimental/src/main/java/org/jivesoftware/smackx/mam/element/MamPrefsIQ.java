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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.mam.element.MamElements.AlwaysJidListElement;
import org.jivesoftware.smackx.mam.element.MamElements.NeverJidListElement;
import org.jxmpp.jid.Jid;

/**
 * MAM Preferences IQ class.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez and Florian Schmaus
 * 
 */
public class MamPrefsIQ extends IQ {

    /**
     * the preferences element.
     */
    public static final String ELEMENT = "prefs";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = MamElements.NAMESPACE;

    /**
     * true if it is a request for update preferences.
     */
    private boolean isUpdate;

    /**
     * true if it is a result preferences.
     */
    private boolean isResult;

    /**
     * list of always.
     */
    private List<Jid> alwaysJids;

    /**
     * list of never.
     */
    private List<Jid> neverJids;

    /**
     * default field.
     */
    private String defaultField;

    /**
     * MAM preferences IQ constructor.
     * 
     * @param type
     * @param alwaysJids
     * @param neverJids
     * @param defaultField
     */
    public MamPrefsIQ(Type type, List<Jid> alwaysJids, List<Jid> neverJids, String defaultField) {
        super(ELEMENT, NAMESPACE);
        this.setType(type);
        this.isUpdate = this.getType().equals(Type.set);
        this.isResult = this.getType().equals(Type.result);
        this.alwaysJids = alwaysJids;
        this.neverJids = neverJids;
        this.defaultField = defaultField;
    }

    /**
     * True if it is a request for update preferences.
     * 
     * @return the update preferences boolean
     */
    public boolean isUpdate() {
        return isUpdate;
    }

    /**
     * True if it is a result.
     * 
     * @return the result preferences boolean
     */
    public boolean isResult() {
        return isUpdate;
    }

    /**
     * Get the list of always store info JIDs.
     * 
     * @return the always list
     */
    public List<Jid> getAlwaysJids() {
        return alwaysJids;
    }

    /**
     * Get the list of never store info JIDs.
     * 
     * @return the never list
     */
    public List<Jid> getNeverJids() {
        return neverJids;
    }

    /**
     * Get the default field.
     * 
     * @return the default field
     */
    public String getDefault() {
        return defaultField;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {

        if (isUpdate || isResult) {
            xml.attribute("default", defaultField);
        }

        xml.rightAngleBracket();

        if (alwaysJids != null) {
            MamElements.AlwaysJidListElement alwaysElement = new AlwaysJidListElement(alwaysJids);
            xml.element(alwaysElement);
        }

        if (neverJids != null) {
            MamElements.NeverJidListElement neverElement = new NeverJidListElement(neverJids);
            xml.element(neverElement);
        }

        return xml;
    }

}
