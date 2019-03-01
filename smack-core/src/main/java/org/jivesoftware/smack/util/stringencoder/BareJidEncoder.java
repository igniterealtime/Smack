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
package org.jivesoftware.smack.util.stringencoder;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public abstract class BareJidEncoder implements StringEncoder<BareJid> {

    @Deprecated
    public static class LegacyEncoder extends BareJidEncoder {

        @Override
        public String encode(BareJid jid) {
            return jid.toString();
        }

        @Override
        public BareJid decode(String string) {
            try {
                return JidCreate.bareFrom(string);
            } catch (XmppStringprepException e) {
                throw new IllegalArgumentException("BareJid cannot be decoded.", e);
            }
        }
    }

    public static class UrlSafeEncoder extends BareJidEncoder {

        @Override
        public String encode(BareJid jid) {
            return jid.asUrlEncodedString();
        }

        @Override
        public BareJid decode(String string) {
            try {
                return JidCreate.bareFromUrlEncoded(string);
            } catch (XmppStringprepException e) {
                throw new IllegalArgumentException("BareJid cannot be decoded.", e);
            }
        }
    }
}
