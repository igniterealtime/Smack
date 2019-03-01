/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.mood.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * XEP-0107 can be extended with additional custom mood concretisations.
 * In order to extend Smacks implementation with a custom mood concretisation, just extend this class and overwrite
 * {@link #getElementName()} and {@link #getNamespace()} with your custom values.
 *
 * TODO: Solution for provider.
 */
public abstract class MoodConcretisation implements ExtensionElement {

    @Override
    public final XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        return new XmlStringBuilder(this).closeEmptyElement();
    }

    public String getMood() {
        return getElementName();
    }
}
