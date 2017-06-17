/**
 *
 * Copyright © 2016 Florian Schmaus and Fernando Ramirez
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
package org.jivesoftware.smackx.mam.filter;

import org.jivesoftware.smack.filter.FlexibleStanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.mam.element.MamElements.MamResultExtension;
import org.jivesoftware.smackx.mam.element.MamQueryIQ;

/**
 * MAM result filter class.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez and Florian Schmaus
 * 
 */
public class MamResultFilter extends FlexibleStanzaTypeFilter<Message> {

    private final String queryId;

    public MamResultFilter(MamQueryIQ mamQueryIQ) {
        super(Message.class);
        this.queryId = mamQueryIQ.getQueryId();
    }

    @Override
    protected boolean acceptSpecific(Message message) {
        MamResultExtension mamResultExtension = MamResultExtension.from(message);

        if (mamResultExtension == null) {
            return false;
        }

        String resultQueryId = mamResultExtension.getQueryId();
        return ((queryId == null && resultQueryId == null) || (queryId != null && queryId.equals(resultQueryId)));
    }

}
