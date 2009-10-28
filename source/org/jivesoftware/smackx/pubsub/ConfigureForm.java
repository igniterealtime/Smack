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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.packet.DataForm;

/**
 * A decorator for a {@link Form} to easily enable reading and updating
 * of node configuration.  All operations read or update the underlying {@link DataForm}.
 * 
 * <p>Unlike the {@link Form}.setAnswer(XXX)} methods, which throw an exception if the field does not
 * exist, all <b>ConfigureForm.setXXX</b> methods will create the field in the wrapped form
 * if it does not already exist. 
 * 
 * @author Robin Collier
 */
public class ConfigureForm extends Form
{
	/**
	 * Create a decorator from an existing {@link DataForm} that has been
	 * retrieved from parsing a node configuration request.
	 * 
	 * @param configDataForm
	 */
	public ConfigureForm(DataForm configDataForm)
	{
		super(configDataForm);
	}
	
	/**
	 * Create a decorator from an existing {@link Form} for node configuration.
	 * Typically, this can be used to create a decorator for an answer form
	 * by using the result of {@link #createAnswerForm()} as the input parameter.
	 * 
	 * @param nodeConfigForm
	 */
	public ConfigureForm(Form nodeConfigForm)
	{
		super(nodeConfigForm.getDataFormToSend());
	}
	
	/**
	 * Create a new form for configuring a node.  This would typically only be used 
	 * when creating and configuring a node at the same time via {@link PubSubManager#createNode(String, Form)}, since 
	 * configuration of an existing node is typically accomplished by calling {@link LeafNode#getNodeConfiguration()} and
	 * using the resulting form to create a answer form.  See {@link #ConfigureForm(Form)}.
	 * @param formType
	 */
	public ConfigureForm(FormType formType)
	{
		super(formType.toString());
	}
	
	/**
	 * Get the currently configured {@link AccessModel}, null if it is not set.
	 * 
	 * @return The current {@link AccessModel}
	 */
	public AccessModel getAccessModel()
	{
		String value = getFieldValue(ConfigureNodeFields.access_model);
		
		if (value == null)
			return null;
		else
			return AccessModel.valueOf(value);
	}
	
	/**
	 * Sets the value of access model.
	 * 
	 * @param accessModel
	 */
	public void setAccessModel(AccessModel accessModel)
	{
		addField(ConfigureNodeFields.access_model, FormField.TYPE_LIST_SINGLE);
		setAnswer(ConfigureNodeFields.access_model.getFieldName(), getListSingle(accessModel.toString()));
	}

	/**
	 * Returns the URL of an XSL transformation which can be applied to payloads in order to 
	 * generate an appropriate message body element.
	 * 
	 * @return URL to an XSL
	 */
	public String getBodyXSLT()
	{
		return getFieldValue(ConfigureNodeFields.body_xslt);
	}

	/**
	 * Set the URL of an XSL transformation which can be applied to payloads in order to 
	 * generate an appropriate message body element.
	 * 
	 * @param bodyXslt The URL of an XSL
	 */
	public void setBodyXSLT(String bodyXslt)
	{
		addField(ConfigureNodeFields.body_xslt, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.body_xslt.getFieldName(), bodyXslt);
	}
	
	/**
	 * The id's of the child nodes associated with a collection node (both leaf and collection).
	 * 
	 * @return Iterator over the list of child nodes.
	 */
	public Iterator<String> getChildren()
	{
		return getFieldValues(ConfigureNodeFields.children);
	}
	
	/**
	 * Set the list of child node ids that are associated with a collection node.
	 * 
	 * @param children
	 */
	public void setChildren(List<String> children)
	{
		addField(ConfigureNodeFields.children, FormField.TYPE_TEXT_MULTI);
		setAnswer(ConfigureNodeFields.children.getFieldName(), children);
	}
	
	/**
	 * Returns the policy that determines who may associate children with the node.
	 *  
	 * @return The current policy
	 */
	public ChildrenAssociationPolicy getChildrenAssociationPolicy()
	{
		String value = getFieldValue(ConfigureNodeFields.children_association_policy);
		
		if (value == null)
			return null;
		else
			return ChildrenAssociationPolicy.valueOf(value);
	}
	
	/**
	 * Sets the policy that determines who may associate children with the node.
	 * 
	 * @param policy The policy being set
	 */
	public void setChildrenAssociationPolicy(ChildrenAssociationPolicy policy)
	{
		addField(ConfigureNodeFields.children_association_policy, FormField.TYPE_LIST_SINGLE);
		setAnswer(ConfigureNodeFields.children_association_policy.getFieldName(), policy.toString());
	}
	
	/**
	 * Iterator of JID's that are on the whitelist that determines who can associate child nodes 
	 * with the collection node.  This is only relevant if {@link #getChildrenAssociationPolicy()} is set to
	 * {@link ChildrenAssociationPolicy#whitelist}.
	 * 
	 * @return Iterator over whitelist
	 */
	public Iterator<String> getChildrenAssociationWhitelist()
	{
		return getFieldValues(ConfigureNodeFields.children_association_whitelist);
	}
	
	/**
	 * Set the JID's in the whitelist of users that can associate child nodes with the collection 
	 * node.  This is only relevant if {@link #getChildrenAssociationPolicy()} is set to
	 * {@link ChildrenAssociationPolicy#whitelist}.
	 * 
	 * @param whitelist The list of JID's
	 */
	public void setChildrenAssociationWhitelist(List<String> whitelist)
	{
		addField(ConfigureNodeFields.children_association_whitelist, FormField.TYPE_JID_MULTI);
		setAnswer(ConfigureNodeFields.children_association_whitelist.getFieldName(), whitelist);
	}

	/**
	 * Gets the maximum number of child nodes that can be associated with the collection node.
	 * 
	 * @return The maximum number of child nodes
	 */
	public int getChildrenMax()
	{
		return Integer.parseInt(getFieldValue(ConfigureNodeFields.children_max));
	}

	/**
	 * Set the maximum number of child nodes that can be associated with a collection node.
	 * 
	 * @param max The maximum number of child nodes.
	 */
	public void setChildrenMax(int max)
	{
		addField(ConfigureNodeFields.children_max, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.children_max.getFieldName(), max);
	}

	/**
	 * Gets the collection node which the node is affiliated with.
	 * 
	 * @return The collection node id
	 */
	public String getCollection()
	{
		return getFieldValue(ConfigureNodeFields.collection);
	}

	/**
	 * Sets the collection node which the node is affiliated with.
	 * 
	 * @param collection The node id of the collection node
	 */
	public void setCollection(String collection)
	{
		addField(ConfigureNodeFields.collection, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.collection.getFieldName(), collection);
	}

	/**
	 * Gets the URL of an XSL transformation which can be applied to the payload
	 * format in order to generate a valid Data Forms result that the client could
	 * display using a generic Data Forms rendering engine.
	 * 
	 * @return The URL of an XSL transformation
	 */
	public String getDataformXSLT()
	{
		return getFieldValue(ConfigureNodeFields.dataform_xslt);
	}

	/**
	 * Sets the URL of an XSL transformation which can be applied to the payload
	 * format in order to generate a valid Data Forms result that the client could
	 * display using a generic Data Forms rendering engine.
	 * 
	 * @param url The URL of an XSL transformation
	 */
	public void setDataformXSLT(String url)
	{
		addField(ConfigureNodeFields.dataform_xslt, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.dataform_xslt.getFieldName(), url);
	}

	/**
	 * Does the node deliver payloads with event notifications.
	 * 
	 * @return true if it does, false otherwise
	 */
	public boolean isDeliverPayloads()
	{
		return parseBoolean(getFieldValue(ConfigureNodeFields.deliver_payloads));
	}
	
	/**
	 * Sets whether the node will deliver payloads with event notifications.
	 * 
	 * @param deliver true if the payload will be delivered, false otherwise
	 */
	public void setDeliverPayloads(boolean deliver)
	{
		addField(ConfigureNodeFields.deliver_payloads, FormField.TYPE_BOOLEAN);
		setAnswer(ConfigureNodeFields.deliver_payloads.getFieldName(), deliver);
	}

	/**
	 * Determines who should get replies to items
	 * 
	 * @return Who should get the reply
	 */
	public ItemReply getItemReply()
	{
		String value = getFieldValue(ConfigureNodeFields.itemreply);
		
		if (value == null)
			return null;
		else
			return ItemReply.valueOf(value);
	}

	/**
	 * Sets who should get the replies to items
	 * 
	 * @param reply Defines who should get the reply
	 */
	public void setItemReply(ItemReply reply)
	{
		addField(ConfigureNodeFields.itemreply, FormField.TYPE_LIST_SINGLE);
		setAnswer(ConfigureNodeFields.itemreply.getFieldName(), getListSingle(reply.toString()));
	}

	/**
	 * Gets the maximum number of items to persisted to this node if {@link #isPersistItems()} is
	 * true.
	 * 
	 * @return The maximum number of items to persist
	 */
	public int getMaxItems()
	{
		return Integer.parseInt(getFieldValue(ConfigureNodeFields.max_items));
	}

	/**
	 * Set the maximum number of items to persisted to this node if {@link #isPersistItems()} is
	 * true.
	 * 
	 * @param max The maximum number of items to persist
	 */
	public void setMaxItems(int max)
	{
		addField(ConfigureNodeFields.max_items, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.max_items.getFieldName(), max);
	}
	
	/**
	 * Gets the maximum payload size in bytes.
	 * 
	 * @return The maximum payload size
	 */
	public int getMaxPayloadSize()
	{
		return Integer.parseInt(getFieldValue(ConfigureNodeFields.max_payload_size));
	}

	/**
	 * Sets the maximum payload size in bytes
	 * 
	 * @param max The maximum payload size
	 */
	public void setMaxPayloadSize(int max)
	{
		addField(ConfigureNodeFields.max_payload_size, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.max_payload_size.getFieldName(), max);
	}
	
	/**
	 * Gets the node type
	 * 
	 * @return The node type
	 */
	public NodeType getNodeType()
	{
		String value = getFieldValue(ConfigureNodeFields.node_type);
		
		if (value == null)
			return null;
		else
			return NodeType.valueOf(value);
	}
	
	/**
	 * Sets the node type
	 * 
	 * @param type The node type
	 */
	public void setNodeType(NodeType type)
	{
		addField(ConfigureNodeFields.node_type, FormField.TYPE_LIST_SINGLE);
		setAnswer(ConfigureNodeFields.node_type.getFieldName(), getListSingle(type.toString()));
	}

	/**
	 * Determines if subscribers should be notified when the configuration changes.
	 * 
	 * @return true if they should be notified, false otherwise
	 */
	public boolean isNotifyConfig()
	{
		return parseBoolean(getFieldValue(ConfigureNodeFields.notify_config));
	}
	
	/**
	 * Sets whether subscribers should be notified when the configuration changes.
	 * 
	 * @param notify true if subscribers should be notified, false otherwise
	 */
	public void setNotifyConfig(boolean notify)
	{
		addField(ConfigureNodeFields.notify_config, FormField.TYPE_BOOLEAN);
		setAnswer(ConfigureNodeFields.notify_config.getFieldName(), notify);
	}

	/**
	 * Determines whether subscribers should be notified when the node is deleted.
	 * 
	 * @return true if subscribers should be notified, false otherwise
	 */
	public boolean isNotifyDelete()
	{
		return parseBoolean(getFieldValue(ConfigureNodeFields.notify_delete));
	}
	
	/**
	 * Sets whether subscribers should be notified when the node is deleted.
	 * 
	 * @param notify true if subscribers should be notified, false otherwise
	 */
	public void setNotifyDelete(boolean notify) 
	{
		addField(ConfigureNodeFields.notify_delete, FormField.TYPE_BOOLEAN);
		setAnswer(ConfigureNodeFields.notify_delete.getFieldName(), notify);
	}

	/**
	 * Determines whether subscribers should be notified when items are deleted 
	 * from the node.
	 * 
	 * @return true if subscribers should be notified, false otherwise
	 */
	public boolean isNotifyRetract()
	{
		return parseBoolean(getFieldValue(ConfigureNodeFields.notify_retract));
	}
	
	/**
	 * Sets whether subscribers should be notified when items are deleted 
	 * from the node.
	 * 
	 * @param notify true if subscribers should be notified, false otherwise
	 */
	public void setNotifyRetract(boolean notify) 
	{
		addField(ConfigureNodeFields.notify_retract, FormField.TYPE_BOOLEAN);
		setAnswer(ConfigureNodeFields.notify_retract.getFieldName(), notify);
	}
	
	/**
	 * Determines whether items should be persisted in the node.
	 * 
	 * @return true if items are persisted
	 */
	public boolean isPersistItems()
	{
		return parseBoolean(getFieldValue(ConfigureNodeFields.persist_items));
	}
	
	/**
	 * Sets whether items should be persisted in the node.
	 * 
	 * @param persist true if items should be persisted, false otherwise
	 */
	public void setPersistentItems(boolean persist) 
	{
		addField(ConfigureNodeFields.persist_items, FormField.TYPE_BOOLEAN);
		setAnswer(ConfigureNodeFields.persist_items.getFieldName(), persist);
	}

	/**
	 * Determines whether to deliver notifications to available users only.
	 * 
	 * @return true if users must be available
	 */
	public boolean isPresenceBasedDelivery()
	{
		return parseBoolean(getFieldValue(ConfigureNodeFields.presence_based_delivery));
	}
	
	/**
	 * Sets whether to deliver notifications to available users only.
	 * 
	 * @param presenceBased true if user must be available, false otherwise
	 */
	public void setPresenceBasedDelivery(boolean presenceBased) 
	{
		addField(ConfigureNodeFields.presence_based_delivery, FormField.TYPE_BOOLEAN);
		setAnswer(ConfigureNodeFields.presence_based_delivery.getFieldName(), presenceBased);
	}

	/**
	 * Gets the publishing model for the node, which determines who may publish to it.
	 * 
	 * @return The publishing model
	 */
	public PublishModel getPublishModel()
	{
		String value = getFieldValue(ConfigureNodeFields.publish_model);
		
		if (value == null)
			return null;
		else
			return PublishModel.valueOf(value);
	}

	/**
	 * Sets the publishing model for the node, which determines who may publish to it.
	 * 
	 * @param publish The enum representing the possible options for the publishing model
	 */
	public void setPublishModel(PublishModel publish) 
	{
		addField(ConfigureNodeFields.publish_model, FormField.TYPE_LIST_SINGLE);
		setAnswer(ConfigureNodeFields.publish_model.getFieldName(), getListSingle(publish.toString()));
	}
	
	/**
	 * Iterator over the multi user chat rooms that are specified as reply rooms.
	 * 
	 * @return The reply room JID's
	 */
	public Iterator<String> getReplyRoom()
	{
		return getFieldValues(ConfigureNodeFields.replyroom);
	}
	
	/**
	 * Sets the multi user chat rooms that are specified as reply rooms.
	 * 
	 * @param replyRooms The multi user chat room to use as reply rooms
	 */
	public void setReplyRoom(List<String> replyRooms) 
	{
		addField(ConfigureNodeFields.replyroom, FormField.TYPE_LIST_MULTI);
		setAnswer(ConfigureNodeFields.replyroom.getFieldName(), replyRooms);
	}
	
	/**
	 * Gets the specific JID's for reply to.
	 *  
	 * @return The JID's
	 */
	public Iterator<String> getReplyTo()
	{
		return getFieldValues(ConfigureNodeFields.replyto);
	}
	
	/**
	 * Sets the specific JID's for reply to.
	 * 
	 * @param replyTos The JID's to reply to
	 */
	public void setReplyTo(List<String> replyTos)
	{
		addField(ConfigureNodeFields.replyto, FormField.TYPE_LIST_MULTI);
		setAnswer(ConfigureNodeFields.replyto.getFieldName(), replyTos);
	}
	
	/**
	 * Gets the roster groups that are allowed to subscribe and retrieve items.
	 *  
	 * @return The roster groups
	 */
	public Iterator<String> getRosterGroupsAllowed()
	{
		return getFieldValues(ConfigureNodeFields.roster_groups_allowed);
	}
	
	/**
	 * Sets the roster groups that are allowed to subscribe and retrieve items.
	 * 
	 * @param groups The roster groups
	 */
	public void setRosterGroupsAllowed(List<String> groups)
	{
		addField(ConfigureNodeFields.roster_groups_allowed, FormField.TYPE_LIST_MULTI);
		setAnswer(ConfigureNodeFields.roster_groups_allowed.getFieldName(), groups);
	}
	
	/**
	 * Determines if subscriptions are allowed.
	 * 
	 * @return true if subscriptions are allowed, false otherwise
	 */
	public boolean isSubscibe()
	{
		return parseBoolean(getFieldValue(ConfigureNodeFields.subscribe));
	}

	/**
	 * Sets whether subscriptions are allowed.
	 * 
	 * @param subscribe true if they are, false otherwise
	 */
	public void setSubscribe(boolean subscribe)
	{
		addField(ConfigureNodeFields.subscribe, FormField.TYPE_BOOLEAN);
		setAnswer(ConfigureNodeFields.subscribe.getFieldName(), subscribe);
	}
	
	/**
	 * Gets the human readable node title.
	 * 
	 * @return The node title
	 */
	public String getTitle()
	{
		return getFieldValue(ConfigureNodeFields.title);
	}

	/**
	 * Sets a human readable title for the node.
	 * 
	 * @param title The node title
	 */
	public void setTitle(String title) 
	{
		addField(ConfigureNodeFields.title, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.title.getFieldName(), title);
	}
	
	/**
	 * The type of node data, usually specified by the namespace of the payload (if any).
	 * 
	 * @return The type of node data
	 */
	public String getDataType()
	{
		return getFieldValue(ConfigureNodeFields.type);
	}

	/**
	 * Sets the type of node data, usually specified by the namespace of the payload (if any).
	 * 
	 * @param type The type of node data
	 */
	public void setDataType(String type) 
	{
		addField(ConfigureNodeFields.type, FormField.TYPE_TEXT_SINGLE);
		setAnswer(ConfigureNodeFields.type.getFieldName(), type);
	}
	
	@Override
	public String toString()
	{
		StringBuilder result = new StringBuilder(getClass().getName() + " Content [");
		
		Iterator<FormField> fields = getFields();
		
		while (fields.hasNext())
		{
			FormField formField = fields.next();
			result.append('(');
			result.append(formField.getVariable());
			result.append(':');
			
			Iterator<String> values = formField.getValues();
			StringBuilder valuesBuilder = new StringBuilder();
				
			while (values.hasNext())
			{
				if (valuesBuilder.length() > 0)
					result.append(',');
				String value = (String)values.next();
				valuesBuilder.append(value);
			}
			
			if (valuesBuilder.length() == 0)
				valuesBuilder.append("NOT SET");
			result.append(valuesBuilder);
			result.append(')');
		}
		result.append(']');
		return result.toString();
	}

	static private boolean parseBoolean(String fieldValue)
	{
		return ("1".equals(fieldValue) || "true".equals(fieldValue));
	}

	private String getFieldValue(ConfigureNodeFields field)
	{
		FormField formField = getField(field.getFieldName());
		
		return formField.getValues().next();
	}

	private Iterator<String> getFieldValues(ConfigureNodeFields field)
	{
		FormField formField = getField(field.getFieldName());
		
		return formField.getValues();
	}

	private void addField(ConfigureNodeFields nodeField, String type)
	{
		String fieldName = nodeField.getFieldName();
		
		if (getField(fieldName) == null)
		{
			FormField field = new FormField(fieldName);
			field.setType(type);
			addField(field);
		}
	}

	private List<String> getListSingle(String value)
	{
		List<String> list = new ArrayList<String>(1);
		list.add(value);
		return list;
	}

}
