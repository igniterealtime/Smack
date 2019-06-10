/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.push_notifications.element;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

/**
 * Enable Push Notifications IQ.
 *
 * @see <a href="http://xmpp.org/extensions/xep-0357.html">XEP-0357: Push
 *      Notifications</a>
 * @author Fernando Ramirez
 *
 */
public class EnablePushNotificationsIQ extends IQ {

    /**
     * enable element.
     */
    public static final String ELEMENT = "enable";

    /**
     * the IQ NAMESPACE.
     */
    public static final String NAMESPACE = PushNotificationsElements.NAMESPACE;

    private final Jid jid;
    private final String node;
    private final HashMap<String, String> publishOptions;

    public EnablePushNotificationsIQ(Jid jid, String node, HashMap<String, String> publishOptions) {
        super(ELEMENT, NAMESPACE);
        this.jid = jid;
        this.node = node;
        this.publishOptions = publishOptions;
        this.setType(Type.set);
    }

    public EnablePushNotificationsIQ(Jid jid, String node) {
        this(jid, node, null);
    }

    /**
     * Get the JID.
     *
     * @return the JID
     */
    public Jid getJid() {
        return jid;
    }

    /**
     * Get the node.
     *
     * @return the node
     */
    public String getNode() {
        return node;
    }

    /**
     * Get the publish options.
     *
     * @return the publish options
     */
    public HashMap<String, String> getPublishOptions() {
        return publishOptions;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder xml) {
        xml.attribute("jid", jid);
        xml.attribute("node", node);
        xml.rightAngleBracket();

        if (publishOptions != null) {
            DataForm dataForm = new DataForm(DataForm.Type.submit);

            // TODO: Shouldn't this use some potentially existing PubSub API? Also FORM_TYPE fields are usually of type
            // 'hidden', but the examples in XEP-0357 do also not set the value to hidden and FORM_TYPE itself appears
            // to be more convention than specification.
            FormField.Builder formTypeField = FormField.builder("FORM_TYPE");
            formTypeField.addValue(PubSub.NAMESPACE + "#publish-options");
            dataForm.addField(formTypeField.build());

            Iterator<Map.Entry<String, String>> publishOptionsIterator = publishOptions.entrySet().iterator();
            while (publishOptionsIterator.hasNext()) {
                Map.Entry<String, String> pairVariableValue = publishOptionsIterator.next();
                FormField.Builder field = FormField.builder(pairVariableValue.getKey());
                field.addValue(pairVariableValue.getValue());
                dataForm.addField(field.build());
            }

            xml.element(dataForm);
        }

        return xml;
    }

}
