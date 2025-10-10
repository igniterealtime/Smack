/*
 *
 * Copyright 2017 Fernando Ramirez
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
package org.jivesoftware.smackx.avatar;

import java.util.Map;

import org.jivesoftware.smack.util.StringUtils;

/**
 * User Avatar metadata pointer model class.
 * A pointer element is used to point to an avatar which is not published via PubSub or HTTP, but provided by a
 * third-party service.
 *
 * @author Fernando Ramirez
 * @see <a href="https://xmpp.org/extensions/xep-0084.html">XEP-0084: User Avatar</a>
 */
public class MetadataPointer {

    private final String namespace;
    private final Map<String, Object> fields;

    /**
     * Metadata Pointer constructor.
     *
     * The following example
     * <pre>
     * {@code
     * <pointer>
     *     <x xmlns='http://example.com/virtualworlds'>
     *         <game>Ancapistan</game>
     *         <character>Kropotkin</character>
     *     </x>
     * </pointer>
     * }
     * </pre>
     * can be created by constructing the object like this:
     * <pre>
     * {@code
     *     Map fields = new HashMap<>();
     *     fields.add("game", "Ancapistan");
     *     fields.add("character", "Kropotkin");
     *     MetadataPointer pointer = new MetadataPointer("http://example.com/virtualworlds", fields);
     * }
     * </pre>
     *
     * @param namespace namespace of the child element of the metadata pointer.
     * @param fields fields of the child element as key, value pairs.
     */
    public MetadataPointer(String namespace, Map<String, Object> fields) {
        this.namespace = StringUtils.requireNotNullNorEmpty(namespace, "Namespace MUST NOT be null, nor empty.");
        this.fields = fields;
    }

    /**
     * Get the namespace of the pointers child element.
     *
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the fields of the pointers child element.
     *
     * @return the fields
     */
    public Map<String, Object> getFields() {
        return fields;
    }

}
