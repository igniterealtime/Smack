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

    public enum DefaultBehavior {
        always,
        never,
        roster,
        ;
    }

    /**
     * the preferences element.
     */
    public static final String ELEMENT = "prefs";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = MamElements.NAMESPACE;

    /**
     * list of always.
     */
    private final List<Jid> alwaysJids;

    /**
     * list of never.
     */
    private final List<Jid> neverJids;

    /**
     * default field.
     */
    private final DefaultBehavior defaultBehavior;

    /**
     * Construct a new MAM {@code <prefs/>} IQ retrieval request (IQ type 'get').
     */
    public MamPrefsIQ() {
        super(ELEMENT, NAMESPACE);
        alwaysJids = null;
        neverJids = null;
        defaultBehavior = null;
    }

    /**
     * MAM preferences IQ constructor.
     *
     * @param alwaysJids
     * @param neverJids
     * @param defaultBehavior
     */
    public MamPrefsIQ(List<Jid> alwaysJids, List<Jid> neverJids, DefaultBehavior defaultBehavior) {
        super(ELEMENT, NAMESPACE);
        setType(Type.set);
        this.alwaysJids = alwaysJids;
        this.neverJids = neverJids;
        this.defaultBehavior = defaultBehavior;
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
     * Get the default behavior.
     *
     * @return the default behavior.
     */
    public DefaultBehavior getDefault() {
        return defaultBehavior;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {

        if (getType().equals(IQ.Type.set) || getType().equals(IQ.Type.result)) {
            xml.attribute("default", defaultBehavior);
        }

        if (alwaysJids == null && neverJids == null) {
            xml.setEmptyElement();
            return xml;
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
