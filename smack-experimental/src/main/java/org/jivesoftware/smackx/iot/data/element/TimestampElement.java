/**
 *
 * Copyright © 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.data.element;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class TimestampElement implements NamedElement {

    public static final String ELEMENT = "timestamp";

    private final Date date;
    private final List<? extends IoTDataField> fields;

    public TimestampElement(Date date, List<? extends IoTDataField> fields) {
        this.date = date;
        this.fields = Collections.unmodifiableList(fields);
    }

    public List<? extends IoTDataField> getDataFields() {
        return fields;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("value", date);
        xml.rightAngleBracket();

        xml.append(fields);

        xml.closeElement(this);
        return xml;
    }

}
