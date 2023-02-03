/**
 *
 * Copyright 2015 Florian Schmaus
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

/**
 * Smacks implementation for attaching arbitrary properties to packets according to
 * https://docs.jivesoftware.com/smack/latest/documentation/properties.html.
 * <p>
 * Smack provides an easy mechanism for attaching arbitrary properties to packets. Each property has a String name, and
 * a value that is a Java primitive (int, long, float, double, boolean) or any Serializable object (a Java object is
 * Serializable when it implements the Serializable interface).
 * </p>
 * <h2 id="using-the-api">Using the API</h2>
 * <p>
 * All major objects have property support, such as Message objects. The following code demonstrates how to set
 * properties:
 * </p>
 *
 * <pre>
 * <code>Message message = chat.createMessage();
JivePropertiesExtension jpe = new JivePropertiesExtension();
// Add a Color object as a property._
jpe.setProperty(&quot;favoriteColor&quot;, new Color(0, 0, 255));
// Add an int as a property._
jpe.setProperty(&quot;favoriteNumber&quot;, 4);
// Add the JivePropertiesExtension to the message packet_
message.addStanzaExtension(jpe);
chat.sendMessage(message);</code>
 * </pre>
 * <p>
 * Getting those same properties would use the following code:
 * </p>
 *
 * <pre>
 * <code>
 * Message message = chat.nextMessage();
 * // Get the JivePropertiesExtension_
 * JivePropertiesExtension jpe = message.getExtension(JivePropertiesExtension.NAMESPACE);
 * // Get a Color object property._
 * Color favoriteColor = (Color)jpe.getProperty(&quot;favoriteColor&quot;);
 * // Get an int property. Note that properties are always returned as
 * // Objects, so we must cast the value to an Integer, then convert
 * // it to an int._
 * int favoriteNumber = ((Integer)jpe.getProperty(&quot;favoriteNumber&quot;)).intValue();
 * </code>
 * </pre>
 * <p>
 * For convenience <code>JivePropertiesManager</code> contains two helper methods namely
 * <code>addProperty(Stanza packet, String name, Object value)</code> and
 * <code>getProperty(Stanza packet, String name)</code>.
 * </p>
 * <h2 id="objects-as-properties">Objects as Properties</h2>
 * <p>
 * Using objects as property values is a very powerful and easy way to exchange data. However, you should keep the
 * following in mind:
 * </p>
 * <ul>
 * <li>When you send a Java object as a property, only clients running Java will be able to interpret the data. So,
 * consider using a series of primitive values to transfer data instead.</li>
 * <li>Objects sent as property values must implement Serialiable. Additionally, both the sender and receiver must have
 * identical versions of the class, or a serialization exception will occur when de-serializing the object.</li>
 * <li>Serialized objects can potentially be quite large, which will use more bandwidth and server resources.</li>
 * </ul>
 * <h2 id="xml-format">XML Format</h2>
 * <p>
 * The current XML format used to send property data is not a standard, so will likely not be recognized by clients not
 * using Smack. The XML looks like the following (comments added for clarity):
 * </p>
 *
 * <pre>
 * <code>
 * &lt;!-- All properties are in a x block. --&gt;
 * &lt;properties xmlns=&quot;http://www.jivesoftware.com/xmlns/xmpp/properties&quot;&gt;
 *     &lt;!-- First, a property named &quot;prop1&quot; that&#39;s an integer. --&gt;
 *     &lt;property&gt;
 *         &lt;name&gt;prop1&lt;/name&gt;
 *         &lt;value type=&quot;integer&quot;&gt;123&lt;/value&gt;
 *     &lt;property&gt;
 *     &lt;!-- Next, a Java object that&#39;s been serialized and then converted
 *          from binary data to base-64 encoded text. --&gt;
 *     &lt;property&gt;
 *         &lt;name&gt;blah2&lt;/name&gt;
 *         &lt;value type=&quot;java-object&quot;&gt;adf612fna9nab&lt;/value&gt;
 *     &lt;property&gt;
 * &lt;/properties&gt;
 * </code>
 * </pre>
 * <p>
 * The currently supported types are: <code>integer</code>, <code>long</code>, <code>float</code>, <code>double</code>,
 * <code>boolean</code>, <code>string</code>, and <code>java-object</code>.
 * </p>
 */
package org.jivesoftware.smackx.jiveproperties;
