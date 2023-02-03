/**
 *
 * Copyright © 2016 Florian Schmaus
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
 * Smack's API for XMPP IoT (XEP-0323, -0324, -0325, -0347).
 * <p>
 * The Internet of Things (IoT) XEPs are an experimental open standard how XMPP
 * can be used for IoT. They currently consists of
 * </p>
 * <ul>
 * <li>XEP-0323 Sensor Data</li>
 * <li>XEP-0324 Provisioning</li>
 * <li>XEP-0325 Control</li>
 * <li>XEP-0326 Concentrators</li>
 * <li>XEP-0347 Discovery</li>
 * </ul>
 * <p>
 * Smack only supports a subset of the functionality described by the XEPs!
 * </p>
 * <h2>Thing Builder</h2>
 * <p>
 * The {@link org.jivesoftware.smackx.iot.Thing} class acts as basic entity
 * representing a single "Thing" which can be used to retrieve data from or to
 * send control commands to. `Things` are constructed using a builder API.
 * </p>
 * <h2>Reading data from things</h2>
 * <p>
 * For example, we can build a Thing which provides the current temperature with
 * </p>
 *
 * <pre><code>
 * Thing dataThing = Thing.builder().setKey(key).setSerialNumber(sn).setMomentaryReadOutRequestHandler(new ThingMomentaryReadOutRequest() {
 *     {@literal @}Override
 *     public void momentaryReadOutRequest(ThingMomentaryReadOutResult callback) {
 *         int temp = getCurrentTemperature();
 *         IoTDataField.IntField field = new IntField("temperature", temp);
 *         callback.momentaryReadOut(Collections.singletonList(field));
 *     }
 * }).build();
 * </code></pre>
 * <p>
 * While not strictly required, most things are identified via a key and serial
 * number. We also build the thing with a "momentary read out request handler"
 * which when triggered, retrieves the current temperature and reports it back
 * to the requestor.
 * </p>
 * <p>
 * After the `Thing` is built, it needs to be made available so that other
 * entities within the federated XMPP network can use it. Right now we only
 * install the Thing in the `IoTDataManager`, which means the thing will act on
 * read out requests but not be managed by a provisioning server.
 * </p>
 *
 * <pre>
 * <code>
 * IoTDataManager iotDataManager = IoTDataManager.getInstanceFor(connection);
 * iotDataManager.installThing(thing);
 * </code>
 * </pre>
 * <p>
 * The data can be read out also by using the <code>IoTDataManager</code>:
 * </p>
 *
 * <pre>{@code
 * FullJid jid = …
 * List<IoTFieldsExtension> values = iotDataManager.requestMomentaryValuesReadOut(jid);
 * }</pre>
 * <p>
 * Now you have to unwrap the `IoTDataField` instances from the
 * `IoTFieldsExtension`. Note that Smack currently only supports a subset of the
 * specified data types.
 * </p>
 * <h2>Controlling a thing</h2>
 * <p>
 * Things can also be controlled, e.g. to turn on a light. Let's create a thing
 * which can be used to turn the light on and off.
 * </p>
 *
 * <pre>
 * <code>
 * Thing controlThing = Thing.builder().setKey(key).setSerialNumber(sn).setControlRequestHandler(new ThingControlRequest() {
 *     {@literal @}Override
 *     public void processRequest(Jid from, Collection&lt;SetData&gt;} setData) throws XMPPErrorException {
 *         for (final SetData data : setData) {
 *             if (!data.getName().equals("light")) continue;
 *             if (!(data instanceof SetBoolData)) continue;
 *             SetBoolData boolData = (SetBoolData) data;
 *             setLight(boolData.getBooleanValue());
 *         }
 *     }
 * }).build();
 * </code>
 * </pre>
 * <p>
 * Now we have to install this thing into the `IoTControlManager`:
 * </p>
 *
 * <pre>
 * <code>
 * IoTControlManager iotControlManager = IoTControlManager.getInstanceFor(connection);
 * iotControlManager.installThing(thing);
 * </code>
 * </pre>
 * <p>
 * The `IoTControlManager` can also be used to control a thing:
 * </p>
 *
 * <pre>
 * <code>
 * FullJid jid = …
 * SetData setData = new SetBoolData("light", true);
 * iotControlManager.setUsingIq(jid, setData);
 * </code>
 * </pre>
 * <p>
 * Smack currently only supports a subset of the possible data types for set
 * data.
 * </p>
 * <h2>Discovery</h2>
 * <p>
 * You may have wondered how a full JIDs of things can be determined. One
 * approach is using the discovery mechanisms specified in XEP-0347. Smack
 * provides the `IoTDiscoveryManager` as an API for this.
 * </p>
 * <p>
 * For example, instead of just installing the previous things in the
 * `IoTDataManager` and/or `IoTControlManager`, we could also use the
 * `IoTDiscoveryManger` to register the thing with a registry. Doing this also
 * installs the thing in the `IoTDataManager` and the `IoTControlManager`.
 * </p>
 *
 * <pre>
 * <code>
 * IoTDiscoveryManager iotDiscoveryManager = IoTDiscoveryManager.getInstanceFor(connection);
 * iotDiscovyerManager.registerThing(thing);
 * </code>
 * </pre>
 * <p>
 * The registry will now make the thing known to a broader audience, and
 * available for a potential owner.
 * </p>
 * <p>
 * The `IoTDiscoveryManager` can also be used to claim, disown, remove and
 * unregister a thing.
 * </p>
 * <h2>Provisioning</h2>
 * <p>
 * Things can usually only be used by other things if they are friends. Since a
 * thing normally can't decide on its own if an incoming friendship request
 * should be granted or not, we can delegate this decision to a provisioning
 * service. Smack provides the `IoTProvisinoManager` to deal with friendship and
 * provisioning.
 * </p>
 * <p>
 * For example, if you want to befriend another thing:
 * </p>
 *
 * <pre>
 * <code>
 * BareJid jid = …
 * IoTProvisioningManager iotProvisioningManager = IoTProvisioningManager.getInstanceFor(connection);
 * iotProvisioningManager.sendFriendshipRequest(jid);
 * </code>
 * </pre>
 *
 *
 */
package org.jivesoftware.smackx.iot;
