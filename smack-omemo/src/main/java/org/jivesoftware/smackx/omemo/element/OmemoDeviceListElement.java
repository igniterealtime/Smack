/**
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
package org.jivesoftware.smackx.omemo.element;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * A OMEMO device list update containing the IDs of all active devices of a contact.
 *
 * @author Paul Schaub
 */
public abstract class OmemoDeviceListElement implements ExtensionElement {

    public static final String DEVICE = "device";
    public static final String ID = "id";
    public static final String LIST = "list";

    /**
     * Unmodifiable set of device IDs.
     */
    private final Set<Integer> deviceIds;

    public OmemoDeviceListElement(Set<Integer> deviceIds) {
        deviceIds = Objects.requireNonNull(deviceIds);
        this.deviceIds = Collections.unmodifiableSet(deviceIds);
    }

    public Set<Integer> getDeviceIds() {
        return deviceIds;
    }

    public Set<Integer> copyDeviceIds() {
        return new HashSet<>(deviceIds);
    }

    @Override
    public String getElementName() {
        return LIST;
    }

    @Override
    public final XmlStringBuilder toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();

        for (Integer id : deviceIds) {
            sb.halfOpenElement(DEVICE).attribute(ID, id).closeEmptyElement();
        }

        sb.closeElement(this);
        return sb;
    }

    @Override
    public final String toString() {
        String out = "OmemoDeviceListElement[";
        for (int i : deviceIds) {
            out += i + ",";
        }
        return out.substring(0, out.length() - 1) + "]";
    }
}
