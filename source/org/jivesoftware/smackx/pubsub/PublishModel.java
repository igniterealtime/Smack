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
 * Determines who may publish to a node.  Denotes possible values 
 * for {@link ConfigureForm#setPublishModel(PublishModel)}.
 * 
 * @author Robin Collier
 */
public enum PublishModel
{
	/** Only publishers may publish */
	publishers,
	
	/** Only subscribers may publish */
	subscribers,
	
	/** Anyone may publish */
	open;
}
