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
package org.jivesoftware.smackx.omemo.element;

import java.util.Objects;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Implementation of OmemoDevice element of an active device of a contact.
 *
 * @author Eng Chong Meng
 */
public class OmemoDeviceElement implements NamedElement {
    public static final String DEVICE = "device";

    public static final String ATTR_ID = "id";
    public static final String ATTR_LABEL = "label";
    public static final String ATTR_LABELSIG = "labelsig";

    private final int id;
    private final String label;
    private final String labelSig;

    public OmemoDeviceElement(int deviceId) {
        this(deviceId, null, null);
    }

    public OmemoDeviceElement(int device, String label, String labelSig) {
        this.id = device;
        this.label = label;
        this.labelSig = labelSig;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getLabelSig() {
        return labelSig;
    }

    @Override
    public String getElementName() {
        return DEVICE;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(DEVICE);

        xml.attribute(ATTR_ID, getId());
        xml.optAttribute(ATTR_LABEL, getLabel());
        xml.optAttribute(ATTR_LABELSIG, getLabelSig());

        xml.rightAngleBracket();
        return xml;
    }

    @Override
    public final String toString() {
        StringBuilder sb = new StringBuilder("OmemoDeviceElement: ");
        sb.append("id: ").append(getId());
        if (StringUtils.isNotEmpty(getLabel()))
            sb.append("label: ").append(getLabel());
        if (StringUtils.isNotEmpty(getLabelSig()))
            sb.append("labelSig: ").append(getLabelSig());

        return sb.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof OmemoDeviceElement) {
            OmemoDeviceElement omemoDevice = (OmemoDeviceElement) other;
            return (this.id == omemoDevice.getId())
                    && Objects.equals(this.label, omemoDevice.getLabel())
                    && Objects.equals(this.labelSig, omemoDevice.getLabelSig());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.getId();
    }
}
