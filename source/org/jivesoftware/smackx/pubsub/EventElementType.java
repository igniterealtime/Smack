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

/**
 * This enumeration defines the possible event types that are supported within pubsub
 * event messages.
 * 
 * @author Robin Collier
 */
public enum EventElementType
{
	/** A node has been associated or dissassociated with a collection node */
	collection, 

	/** A node has had its configuration changed */
	configuration, 
	
	/** A node has been deleted */
	delete, 
	
	/** Items have been published to a node */
	items, 
	
	/** All items have been purged from a node */
	purge, 
	
	/** A node has been subscribed to */
	subscription
}
