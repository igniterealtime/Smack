/**
 *
 * Copyright © 2016-2021 Florian Schmaus and Frank Matheron
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
package org.jivesoftware.smackx.mam.element;

import java.util.List;
import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

class MamV1ElementFactory implements MamElementFactory {

    @Override
    public MamElements.MamResultExtension newResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
        return new MamV1ResultExtension(queryId, id, forwarded);
    }

    @Override
    public MamFinIQ newFinIQ(String queryId, RSMSet rsmSet, boolean complete, boolean stable) {
        return new MamFinIQ(MamVersion.MAM1, queryId, rsmSet, complete, stable);
    }

    @Override
    public MamPrefsIQ newPrefsIQ(List<Jid> alwaysJids, List<Jid> neverJids, MamPrefsIQ.DefaultBehavior defaultBehavior) {
        return new MamPrefsIQ(MamVersion.MAM1, alwaysJids, neverJids, defaultBehavior);
    }

    @Override
    public MamPrefsIQ newPrefsIQ() {
        return new MamPrefsIQ(MamVersion.MAM1);
    }

    @Override
    public MamQueryIQ newQueryIQ(String queryId, String node, DataForm dataForm) {
        return new MamQueryIQ(MamVersion.MAM1, queryId, node, dataForm);
    }

    public static class MamV1ResultExtension extends MamElements.MamResultExtension {
        /**
         * The qualified name of the MAM result extension element.
         */
        public static final QName QNAME = new QName(MamVersion.MAM1.getNamespace(), ELEMENT);

        MamV1ResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
            super(MamVersion.MAM1, queryId, id, forwarded);
        }
    }
}
