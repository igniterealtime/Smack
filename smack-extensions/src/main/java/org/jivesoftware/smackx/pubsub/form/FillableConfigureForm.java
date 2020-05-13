/**
 *
 * Copyright the original author or authors, 2020 Florian Schmaus
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
package org.jivesoftware.smackx.pubsub.form;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ChildrenAssociationPolicy;
import org.jivesoftware.smackx.pubsub.ConfigureNodeFields;
import org.jivesoftware.smackx.pubsub.ItemReply;
import org.jivesoftware.smackx.pubsub.NodeType;
import org.jivesoftware.smackx.pubsub.NotificationType;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.form.FillableForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.Jid;

public class FillableConfigureForm extends FillableForm implements ConfigureFormReader {

    public FillableConfigureForm(DataForm dataForm) {
        super(dataForm);
    }

    /**
     * Sets the value of access model.
     *
     * @param accessModel TODO javadoc me please
     */
    public void setAccessModel(AccessModel accessModel) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.access_model.getFieldName())
                        .setValue(accessModel)
                        .build();
        write(formField);
    }

    /**
     * Set the URL of an XSL transformation which can be applied to payloads in order to
     * generate an appropriate message body element.
     *
     * @param bodyXslt The URL of an XSL
     */
    public void setBodyXSLT(String bodyXslt) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.body_xslt.getFieldName())
                        .setValue(bodyXslt)
                        .build();
        write(formField);
    }

    /**
     * Set the list of child node ids that are associated with a collection node.
     *
     * @param children TODO javadoc me please
     */
    public void setChildren(List<String> children) {
        FormField formField = FormField.textMultiBuilder(ConfigureNodeFields.children.getFieldName())
                        .addValues(children)
                        .build();
        write(formField);
    }

    /**
     * Sets the policy that determines who may associate children with the node.
     *
     * @param policy The policy being set
     */
    public void setChildrenAssociationPolicy(ChildrenAssociationPolicy policy) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.children_association_policy.getFieldName())
                        .setValue(policy)
                        .build();
        write(formField);
    }

    /**
     * Set the JID's in the whitelist of users that can associate child nodes with the collection
     * node.  This is only relevant if {@link #getChildrenAssociationPolicy()} is set to
     * {@link ChildrenAssociationPolicy#whitelist}.
     *
     * @param whitelist The list of JID's
     */
    public void setChildrenAssociationWhitelist(List<? extends Jid> whitelist) {
        FormField formField = FormField.jidMultiBuilder(ConfigureNodeFields.children_association_whitelist.getFieldName())
                        .addValues(whitelist)
                        .build();
        write(formField);
    }

    /**
     * Set the maximum number of child nodes that can be associated with a collection node.
     *
     * @param max The maximum number of child nodes.
     */
    public void setChildrenMax(int max) {
        FormField formField = FormField.textSingleBuilder(ConfigureNodeFields.children_max.getFieldName())
                        .setValue(max)
                        .build();
        write(formField);
    }

    /**
     * Sets the collection node which the node is affiliated with.
     *
     * @param collection The node id of the collection node
     */
    public void setCollection(String collection) {
        setCollections(Collections.singletonList(collection));
    }

    public void setCollections(Collection<String> collections) {
        FormField formField = FormField.textMultiBuilder(ConfigureNodeFields.collection.getFieldName())
                        .addValues(collections)
                        .build();
        write(formField);
    }

    /**
     * Sets the URL of an XSL transformation which can be applied to the payload
     * format in order to generate a valid Data Forms result that the client could
     * display using a generic Data Forms rendering engine.
     *
     * @param url The URL of an XSL transformation
     */
    public void setDataformXSLT(URL url) {
        FormField formField = FormField.textSingleBuilder(ConfigureNodeFields.dataform_xslt.getFieldName())
                        .setValue(url)
                        .build();
        write(formField);
    }

    /**
     * Sets whether the node will deliver payloads with event notifications.
     *
     * @param deliver true if the payload will be delivered, false otherwise
     */
    public void setDeliverPayloads(boolean deliver) {
        writeBoolean(ConfigureNodeFields.deliver_payloads.getFieldName(), deliver);
    }

    /**
     * Sets who should get the replies to items.
     *
     * @param reply Defines who should get the reply
     */
    public void setItemReply(ItemReply reply) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.itemreply.getFieldName())
                        .setValue(reply)
                        .build();
        write(formField);
    }

    /**
     * Set the maximum number of items to persisted to this node if {@link #isPersistItems()} is
     * true.
     *
     * @param max The maximum number of items to persist
     */
    public void setMaxItems(int max) {
        FormField formField = FormField.textSingleBuilder(ConfigureNodeFields.max_items.getFieldName())
                        .setValue(max)
                        .build();
        write(formField);
    }

    /**
     * Sets the maximum payload size in bytes.
     *
     * @param max The maximum payload size
     */
    public void setMaxPayloadSize(int max) {
        FormField formField = FormField.textSingleBuilder(ConfigureNodeFields.max_payload_size.getFieldName())
                        .setValue(max)
                        .build();
        write(formField);
    }

    /**
     * Sets the node type.
     *
     * @param type The node type
     */
    public void setNodeType(NodeType type) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.node_type.getFieldName())
                        .setValue(type)
                        .build();
        write(formField);
    }

    /**
     * Sets whether subscribers should be notified when the configuration changes.
     *
     * @param notify true if subscribers should be notified, false otherwise
     */
    public void setNotifyConfig(boolean notify) {
        writeBoolean(ConfigureNodeFields.notify_config.getFieldName(), notify);
    }

    /**
     * Sets whether subscribers should be notified when the node is deleted.
     *
     * @param notify true if subscribers should be notified, false otherwise
     */
    public void setNotifyDelete(boolean notify)  {
        writeBoolean(ConfigureNodeFields.notify_delete.getFieldName(), notify);
    }


    /**
     * Sets whether subscribers should be notified when items are deleted
     * from the node.
     *
     * @param notify true if subscribers should be notified, false otherwise
     */
    public void setNotifyRetract(boolean notify)  {
        writeBoolean(ConfigureNodeFields.notify_retract.getFieldName(), notify);
    }

    /**
     * Sets the NotificationType for the node.
     *
     * @param notificationType The enum representing the possible options
     * @since 4.3
     */
    public void setNotificationType(NotificationType notificationType) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.notification_type.getFieldName())
                        .setValue(notificationType)
                        .build();
        write(formField);
    }

    /**
     * Sets whether items should be persisted in the node.
     *
     * @param persist true if items should be persisted, false otherwise
     */
    public void setPersistentItems(boolean persist) {
        writeBoolean(ConfigureNodeFields.persist_items.getFieldName(), persist);
    }

    /**
     * Sets whether to deliver notifications to available users only.
     *
     * @param presenceBased true if user must be available, false otherwise
     */
    public void setPresenceBasedDelivery(boolean presenceBased) {
        writeBoolean(ConfigureNodeFields.presence_based_delivery.getFieldName(), presenceBased);
    }


    /**
     * Sets the publishing model for the node, which determines who may publish to it.
     *
     * @param publish The enum representing the possible options for the publishing model
     */
    public void setPublishModel(PublishModel publish) {
        FormField formField = FormField.listSingleBuilder(ConfigureNodeFields.publish_model.getFieldName())
                        .setValue(publish)
                        .build();
        write(formField);
    }

    /**
     * Sets the roster groups that are allowed to subscribe and retrieve items.
     *
     * @param groups The roster groups
     */
    public void setRosterGroupsAllowed(List<? extends CharSequence> groups) {
        writeListMulti(ConfigureNodeFields.roster_groups_allowed.getFieldName(), groups);
    }

    /**
     * Sets whether subscriptions are allowed.
     *
     * @param subscribe true if they are, false otherwise
     */
    public void setSubscribe(boolean subscribe) {
        writeBoolean(ConfigureNodeFields.subscribe.getFieldName(), subscribe);
    }

    /**
     * Sets a human readable title for the node.
     *
     * @param title The node title
     */
    public void setTitle(CharSequence title)  {
        writeTextSingle(ConfigureNodeFields.title.getFieldName(), title);
    }

    /**
     * Sets the type of node data, usually specified by the namespace of the payload (if any).
     *
     * @param type The type of node data
     */
    public void setDataType(CharSequence type)  {
        writeTextSingle(ConfigureNodeFields.type.getFieldName(), type);
    }
}
