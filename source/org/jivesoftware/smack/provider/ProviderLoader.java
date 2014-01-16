package org.jivesoftware.smack.provider;

import java.util.Collection;

/**
 * Used to load providers into the {@link ProviderManager}.
 * 
 * @author Robin Collier
 */
public interface ProviderLoader {

    /**
     * Provides the IQ provider info for the creation of IQ providers to be added to the <code>ProviderManager</code>.
     * @return The IQ provider info to load.
     */
    Collection<IQProviderInfo> getIQProviderInfo();

    /**
     * Provides the extension providers for the creation of extension providers to be added to the <code>ProviderManager</code>.
     * @return The extension provider info to load.
     */
    Collection<ExtensionProviderInfo> getExtensionProviderInfo();
}
