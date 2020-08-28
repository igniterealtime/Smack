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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.FullyQualifiedElement;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.provider.ProviderManager;

import org.jxmpp.util.cache.LruCache;

public class XmppElementUtil {

    private static final LruCache<Class<? extends FullyQualifiedElement>, QName> CLASS_TO_QNAME_CACHE = new LruCache<>(512);

    public static final Logger LOGGER = Logger.getLogger(XmppElementUtil.class.getName());

    public static QName getQNameFor(Class<? extends FullyQualifiedElement> fullyQualifiedElement) {
        QName qname = CLASS_TO_QNAME_CACHE.get(fullyQualifiedElement);
        if (qname != null) {
            return qname;
        }

        try {
            Object qnameObject = fullyQualifiedElement.getField("QNAME").get(null);
            if (QName.class.isAssignableFrom(qnameObject.getClass())) {
                qname = (QName) qnameObject;
                CLASS_TO_QNAME_CACHE.put(fullyQualifiedElement, qname);
                return qname;
            }
            LOGGER.warning("The QNAME field of " + fullyQualifiedElement + " is not of type QNAME.");
        } catch (NoSuchFieldException e) {
            LOGGER.finer("The " + fullyQualifiedElement + " has no static QNAME field. Consider adding one.");
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
            throw new IllegalArgumentException("The " + fullyQualifiedElement + " has no ELEMENT, NAMESPACE or QNAME member. Consider adding QNAME", e);
        }

        qname = new QName(namespace, element);
        CLASS_TO_QNAME_CACHE.put(fullyQualifiedElement, qname);
        return qname;
    }

    public static <E extends ExtensionElement> List<E> getElementsFrom(
                    MultiMap<QName, ExtensionElement> elementMap, Class<E> extensionElementClass) {
        QName qname = XmppElementUtil.getQNameFor(extensionElementClass);

        List<ExtensionElement> extensionElements = elementMap.getAll(qname);

        if (extensionElements.isEmpty()) {
            return Collections.emptyList();
        }

        List<E> res = new ArrayList<>(extensionElements.size());
        for (ExtensionElement extensionElement : extensionElements) {
            E e = castOrThrow(extensionElement, extensionElementClass);
            res.add(e);
        }
        return res;
    }

    public static <E extends ExtensionElement> E castOrThrow(ExtensionElement extensionElement, Class<E> extensionElementClass) {
        if (!extensionElementClass.isInstance(extensionElement)) {
            final QName qname = getQNameFor(extensionElementClass);

            final String detailMessage;
            if (extensionElement instanceof StandardExtensionElement) {
                detailMessage = "because there is no according extension element provider registered with ProviderManager for "
                                + qname
                                + ". WARNING: This indicates a serious problem with your Smack setup, probably causing Smack not being able to properly initialize itself.";
            } else {
                Object provider = ProviderManager.getExtensionProvider(qname);
                detailMessage = "because there is an inconsistency with the provider registered with ProviderManager: the active provider for "
                                + qname + " '" + provider.getClass()
                                + "' does not return instances of type " + extensionElementClass
                                + ", but instead returns instances of type " + extensionElement.getClass() + ".";
            }

            String message = "Extension element is not of expected class '" + extensionElementClass.getName() + "', "
                            + detailMessage;
            throw new IllegalStateException(message);
        }

        return extensionElementClass.cast(extensionElement);
    }

}
