/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo.elements;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.util.HashSet;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.ID;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.List.DEVICE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.List.LIST;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE;

/**
 * A OMEMO device list update containing the ids of all active devices of a contact
 *
 * @author Paul Schaub
 */
public class OmemoDeviceListElement extends HashSet<Integer> implements ExtensionElement {

    private static final long serialVersionUID = 635212332059449259L;

    @Override
    public String getElementName() {
        return LIST;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();

        for (Integer id : this) {
            sb.halfOpenElement(DEVICE).attribute(ID, id).closeEmptyElement();
        }

        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return OMEMO_NAMESPACE;
    }

    @Override
    public String toString() {
        String out = "OmemoDeviceListElement[";
        for (int i : this) {
            out += i + ",";
        }
        return out.substring(0, out.length() - 1) + "]";
    }
}
