/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack.provider;

/**
 * Defines the information required to register a packet extension Provider with the {@link ProviderManager} when using the
 * {@link ProviderLoader}.
 * 
 * @author Robin Collier
 *
 */
public final class ExtensionProviderInfo extends AbstractProviderInfo {

    /**
     * Defines an extension provider which implements the <code>PacketExtensionProvider</code> interface.
     * 
     * @param elementName Element that provider parses.
     * @param namespace Namespace that provider parses.
     * @param extProvider The provider implementation.
     */
    public ExtensionProviderInfo(String elementName, String namespace, PacketExtensionProvider extProvider) {
        super(elementName, namespace, extProvider);
    }

    /**
     * Defines an extension provider which is adheres to the JavaBean spec for parsing the extension.
     * 
     * @param elementName Element that provider parses.
     * @param namespace Namespace that provider parses.
     * @param beanClass The provider bean class.
     */
    public ExtensionProviderInfo(String elementName, String namespace, Class<?> beanClass) {
        super(elementName, namespace, beanClass);
    }
}
