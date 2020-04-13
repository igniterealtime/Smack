/**
 *
 * Copyright 2020 Florian Schmaus
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
package org.jivesoftware.smack.filter;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.XmppElementUtil;

public class ExtensionElementFilter<E extends ExtensionElement> implements StanzaFilter {

    private final Class<E> extensionElementClass;
    private final QName extensionElementQName;

    protected ExtensionElementFilter(Class<E> extensionElementClass) {
        this.extensionElementClass = extensionElementClass;
        extensionElementQName = XmppElementUtil.getQNameFor(extensionElementClass);
    }

    @Override
    public final boolean accept(Stanza stanza) {
        ExtensionElement extensionElement = stanza.getExtension(extensionElementQName);
        if (extensionElement == null) {
            return false;
        }

        if (!extensionElementClass.isInstance(extensionElement)) {
            return false;
        }

        E specificExtensionElement = extensionElementClass.cast(extensionElement);
        return accept(specificExtensionElement);
    }

    public boolean accept(E extensionElement) {
        return true;
    }
}
