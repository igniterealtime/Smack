/*
 *
 * Copyright 2021 Paul Schaub
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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.StringUtils;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.JidTestUtil;

public class AvatarMetadataStoreTest {

    @Test
    public void testStoreHasAvatarAvailable() {
        AvatarMetadataStore store = new MemoryAvatarMetadataStore();
        EntityBareJid exist = JidTestUtil.BARE_JID_1;
        EntityBareJid notExist = JidTestUtil.BARE_JID_2;

        for (String itemId : getRandomIds(10)) {
            assertFalse(store.hasAvatarAvailable(exist, itemId));
            store.setAvatarAvailable(exist, itemId);
            assertTrue(store.hasAvatarAvailable(exist, itemId));
            assertFalse(store.hasAvatarAvailable(notExist, itemId));
        }
    }

    private static List<String> getRandomIds(int len) {
        List<String> ids = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
            ids.add(StringUtils.randomString(14));
        }
        return ids;
    }
}
