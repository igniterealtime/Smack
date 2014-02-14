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
package org.jivesoftware.smackx.pubsub.listener;

import org.jivesoftware.smackx.pubsub.ConfigurationEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;

/**
 * Defines the listener for a node being configured.
 * 
 * @see LeafNode#addConfigurationListener(NodeConfigListener)
 *
 * @author Robin Collier
 */
public interface NodeConfigListener
{
	/**
	 * Called whenever the node the listener
	 * is registered with is configured.
	 * 
	 * @param config The configuration details.
	 */
	void handleNodeConfiguration(ConfigurationEvent config);
}
