/**
 *
 * Copyright © 2016-2020 Florian Schmaus and Fernando Ramirez
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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.forward.packet.Forwarded;
import org.jivesoftware.smackx.mam.MamManager;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;


/**
 * A factory that creates extension elements and IQs for a specific MAM version. {@link #getSupportedMamNamespace()}
 * returns the namespace of the MAM version the elements are created for. Used by {@link MamManager}.
 */
public interface MamElementFactory {

    /**
     * Returns the MAM namespace for which this factory creates elements.
     *
     * @return the mam namespace
     */
    String getSupportedMamNamespace();

    /**
     * Returns a new {@link org.jivesoftware.smackx.mam.element.MamElements.MamResultExtension} instance suitable for
     * the MAM namespace of this factory.
     *
     * @param queryId TODO javadoc me please
     * @param id TODO javadoc me please
     * @param forwarded TODO javadoc me please
     * @return MAM result extension
     */
    MamElements.MamResultExtension newResultExtension(String queryId, String id, Forwarded<Message> forwarded);

    default MamFinIQ newFinIQ(String queryId, RSMSet rsmSet, boolean complete, boolean stable) {
        return new MamFinIQ(getSupportedMamNamespace(), queryId, rsmSet, complete, stable);
    }

    default MamPrefsIQ newPrefsIQ() {
        return new MamPrefsIQ(getSupportedMamNamespace());
    }

    default MamPrefsIQ newPrefsIQ(List<Jid> alwaysJids, List<Jid> neverJids, MamPrefsIQ.DefaultBehavior defaultBehavior) {
        return new MamPrefsIQ(getSupportedMamNamespace(), alwaysJids, neverJids, defaultBehavior);
    }

    default MamQueryIQ newQueryIQ(String queryId, String node, DataForm dataForm) {
        return new MamQueryIQ(getSupportedMamNamespace(), queryId, node, dataForm);
    }

}
