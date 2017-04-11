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
package org.jivesoftware.smackx.mam.element;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

/**
 * MAM Query IQ class.
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0313.html">XEP-0313: Message
 *      Archive Management</a>
 * @author Fernando Ramirez and Florian Schmaus
 * 
 */
public class MamQueryIQ extends IQ {

    /**
     * the MAM query IQ element.
     */
    public static final String ELEMENT = QUERY_ELEMENT;

    /**
     * the MAM query IQ NAMESPACE.
     */
    public static final String NAMESPACE = MamElements.NAMESPACE;

    private final String queryId;
    private final String node;
    private final DataForm dataForm;

    /**
     * MAM query IQ constructor.
     * 
     * @param queryId
     */
    public MamQueryIQ(String queryId) {
        this(queryId, null, null);
        setType(IQ.Type.get);
    }

    /**
     * MAM query IQ constructor.
     * 
     * @param form
     */
    public MamQueryIQ(DataForm form) {
        this(null, null, form);
    }

    /**
     * MAM query IQ constructor.
     * 
     * @param queryId
     * @param form
     */
    public MamQueryIQ(String queryId, DataForm form) {
        this(queryId, null, form);
    }

    /**
     * MAM query IQ constructor.
     * 
     * @param queryId
     * @param node
     * @param dataForm
     */
    public MamQueryIQ(String queryId, String node, DataForm dataForm) {
        super(ELEMENT, NAMESPACE);
        this.queryId = queryId;
        this.node = node;
        this.dataForm = dataForm;

        if (dataForm != null) {
            FormField field = dataForm.getHiddenFormTypeField();
            if (field == null) {
                throw new IllegalArgumentException("If a data form is given it must posses a hidden form type field");
            }
            if (!field.getValues().get(0).equals(MamElements.NAMESPACE)) {
                throw new IllegalArgumentException(
                        "Value of the hidden form type field must be '" + MamElements.NAMESPACE + "'");
            }
            addExtension(dataForm);
        }
    }

    /**
     * Get query id.
     * 
     * @return the query id
     */
    public String getQueryId() {
        return queryId;
    }

    /**
     * Get the Node name.
     * 
     * @return the node
     */
    public String getNode() {
      return node;
    }

    /**
     * Get the data form.
     *
     * @return the data form
     */
    public DataForm getDataForm() {
        return dataForm;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.optAttribute("queryid", queryId);
        xml.optAttribute("node", node);
        xml.rightAngleBracket();
        return xml;
    }

}
