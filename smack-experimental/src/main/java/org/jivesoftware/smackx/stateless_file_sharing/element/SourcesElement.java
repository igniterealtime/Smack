/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.stateless_file_sharing.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.url_address_information.element.UrlDataElement;

public class SourcesElement implements NamedElement {

    public static final String ELEMENT = "sources";

    private final List<UrlDataElement> urlDataElements = new ArrayList<>();
    private final List<ExtensionElement> otherSourceElements = new ArrayList<>();

    public SourcesElement(List<UrlDataElement> urlDataElements, List<ExtensionElement> otherSourceElements) {
        this.urlDataElements.addAll(urlDataElements);
        this.otherSourceElements.addAll(otherSourceElements);
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
        return new XmlStringBuilder(this)
                .rightAngleBracket()
                .append(getUrlDataElements())
                .append(getOtherSourceElements())
                .closeElement(this);
    }

    public List<UrlDataElement> getUrlDataElements() {
        return Collections.unmodifiableList(urlDataElements);
    }

    public List<ExtensionElement> getOtherSourceElements() {
        return Collections.unmodifiableList(otherSourceElements);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }
}
