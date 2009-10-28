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
 * This enumeration represents the access models for the pubsub node
 * as defined in the pubsub specification section <a href="http://xmpp.org/extensions/xep-0060.html#registrar-formtypes-config">16.4.3</a>
 * 
 * @author Robin Collier
 */
public enum AccessModel
{
	/** Anyone may subscribe and retrieve items	 */
	open,

	/** Subscription request must be approved and only subscribers may retrieve items */
	authorize,
	
	/** Anyone with a presence subscription of both or from may subscribe and retrieve items */
	presence,
	
	/** Anyone in the specified roster group(s) may subscribe and retrieve items */
	roster,
	
	/** Only those on a whitelist may subscribe and retrieve items */
	whitelist;
}
