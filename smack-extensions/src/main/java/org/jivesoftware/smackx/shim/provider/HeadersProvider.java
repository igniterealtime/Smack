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
package org.jivesoftware.smackx.shim.provider;

import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smackx.shim.packet.Header;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;

/**
 * Parses the headers element as defined in <a href="http://xmpp.org/extensions/xep-0131">Stanza Headers and Internet Metadata (SHIM)</a>.
 * 
 * @author Robin Collier
 */
public class HeadersProvider extends EmbeddedExtensionProvider<HeadersExtension> {
    public static final HeadersProvider INSTANCE = new HeadersProvider();

    @SuppressWarnings("unchecked")
    @Override
    protected HeadersExtension createReturnExtension(String currentElement, String currentNamespace,
                    Map<String, String> attributeMap, List<? extends ExtensionElement> content) {
        return new HeadersExtension((List<Header>) content);
    }

}
