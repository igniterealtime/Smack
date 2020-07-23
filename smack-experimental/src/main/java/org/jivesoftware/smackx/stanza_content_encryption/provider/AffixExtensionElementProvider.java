/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.stanza_content_encryption.provider;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.stanza_content_encryption.element.AffixExtensionElement;

/**
 * Abstract class that needs to be extended by provider classes that parse out affix extension elements.
 *
 * @param <AE> affix extension element.
 */
public abstract class AffixExtensionElementProvider<AE extends AffixExtensionElement> extends ExtensionElementProvider<AE> {

}
