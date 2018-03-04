Internet of Things (XEP-0323, -0324, -0325, -0347)
==================================================

The Internet of Things (IoT) XEPs are an experimental open standard on how XMPP can be used for IoT. They currently consist of
- XEP-0323 Sensor Data
- XEP-0324 Provisioning
- XEP-0325 Control
- XEP-0326 Concentrators
- XEP-0347 Discovery

Smack only supports a subset of the functionality described by the XEPs!

Thing Builder
-------------

The `org.jivesoftware.smackx.iot.Thing` class acts as basic entity representing a single "Thing" which can be used to retrieve data from or to send control commands to. `Things` are constructed using a builder API.


Reading data from things
------------------------

For example, we can build a Thing which provides the current temperature with

```java
Thing dataThing = Thing.builder().setKey(key).setSerialNumber(sn).setMomentaryReadOutRequestHandler(new ThingMomentaryReadOutRequest() {
    @Override
    public void momentaryReadOutRequest(ThingMomentaryReadOutResult callback) {
        int temp = getCurrentTemperature();
        IoTDataField.IntField field = new IntField("temperature", temp);
        callback.momentaryReadOut(Collections.singletonList(field));
    }
}).build();
```

While not strictly required, most things are identified via a key and serial number. We also build the thing with a "momentary read out request handler" which when triggered, retrieves the current temperature and reports it back to the requestor.

After the `Thing` is built, it needs to be made available so that other entities within the federated XMPP network can use it. Right now we only install the Thing in the `IoTDataManager`, which means the thing will act on read out requests but not be managed by a provisioning server.

```java
IoTDataManager iotDataManager = IoTDataManager.getInstanceFor(connection);
iotDataManager.installThing(thing);
```

The data can be read out also by using the `IoTDataManager`:

```java
FullJid jid = …
List<IoTFieldsExtension> values = iotDataManager.requestMomentaryValuesReadOut(jid);
```

Now you have to unwrap the `IoTDataField` instances from the `IoTFieldsExtension`. Note that Smack currently only supports a subset of the specified data types.

Controlling a thing
-------------------

Things can also be controlled, e.g. to turn on a light. Let's create a thing which can be used to turn the light on and off.

```java
Thing controlThing = Thing.builder().setKey(key).setSerialNumber(sn).setControlRequestHandler(new ThingControlRequest() {
    @Override
    public void processRequest(Jid from, Collection<SetData> setData) throws XMPPErrorException {
        for (final SetData data : setData) {
            if (!data.getName().equals("light")) continue;
            if (!(data instanceof SetBoolData)) continue;
            SetBoolData boolData = (SetBoolData) data;
            setLight(boolData.getBooleanValue());
        }
    }
}).build();
```

Now we have to install this thing into the `IoTControlManager`:

```java
IoTControlManager iotControlManager = IoTControlManager.getInstanceFor(connection);
iotControlManager.installThing(thing);
```

The `IoTControlManager` can also be used to control a thing:

```java
FullJid jid = …
SetData setData = new SetBoolData("light", true);
iotControlManager.setUsingIq(jid, setData);
```

Smack currently only supports a subset of the possible data types for set data.

Discovery
---------

You may have wondered how a full JIDs of things can be determined. One approach is using the discovery mechanisms specified in XEP-0347. Smack provides the `IoTDiscoveryManager` as an API for this.

For example, instead of just installing the previous things in the `IoTDataManager` and/or `IoTControlManager`, we could also use the `IoTDiscoveryManger` to register the thing with a registry. Doing this also installs the thing in the `IoTDataManager` and the `IoTControlManager`.

```java
IoTDiscoveryManager iotDiscoveryManager = IoTDiscoveryManager.getInstanceFor(connection);
iotDiscovyerManager.registerThing(thing);
```

The registry will now make the thing known to a broader audience, and available for a potential owner.

The `IoTDiscoveryManager` can also be used to claim, disown, remove and unregister a thing.

Provisioning
------------

Things can usually only be used by other things if they are friends. Since a thing normally can't decide on its own if an incoming friendship request should be granted or not, we can delegate this decision to a provisioning service. Smack provides the `IoTProvisinoManager` to deal with friendship and provisioning.

For example, if you want to befriend another thing:

```java
BareJid jid = …
IoTProvisioningManager iotProvisioningManager = IoTProvisioningManager.getInstanceFor(connection);
iotProvisioningManager.sendFriendshipRequest(jid);
```
