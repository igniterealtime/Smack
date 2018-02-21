/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.sid.element;

import java.util.UUID;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.StringUtils;

public abstract class StableAndUniqueIdElement implements ExtensionElement {

    public static final String ATTR_ID = "id";

    private final String id;

    public StableAndUniqueIdElement() {
        this.id = UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public StableAndUniqueIdElement(String id) {
        if (StringUtils.isNullOrEmpty(id)) {
            throw new IllegalArgumentException("Argument 'id' cannot be null or empty.");
        }
        this.id = id;
    }
}
