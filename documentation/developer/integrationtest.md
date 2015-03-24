Smack's Integration Test Framework
==================================

Introduction
------------

Smack's Integration Test Framwork is used ot run a set of tests against a real XMPP service.
The framework discovers on startup the available tests by reflection.

Quickstart
----------

You can run the framework against an XMPP service with

```bash
$ gradle integrationTest -Dsinttest.service=my.xmppservice.org
```

Configuration
-------------

The framework is configured with a standard Java properties file.
This file simply contains key/value pairs, which are separated by an equals sign ("=").
The most important configuration value is the `service` value, it's also the only required setting.

The file properties can be overridden with Java system properties.
The name of a system property that is used by the framework needs to be prefixed with `sinttest.` (*S*mack *Int*egration *Test* Framework).
For example the `service` property becomes `sinttest.service`.

### Minimal example **properties** file

```bash
service=example.org
```

### Another example **properties** file

```bash
service=example.org
serviceTlsPin=serviceTlsPin=CERTSHA256:2F:92:C9:4D:30:58:E1:05:21:9A:57:59:5F:6E:25:9A:0F:BF:FF:64:1A:C3:4B:EC:06:7D:4A:6F:0A:D5:21:85
debug=true
```

### Framework properties

| Name                 |                                           |
|----------------------|-------------------------------------------|
| service              | XMPP service to run the tests on          |
| serviceTlsPin        | TLS Pin (used by java-pinning)            |
| securityMode         | Either 'required' or disabled'            |
| replyTimeout         | In milliseconds                           |
| accountOneUsername   | Username of the first XMPP account        |
| accountOnePassword   | Password of the first XMPP account        |
| accountTwoUsername   | Username of the second XMPP account       |
| accountTwoPassword   | Password of the second XMPP account       |
| debug                | 'true' to enable debug output             |
| enabledTests         | List of enabled tests                     |
| disabledTests        | List of disabled tests                    |
| testPackages         | List of packages with tests               |

Overview of the components
--------------------------

Package `org.igniterealtime.smack.inttest`

### `SmackIntegrationTestFramework`

Contains `public static void main` method, i.e. the entry point for the framework.
Here the available integration tests are discovered by means of reflections, the configured is read and a `IntegrationTestEnvironment` instance created, include the XMPPConnections.

### `AbstractSmackIntegrationTest`

The base class that integration tests need to subclass.

### `AbstractSmackLowLevelIntegrationTest`

Allows low level integration test, i.e. ever test method will have it's on exclusive XMPPTCPConnection instances.

### `IntegrationTestEnvironment`

The environment, e.g. the `XMPPConnections` provided to the integration tests by the framework. Note that for convenience `AbstractSmackIntegrationTest` contains some of those as protected members.

### `SmackIntegrationTest`

An annotation that needs to be added to all methods that represent a single integration test.
Annotated integration test methods must not take any arguments (i.e. their parameter count is 0), and should return void as it's not evaluated in any way.
The methods are supposed to throw an exception if their integration test fails.

### `TestNotPossibleException`

Can be thrown by test methods or constructors to signal that their test it no possible, e.g. because the service does not support the required feature.

Running the integration tests
-----------------------------

Smack's Gradle build system is configured with a special task called `integrationTest`, which means you can run the tests simply with

```bash
$ gradle integrationTest
```
If one of `accountOneUsername`, `accountOnePassword`, `accountTwoUsername` or `accountTwoPassword` is not configured, then the framework will automatically create the accounts on the service (if account registration is supported and enabled).
If the accounts got created, then they will also be deleted at the end of the test.

Implementing Integration Tests
------------------------------

Create a new class which extends `AbstractSmackIntegrationTest`.
Every non-static method, including the constructor, of this class will have two XMPPConnections available to perform the integration tests with: `conOne` and `conTwo`.
You can use the constructor to check if the XMPP service does provide the required XMPP feature.
If it does not, simply throw a `TestNotPossibleException`.

Test methods must be `public`, take zero arguments i.e. declare no parameters and be annoated with `@SmackIntegrationTest`.
If the test method is not able to perform a test then it should throw a `TestNotPossibleException`.

### Rules for integration tests

Tests should not leave any traces on the service if they are finished, i.e. the service state at the end of the test must be equal to the state of the beginning.
It must be possible to run the tests in parallel.

### Why are there two mechanisms to signal that the test is not possible?

Because the XMPP service may provide a component that is required to perform a certain integration test, but that component may not support all features.
For example, the XMPP service may provides a PubSub (XEP-60) component, but this component may not support all features of XEP-60.

### Low-Level Integration Tests

Classes that implement low-level integration tests need to sublcass `AbstractSmackLowLevelIntegrationTest`.
The test methods can declare as many parameters as they need to, but every parameter must be of type `XMPPTCPConnection`.
The framework will automatically create, register and login the connections.
After the test is finished, the connections will be unregistered with the XMPP service and terminated.

Running your own integration tests
----------------------------------

The framework can be used to run your own tests.
Simply set the `testPackages` property to a comma separated list of package names where the framework should look for integration tests.

Example:

```bash
$ gradle integrationTest -Dsinttest.service=my.xmppserivce.org -Dsinttest.testPackages=org.mypackage,org.otherpackage
```
