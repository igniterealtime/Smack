/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack.serverless;

import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.caps.packet.CapsExtension;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;

public class LLEntityCapsManager extends EntityCapsManager {
    private LLEntityCapsManager() {
        throw new UnsupportedOperationException();
    }

    static void addDiscoverInfo(String jid, DiscoverInfo discoverInfo) {
        CapsExtension capsExtension = CapsExtension.from(discoverInfo);
        if (capsExtension == null) {
            return;
        }
        addCapsExtensionInfo(jid, capsExtension);
    }
}
