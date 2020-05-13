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

import java.util.Collections;
import java.util.List;

import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ChildrenAssociationPolicy;
import org.jivesoftware.smackx.pubsub.ConfigureNodeFields;
import org.jivesoftware.smackx.pubsub.ItemReply;
import org.jivesoftware.smackx.pubsub.NodeType;
import org.jivesoftware.smackx.pubsub.NotificationType;
import org.jivesoftware.smackx.pubsub.PublishModel;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.xdata.FormField;
import org.jivesoftware.smackx.xdata.JidMultiFormField;
import org.jivesoftware.smackx.xdata.form.FormReader;

import org.jxmpp.jid.Jid;

public interface ConfigureFormReader extends FormReader {

    String FORM_TYPE = PubSub.NAMESPACE + "#node_config";

    /**
     * Get the currently configured {@link AccessModel}, null if it is not set.
     *
     * @return The current {@link AccessModel}
     */
    default AccessModel getAccessModel() {
        String value = readFirstValue(ConfigureNodeFields.access_model.getFieldName());
        if (value == null) {
            return null;
        }
        return AccessModel.valueOf(value);
    }

    /**
     * Returns the URL of an XSL transformation which can be applied to payloads in order to
     * generate an appropriate message body element.
     *
     * @return URL to an XSL
     */
    default String getBodyXSLT() {
        return readFirstValue(ConfigureNodeFields.body_xslt.getFieldName());
    }

    /**
     * The id's of the child nodes associated with a collection node (both leaf and collection).
     *
     * @return list of child nodes.
     */
    default List<String> getChildren() {
        return readStringValues(ConfigureNodeFields.children.getFieldName());
    }

    /**
     * Returns the policy that determines who may associate children with the node.
     *
     * @return The current policy
     */
    default ChildrenAssociationPolicy getChildrenAssociationPolicy() {
        String value = readFirstValue(ConfigureNodeFields.children_association_policy.getFieldName());
        if (value == null) {
            return null;
        }
        return ChildrenAssociationPolicy.valueOf(value);
    }

    /**
     * List of JID's that are on the whitelist that determines who can associate child nodes
     * with the collection node.  This is only relevant if {@link #getChildrenAssociationPolicy()} is set to
     * {@link ChildrenAssociationPolicy#whitelist}.
     *
     * @return List of the whitelist
     */
    default List<Jid> getChildrenAssociationWhitelist() {
        FormField formField = read(ConfigureNodeFields.children_association_whitelist.getFieldName());
        if (formField == null) {
            Collections.emptyList();
        }
        JidMultiFormField jidMultiFormField = formField.ifPossibleAs(JidMultiFormField.class);
        return jidMultiFormField.getValues();
    }

    /**
     * Gets the maximum number of child nodes that can be associated with the collection node.
     *
     * @return The maximum number of child nodes
     */
    default Integer getChildrenMax() {
        return readInteger(ConfigureNodeFields.children_max.getFieldName());
    }

    /**
     * Gets the collection node which the node is affiliated with.
     *
     * @return The collection node id
     */
    default List<? extends CharSequence> getCollection() {
        return readValues(ConfigureNodeFields.collection.getFieldName());
    }

    /**
     * Gets the URL of an XSL transformation which can be applied to the payload
     * format in order to generate a valid Data Forms result that the client could
     * display using a generic Data Forms rendering engine.
     *
     * @return The URL of an XSL transformation
     */
    default String getDataformXSLT() {
        return readFirstValue(ConfigureNodeFields.dataform_xslt.getFieldName());
    }

    /**
     * Does the node deliver payloads with event notifications.
     *
     * @return true if it does, false otherwise
     */
    default Boolean isDeliverPayloads() {
        return readBoolean(ConfigureNodeFields.deliver_payloads.getFieldName());
    }

    /**
     * Determines who should get replies to items.
     *
     * @return Who should get the reply
     */
    default ItemReply getItemReply() {
        String value = readFirstValue(ConfigureNodeFields.itemreply.getFieldName());
        if (value == null) {
            return null;
        }
        return ItemReply.valueOf(value);
    }

    /**
     * Gets the maximum number of items to persisted to this node if {@link #isPersistItems()} is
     * true.
     *
     * @return The maximum number of items to persist
     */
    default Integer getMaxItems() {
        return readInteger(ConfigureNodeFields.max_items.getFieldName());
    }

    /**
     * Gets the maximum payload size in bytes.
     *
     * @return The maximum payload size
     */
    default Integer getMaxPayloadSize() {
        return readInteger(ConfigureNodeFields.max_payload_size.getFieldName());
    }

    /**
     * Gets the node type.
     *
     * @return The node type
     */
    default NodeType getNodeType() {
        String value = readFirstValue(ConfigureNodeFields.node_type.getFieldName());
        if (value == null) {
            return null;
        }
        return NodeType.valueOf(value);
    }

    /**
     * Determines if subscribers should be notified when the configuration changes.
     *
     * @return true if they should be notified, false otherwise
     */
    default Boolean isNotifyConfig() {
        return readBoolean(ConfigureNodeFields.notify_config.getFieldName());
    }

    /**
     * Determines whether subscribers should be notified when the node is deleted.
     *
     * @return true if subscribers should be notified, false otherwise
     */
    default Boolean isNotifyDelete() {
        return readBoolean(ConfigureNodeFields.notify_delete.getFieldName());
    }

    /**
     * Determines whether subscribers should be notified when items are deleted
     * from the node.
     *
     * @return true if subscribers should be notified, false otherwise
     */
    default Boolean isNotifyRetract() {
        return readBoolean(ConfigureNodeFields.notify_retract.getFieldName());
    }

    /**
     * Determines the type of notifications which are sent.
     *
     * @return NotificationType for the node configuration
     * @since 4.3
     */
    default NotificationType getNotificationType() {
        String value = readFirstValue(ConfigureNodeFields.notification_type.getFieldName());
        if (value == null) {
            return null;
        }
        return NotificationType.valueOf(value);
    }

    /**
     * Determines whether items should be persisted in the node.
     *
     * @return true if items are persisted
     */
    default boolean isPersistItems() {
        return readBoolean(ConfigureNodeFields.persist_items.getFieldName());
    }

    /**
     * Determines whether to deliver notifications to available users only.
     *
     * @return true if users must be available
     */
    default boolean isPresenceBasedDelivery() {
        return readBoolean(ConfigureNodeFields.presence_based_delivery.getFieldName());
    }

    /**
     * Gets the publishing model for the node, which determines who may publish to it.
     *
     * @return The publishing model
     */
    default PublishModel getPublishModel() {
        String value = readFirstValue(ConfigureNodeFields.publish_model.getFieldName());
        if (value == null) {
            return null;
        }
        return PublishModel.valueOf(value);
    }

    /**
     * Gets the roster groups that are allowed to subscribe and retrieve items.
     *
     * @return The roster groups
     */
    default List<String> getRosterGroupsAllowed() {
        return readStringValues(ConfigureNodeFields.roster_groups_allowed.getFieldName());
    }

    /**
     * Determines if subscriptions are allowed.
     *
     * @return true if subscriptions are allowed, false otherwise
     */
    default boolean isSubscribe() {
        return readBoolean(ConfigureNodeFields.subscribe.getFieldName());
    }

    /**
     * Gets the human readable node title.
     *
     * @return The node title
     */
    default String getTitle() {
        return readFirstValue(ConfigureNodeFields.title.getFieldName());
    }

    /**
     * The type of node data, usually specified by the namespace of the payload (if any).
     *
     * @return The type of node data
     */
    default String getDataType() {
        return readFirstValue(ConfigureNodeFields.type.getFieldName());
    }
}
