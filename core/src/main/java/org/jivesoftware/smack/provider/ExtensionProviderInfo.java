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
