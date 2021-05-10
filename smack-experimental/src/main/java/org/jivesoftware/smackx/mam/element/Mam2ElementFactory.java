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

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.forward.packet.Forwarded;

/**
 * Factory that creates MAM elements for MAM version 2.
 */
public class Mam2ElementFactory implements MamElementFactory {

    public static final String MAM2_NAMESPACE = MamElements.MAM2_NAMESPACE;

    @Override
    public String getSupportedMamNamespace() {
        return MAM2_NAMESPACE;
    }

    @Override
    public MamElements.MamResultExtension newResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
        return new Mam2ResultExtension(queryId, id, forwarded);
    }

    public static class Mam2ResultExtension extends MamElements.MamResultExtension {

        public static final QName QNAME = new QName(MAM2_NAMESPACE, ELEMENT);

        /**
         * MAM result extension constructor.
         *
         * @param queryId    TODO javadoc me please
         * @param id         TODO javadoc me please
         * @param forwarded  TODO javadoc me please
         */
        public Mam2ResultExtension(String queryId, String id, Forwarded<Message> forwarded) {
            super(MAM2_NAMESPACE, queryId, id, forwarded);
        }
    }
}
