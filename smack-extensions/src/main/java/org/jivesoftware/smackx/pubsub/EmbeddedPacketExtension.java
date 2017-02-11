/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub;

import java.util.List;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.PacketParserUtils;

/**
 * This interface defines {@link ExtensionElement} implementations that contain other
 * extensions.  This effectively extends the idea of an extension within one of the 
 * top level {@link Stanza} types to consider any embedded element to be an extension
 * of its parent.  This more easily enables the usage of some of Smacks parsing
 * utilities such as {@link PacketParserUtils#parseExtensionElement(String, String, org.xmlpull.v1.XmlPullParser)} to be used
 * to parse any element of the XML being parsed.
 * 
 * <p>Top level extensions have only one element, but they can have multiple children, or
 * their children can have multiple children.  This interface is a way of allowing extensions 
 * to be embedded within one another as a partial or complete one to one mapping of extension 
 * to element.
 * 
 * @author Robin Collier
 */
public interface EmbeddedPacketExtension extends ExtensionElement
{
    /**
     * Get the list of embedded {@link ExtensionElement} objects.
     *  
     * @return List of embedded {@link ExtensionElement}
     */
    List<ExtensionElement> getExtensions();
}
