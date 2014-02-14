package org.jivesoftware.smack.provider;

import org.jivesoftware.smack.packet.IQ;

/**
 * Defines the information required to register an IQ Provider with the {@link ProviderManager} when using the
 * {@link ProviderLoader}.
 * 
 * @author Robin Collier
 *
 */
public final class IQProviderInfo extends AbstractProviderInfo {
    
    /**
     * Defines an IQ provider which implements the <code>IQProvider</code> interface.
     * 
     * @param elementName Element that provider parses.
     * @param namespace Namespace that provider parses.
     * @param iqProvider The provider implementation.
     */
    public IQProviderInfo(String elementName, String namespace, IQProvider iqProvider) {
        super(elementName, namespace, iqProvider);
    }

    /**
     * Defines an IQ class which can be used as a provider via introspection.
     * 
     * @param elementName Element that provider parses.
     * @param namespace Namespace that provider parses.
     * @param iqProviderClass The IQ class being parsed.
     */
    public IQProviderInfo(String elementName, String namespace, Class<? extends IQ> iqProviderClass) {
        super(elementName, namespace, iqProviderClass);
    }
}
