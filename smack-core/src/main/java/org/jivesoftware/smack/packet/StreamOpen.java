/**
 *
 * Copyright © 2014 Florian Schmaus
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

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * 
 */
public class StreamOpen extends FullStreamElement {

    public static final String ELEMENT = "stream:stream";
    public static final String NAMESPACE = "jabber:client";
    public static final String VERSION = "1.0";

    private final String service;

    public StreamOpen(String service) {
       this.service = service;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("to", service);
        xml.attribute("xmlns:stream", "http://etherx.jabber.org/streams");
        xml.attribute("version", VERSION);
        xml.rightAngleBracket();
        return xml;
    }

}
