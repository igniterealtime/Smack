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
