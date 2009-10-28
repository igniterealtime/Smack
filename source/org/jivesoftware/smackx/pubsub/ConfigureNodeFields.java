/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.pubsub;

import java.net.URL;

import org.jivesoftware.smackx.Form;

/**
 * This enumeration represents all the fields of a node configuration form.  This enumeration
 * is not required when using the {@link ConfigureForm} to configure nodes, but may be helpful
 * for generic UI's using only a {@link Form} for configuration.
 * 
 * @author Robin Collier
 */
public enum ConfigureNodeFields
{
	/**
	 * Determines who may subscribe and retrieve items
	 * 
	 * <p><b>Value: {@link AccessModel}</b></p>
	 */
	access_model,

	/**
	 * The URL of an XSL transformation which can be applied to 
	 * payloads in order to generate an appropriate message
	 * body element
	 * 
	 * <p><b>Value: {@link URL}</b></p>
	 */
	body_xslt,
	
	/**
	 * The collection with which a node is affiliated
	 * 
	 * <p><b>Value: String</b></p>
	 */
	collection,

	/**
	 * The URL of an XSL transformation which can be applied to 
	 * payload format in order to generate a valid Data Forms result 
	 * that the client could display using a generic Data Forms 
	 * rendering engine body element.
	 * 
	 * <p><b>Value: {@link URL}</b></p>
	 */
	dataform_xslt,

	/**
	 * Whether to deliver payloads with event notifications
	 *
	 * <p><b>Value: boolean</b></p>
	 */
	deliver_payloads,
	
	/**
	 * Whether owners or publisher should receive replies to items
	 *
	 * <p><b>Value: {@link ItemReply}</b></p>
	 */
	itemreply,
	
	/**
	 * Who may associate leaf nodes with a collection
	 * 
	 * <p><b>Value: {@link ChildrenAssociationPolicy}</b></p>
	 */
	children_association_policy,
	
	/**
	 * The list of JIDs that may associate leaf nodes with a 
	 * collection
	 * 
	 * <p><b>Value: List of JIDs as Strings</b></p>
	 */
	children_association_whitelist,
	
	/**
	 * The child nodes (leaf or collection) associated with a collection
	 * 
	 * <p><b>Value: List of Strings</b></p>
	 */
	children,
	
	/**
	 * The maximum number of child nodes that can be associated with a 
	 * collection
	 * 
	 * <p><b>Value: int</b></p>
	 */
	children_max,
	
	/**
	 * The maximum number of items to persist
	 * 
	 * <p><b>Value: int</b></p>
	 */
	max_items,
	
	/**
	 * The maximum payload size in bytes
	 * 
	 * <p><b>Value: int</b></p>
	 */
	max_payload_size,
	
	/**
	 * Whether the node is a leaf (default) or collection
	 * 
	 * <p><b>Value: {@link NodeType}</b></p>
	 */
	node_type,
	
	/**
	 * Whether to notify subscribers when the node configuration changes
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	notify_config,
	
	/**
	 * Whether to notify subscribers when the node is deleted
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	notify_delete,

	/**
	 * Whether to notify subscribers when items are removed from the node
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	notify_retract,
	
	/**
	 * Whether to persist items to storage.  This is required to have multiple 
	 * items in the node. 
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	persist_items,
	
	/**
	 * Whether to deliver notifications to available users only
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	presence_based_delivery,

	/**
	 * Defines who can publish to the node
	 * 
	 * <p><b>Value: {@link PublishModel}</b></p>
	 */
	publish_model,
	
	/**
	 * The specific multi-user chat rooms to specify for replyroom
	 * 
	 * <p><b>Value: List of JIDs as Strings</b></p>
	 */
	replyroom,
	
	/**
	 * The specific JID(s) to specify for replyto
	 * 
	 * <p><b>Value: List of JIDs as Strings</b></p>
	 */
	replyto,
	
	/**
	 * The roster group(s) allowed to subscribe and retrieve items
	 * 
	 * <p><b>Value: List of strings</b></p>
	 */
	roster_groups_allowed,
	
	/**
	 * Whether to allow subscriptions
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	subscribe,
	
	/**
	 * A friendly name for the node
	 * 
	 * <p><b>Value: String</b></p>
	 */
	title,
	
	/**
	 * The type of node data, ussually specified by the namespace 
	 * of the payload(if any);MAY be a list-single rather than a 
	 * text single
	 * 
	 * <p><b>Value: String</b></p>
	 */
	type;
	
	public String getFieldName()
	{
		return "pubsub#" + toString();
	}
}
