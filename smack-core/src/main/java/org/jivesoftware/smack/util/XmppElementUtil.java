/**
 *
 * Copyright 2018-2019 Florian Schmaus
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
package org.jivesoftware.smack.util;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.FullyQualifiedElement;

public class XmppElementUtil {

    public static QName getQNameFor(Class<? extends FullyQualifiedElement> fullyQualifiedElement) {
        String element, namespace;
        try {
            element = (String) fullyQualifiedElement.getField("ELEMENT").get(null);
            namespace = (String) fullyQualifiedElement.getField("NAMESPACE").get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }

        return new QName(namespace, element);
    }

}
