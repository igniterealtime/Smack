/**
 *
 * Copyright © 2014 Florian Schmaus
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
 * Smack's API for XEP-0332: HTTP over XMPP transport.
 * <h2 id="discover-hoxt-support">Discover HOXT support</h2>
 * <p>
 * <strong>Description</strong>
 * </p>
 * <p>
 * Before using this extension you must ensure that your counterpart supports it also.
 * </p>
 * <p>
 * <strong>Usage</strong>
 * </p>
 * <p>
 * Once you have your <em><strong>ServiceDiscoveryManager</strong></em> you will be able to discover information
 * associated with an XMPP entity. To discover the information of a given XMPP entity send
 * <strong>discoverInfo(entityID)</strong> to your <em><strong>ServiceDiscoveryManager</strong></em> where entityID is
 * the ID of the entity. The message <strong>discoverInfo(entityID)</strong> will answer with an instance of
 * <em><strong>DiscoverInfo</strong></em> that contains the discovered information.
 * </p>
 * <p>
 * <strong>Examples</strong>
 * </p>
 * <p>
 * In this example we can see how to check if the counterpart supports HOXT:
 * </p>
 *
 * <pre>
 * <code>// Obtain the ServiceDiscoveryManager associated with my XMPPConnection
 * ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
 * // Get the information of a given XMPP entity, where entityID is a Jid
 * DiscoverInfo discoInfo = discoManager.discoverInfo(entityID);
 * // Check if room is HOXT is supported
 * boolean isSupported = discoInfo.containsFeature(&quot;urn:xmpp:http&quot;);</code>
 * </pre>
 *
 * <h2 id="iq-exchange">IQ exchange</h2>
 * <p>
 * <strong>Description</strong>
 * </p>
 * <p>
 * You can use IQ’s to perform HTTP requests and responses. This is applicable to relatively short requests and
 * responses (due to the limitation of XMPP message size).
 * </p>
 * <p>
 * <strong>Usage</strong>
 * </p>
 * <p>
 * First you need to register a <em><strong>StanzaListener</strong></em> to be able to handle intended IQs.
 * </p>
 * <p>
 * For the HTTP client you:
 * </p>
 * <ul>
 * <li>You create and send <em><strong>HttpOverXmppReq</strong></em> request.</li>
 * <li>Then you handle the <em><strong>HttpOverXmppResp</strong></em> response in your
 * <em><strong>StanzaListener</strong></em>.</li>
 * </ul>
 * <p>
 * For the HTTP server you:
 * </p>
 * <ul>
 * <li>You handle the <em><strong>HttpOverXmppReq</strong></em> requests in your
 * <em><strong>StanzaListener</strong></em>.</li>
 * <li>And create and send <em><strong>HttpOverXmppResp</strong></em> responses.</li>
 * </ul>
 * <p>
 * <strong>Examples</strong>
 * </p>
 * <p>
 * In this example we are an HTTP client, so we send a request (POST) and handle the response:
 * </p>
 *
 * <pre>
 * <code>
 * // create a request body
 * String urlEncodedMessage = &quot;I_love_you&quot;;
 *
 * // prepare headers
 * List&lt;Header&gt; headers = new ArrayList&lt;&gt;();
 * headers.add(new Header(&quot;Host&quot;, &quot;juliet.capulet.com&quot;));
 * headers.add(new Header(&quot;Content-Type&quot;, &quot;application/x-www-form-urlencoded&quot;));
 * headers.add(new Header(&quot;Content-Length&quot;, Integer.toString(urlEncodedMessage.length())));
 *
 * // provide body or request (not mandatory, - empty body is used for GET)
 * AbstractHttpOverXmpp.Text child = new AbstractHttpOverXmpp.Text(urlEncodedMessage);
 * AbstractHttpOverXmpp.Data data = new AbstractHttpOverXmpp.Data(child);
 *
 * // create request
 * HttpOverXmppReq req = HttpOverXmppReq.buider()
 *                             .setMethod(HttpMethod.POST)
 *                             .setResource(&quot;/mailbox&quot;)
 *                             .setHeaders(headers)
 *                             .setVersion(&quot;1.1&quot;)
 *                             .setData(data)
 *                             .build();
 *
 * // add to, where jid is the Jid of the individual the packet is sent to
 * req.setTo(jid);
 *
 * // send it
 * connection.sendIqWithResponseCallback(req, new StanzaListener() {
 *    public void processStanza(Stanza iq) {
 *         HttpOverXmppResp resp = (HttpOverXmppResp) iq;
 *         // check HTTP response code
 *         if (resp.getStatusCode() == 200) {
 *             // get content of the response
 *             NamedElement child = resp.getData().getChild();
 *             // check which type of content of the response arrived
 *             if (child instanceof AbstractHttpOverXmpp.Xml) {
 *                 // print the message and anxiously read if from the console ;)
 *                 System.out.println(((AbstractHttpOverXmpp.Xml) child).getText());
 *             } else {
 *                 // process other AbstractHttpOverXmpp data child subtypes
 *             }
 *         }
 *     }
 * });
 * </code>
 * </pre>
 */
package org.jivesoftware.smackx.hoxt;
