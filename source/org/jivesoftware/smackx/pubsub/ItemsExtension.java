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

import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * This class is used to for multiple purposes.  
 * <li>It can represent an event containing a list of items that have been published
 * <li>It can represent an event containing a list of retracted (deleted) items.
 * <li>It can represent a request to delete a list of items.
 * <li>It can represent a request to get existing items.
 * 
 * <p><b>Please note, this class is used for internal purposes, and is not required for usage of 
 * pubsub functionality.</b>
 * 
 * @author Robin Collier
 */
public class ItemsExtension extends NodeExtension implements EmbeddedPacketExtension
{
	protected ItemsElementType type;
	protected String attValue;
	protected List<? extends PacketExtension> items;

	public enum ItemsElementType
	{
		/** An items element, which has an optional <b>max_items</b> attribute when requesting items */
		items(PubSubElementType.ITEMS, "max_items"),
		
		/** A retract element, which has an optional <b>notify</b> attribute when publishing deletions */
		retract(PubSubElementType.RETRACT, "notify");
		
		private PubSubElementType elem;
		private String att;
		
		private ItemsElementType(PubSubElementType nodeElement, String attribute)
		{
			elem = nodeElement;
			att = attribute;
		}
		
		public PubSubElementType getNodeElement()
		{
			return elem;
		}

		public String getElementAttribute()
		{
			return att;
		}
	}

	/**
	 * Construct an instance with a list representing items that have been published or deleted.
	 * 
	 * <p>Valid scenarios are:
	 * <li>Request items from node - itemsType = {@link ItemsElementType#items}, items = list of {@link Item} and an
	 * optional value for the <b>max_items</b> attribute.
	 * <li>Request to delete items - itemsType = {@link ItemsElementType#retract}, items = list of {@link Item} containing
	 * only id's and an optional value for the <b>notify</b> attribute.
	 * <li>Items published event - itemsType = {@link ItemsElementType#items}, items = list of {@link Item} and 
	 * attributeValue = <code>null</code>
	 * <li>Items deleted event -  itemsType = {@link ItemsElementType#items}, items = list of {@link RetractItem} and 
	 * attributeValue = <code>null</code> 
	 * 
	 * @param itemsType Type of representation
	 * @param nodeId The node to which the items are being sent or deleted
	 * @param items The list of {@link Item} or {@link RetractItem}
	 * @param attributeValue The value of the <b>max_items</b>  
	 */
	public ItemsExtension(ItemsElementType itemsType, String nodeId, List<? extends PacketExtension> items, String attributeValue)
	{
		super(itemsType.getNodeElement(), nodeId);
		type = itemsType;
		this.items = items;
		attValue = attributeValue;
	}
	
	/**
	 * Constructs a request to get items from the node as defined in the first scenario
	 * in {@link #ItemsExtension(ItemsElementType, String, List, String)}
	 * 
	 * @param nodeId The node the items will be requested from
	 * @param maxItems The limit on the number of items to retrieve (null for all)
	 */
	public ItemsExtension(String nodeId, Integer maxItems)
	{
		this(ItemsElementType.items, nodeId, null, maxItems == null ? null : maxItems.toString());
	}
	
	/**
	 * Get the type of element
	 * 
	 * @return The element type
	 */
	public ItemsElementType getItemsElementType()
	{
		return type;
	}
	
	public List<PacketExtension> getExtensions()
	{
		return (List<PacketExtension>)getItems();
	}
	
	/**
	 * Gets the items related to the type of request or event.
	 * 
	 * return List of {@link Item}, {@link RetractItem}, or null
	 */
	public List<? extends PacketExtension> getItems()
	{
		return items;
	}

	/**
	 * Gets the value of the optional attribute related to the {@link ItemsElementType}.
	 * 
	 * @return The attribute value
	 */
	public String getAttributeValue()
	{
		return attValue;
	}
	
	@Override
	public String toXML()
	{
		if (((items == null) || (items.size() == 0)) && (attValue == null))
		{
			return super.toXML();
		}
		else
		{
			StringBuilder builder = new StringBuilder("<");
			builder.append(getElementName());
			builder.append(" node='");
			builder.append(getNode());
			
			if (attValue != null)
			{
				builder.append("' ");
				builder.append(type.getElementAttribute());
				builder.append("='");
				builder.append(attValue);
				builder.append("'>");
			}
			else
			{
				builder.append("'>");
				for (PacketExtension item : items)
				{
					builder.append(item.toXML());
				}
			}
			
			builder.append("</");
			builder.append(getElementName());
			builder.append(">");
			return builder.toString();
		}
	}

	@Override
	public String toString()
	{
		return getClass().getName() + "Content [" + toXML() + "]";
	}

}
