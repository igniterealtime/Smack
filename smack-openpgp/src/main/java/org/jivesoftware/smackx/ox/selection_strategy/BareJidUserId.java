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
package org.jivesoftware.smackx.ox.selection_strategy;

import java.util.Iterator;

import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.jxmpp.jid.BareJid;
import org.pgpainless.key.selection.keyring.PublicKeyRingSelectionStrategy;
import org.pgpainless.key.selection.keyring.SecretKeyRingSelectionStrategy;

public class BareJidUserId {

    public static class PubRingSelectionStrategy extends PublicKeyRingSelectionStrategy<BareJid> {

        @Override
        public boolean accept(BareJid jid, PGPPublicKeyRing ring) {
            Iterator<String> userIds = ring.getPublicKey().getUserIDs();
            while (userIds.hasNext()) {
                String userId = userIds.next();
                if (userId.equals("xmpp:" + jid.toString())) {
                    return true;
                }
            }
            return false;
        }
    }

    public static class SecRingSelectionStrategy extends SecretKeyRingSelectionStrategy<BareJid> {

        @Override
        public boolean accept(BareJid jid, PGPSecretKeyRing ring) {
            Iterator<String> userIds = ring.getPublicKey().getUserIDs();
            while (userIds.hasNext()) {
                String userId = userIds.next();
                if (userId.equals("xmpp:" + jid.toString())) {
                    return true;
                }
            }
            return false;
        }
    }
}
