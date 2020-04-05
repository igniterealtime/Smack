/**
 *
 * Copyright 2018-2020 Florian Schmaus
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.FullyQualifiedElement;

public class XmppElementUtil {

    public static final Logger LOGGER = Logger.getLogger(XmppElementUtil.class.getName());

    public static QName getQNameFor(Class<? extends FullyQualifiedElement> fullyQualifiedElement) {
        try {
            Object qnameObject = fullyQualifiedElement.getField("QNAME").get(null);
            if (QName.class.isAssignableFrom(qnameObject.getClass())) {
                return (QName) qnameObject;
            }
            LOGGER.warning("The QNAME field of " + fullyQualifiedElement + " is not of type QNAME.");
        } catch (NoSuchFieldException e) {
            LOGGER.finer("The class " + fullyQualifiedElement + " has no static QNAME field. Consider adding one.");
            // Proceed to fallback strategy.
        } catch (IllegalArgumentException | IllegalAccessException | SecurityException e) {
            throw new IllegalArgumentException(e);
        }

        String element, namespace;
        try {
            element = (String) fullyQualifiedElement.getField("ELEMENT").get(null);
            namespace = (String) fullyQualifiedElement.getField("NAMESPACE").get(null);
        }
        catch (IllegalArgumentException | IllegalAccessException | NoSuchFieldException | SecurityException e) {
            throw new IllegalArgumentException("The class" + fullyQualifiedElement + " has no ELEMENT, NAMSEPACE or QNAME member. Consider adding QNAME", e);
        }

        return new QName(namespace, element);
    }

    public static <E extends FullyQualifiedElement, R extends FullyQualifiedElement> List<R> getElementsFrom(
                    MultiMap<QName, E> elementMap, Class<R> extensionElementClass) {
        QName qname = XmppElementUtil.getQNameFor(extensionElementClass);

        List<E> extensionElements = elementMap.getAll(qname);

        if (extensionElements.isEmpty()) {
            return Collections.emptyList();
        }

        List<R> res = new ArrayList<>(extensionElements.size());
        for (E extensionElement : extensionElements) {
            R e = extensionElementClass.cast(extensionElement);
            res.add(e);
        }
        return res;
    }
}
