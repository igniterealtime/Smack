/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.discovery.element;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;

public class IoTMine extends IQ {

    public static final String ELEMENT = "mine";
    public static final String NAMESPACE = Constants.IOT_DISCOVERY_NAMESPACE;

    private final List<Tag> metaTags;
    private final boolean publicThing;

    public IoTMine(Collection<Tag> metaTags, boolean publicThing) {
        this(new ArrayList<>(metaTags), publicThing);
    }

    public IoTMine(List<Tag> metaTags, boolean publicThing) {
        super(ELEMENT, NAMESPACE);
        this.metaTags = metaTags;
        this.publicThing = publicThing;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.optBooleanAttributeDefaultTrue("public", publicThing);
        xml.rightAngleBracket();
        xml.append(metaTags);

        return xml;
    }

}
