/**
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

import java.util.HashMap;

/**
 * User Avatar metadata pointer model class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/xep-0084.html">XEP-0084: User
 *      Avatar</a>
 */
public class MetadataPointer {

    private final String namespace;
    private final HashMap<String, Object> fields;

    /**
     * Metadata Pointer constructor.
     * 
     * @param namespace
     * @param fields
     */
    public MetadataPointer(String namespace, HashMap<String, Object> fields) {
        this.namespace = namespace;
        this.fields = fields;
    }

    /**
     * Get the namespace.
     * 
     * @return the namespace
     */
    public String getNamespace() {
        return namespace;
    }

    /**
     * Get the fields.
     * 
     * @return the fields
     */
    public HashMap<String, Object> getFields() {
        return fields;
    }

}
