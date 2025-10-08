/*
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smackx.muc;

import java.util.Locale;

/**
 * XEP-0045: Multi User Chat - 5.1 Roles.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0045.html#roles">XEP-0045: Multi-User-Chat - 5.1 Roles</a>
 */
public enum MUCRole {

    moderator,
    none,
    participant,
    visitor;

    public static MUCRole fromString(String string) {
        if (string == null) {
            return null;
        }
        return MUCRole.valueOf(string.toLowerCase(Locale.US));
    }
}
