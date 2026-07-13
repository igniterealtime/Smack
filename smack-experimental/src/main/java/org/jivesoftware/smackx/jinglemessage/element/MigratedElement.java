/*
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jinglemessage.element;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Implementation of JingleMessage Migrated element.
 *
 * @author Eng Chong Meng
 */
public class MigratedElement implements NamedElement {
    public static final String ELEMENT = "migrated";

    public static final String ATTR_TO_ID = "to";

    private final String mUuid;

    public MigratedElement(String toId) {
        mUuid = toId;
    }


    public String getUuidd() {
        return mUuid;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(ELEMENT);
        xml.attribute(ATTR_TO_ID, getUuidd());
        xml.closeEmptyElement();
        return xml;
    }

    @Override
    public final String toString() {
        return "Migrated: To: " + getUuidd();
    }

    public int equals(MigratedElement other) {
        if (other != null) {
            String uuid = other.getUuidd();
            return getUuidd().compareTo(uuid);
        }
        return -1;
    }

    @Override
    public int hashCode() {
        return getUuidd().hashCode();
    }
}
