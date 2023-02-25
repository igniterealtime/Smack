/**
 *
 * Copyright 2020 Aditya Borikar, 2021 Florian Schmaus.
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
package org.jivesoftware.smack.c2s;

import org.jivesoftware.smack.packet.AbstractStreamClose;
import org.jivesoftware.smack.packet.AbstractStreamOpen;

import org.jxmpp.jid.DomainBareJid;

public interface StreamOpenAndCloseFactory {

    AbstractStreamOpen createStreamOpen(DomainBareJid to, CharSequence from, String id, String lang);

    AbstractStreamClose createStreamClose();

}
