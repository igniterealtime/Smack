/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smack.tbr.element;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.tbr.TBRManager;
import org.jxmpp.jid.Jid;

/**
 * TBR get tokens IQ class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://www.xmpp.org/extensions/inbox/token-reconnection.html">
 *      XEP-xxxx: Token-based reconnection</a>
 */
public class TBRGetTokensIQ extends IQ {

    public static final String NAMESPACE = TBRManager.AUTH_NAMESPACE;
    public static final String ELEMENT = QUERY_ELEMENT;

    /**
     * TBR get tokens IQ constructor.
     * 
     * @param to
     */
    public TBRGetTokensIQ(Jid to) {
        super(ELEMENT, NAMESPACE);
        this.setType(Type.get);
        this.setTo(to);
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.setEmptyElement();
        return xml;
    }

}
