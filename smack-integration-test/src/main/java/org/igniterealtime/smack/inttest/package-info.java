/**
 *
 * Copyright 2015-2023 Florian Schmaus
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
 * The Smack Integration Test Framework (sinttest) used to run a set of tests against a real XMPP service. The framework
 * discovers on start-up the available tests by reflection.
 * <h2>Quickstart</h2>
 * <p>
 * You can run the framework against an XMPP service with
 * </p>
 *
 * <pre>
 * $ gradle integrationTest -Dsinttest.service=my.xmppservice.org
 * </pre>
 * <p>
 * Note that the service needs to have In-Band Registration (IBR) enabled.
 * </p>
 * <p>
 * A better alternative to IBR is using XEP-0133: Service Administration to create the throw away accounts used by the
 * integration test framework. Simply use
 * </p>
 *
 * <pre>
 * $ gradle integrationTest -Dsinttest.service=my.xmppservice.org \
 *                          -Dsinttest.adminAccountUsername=admin \
 *                          -Dsinttest.adminAccountPassword=aeR0Wuub
 * </pre>
 * <p>
 * to run Smack's integration test framework against my.xmppservice.org with an admin account named 'admin' and
 * 'aeR0Wuub' as password.
 * </p>
 * <h2>Configuration</h2>
 * <p>
 * The framework is configured with a standard Java properties file. This file simply contains key/value pairs, which
 * are separated by an equals sign ("="). The most important configuration value is the `service` value, it's also the
 * only required setting.
 * </p>
 * <p>
 * The file properties can be overridden with Java system properties. The name of a system property that is used by the
 * framework needs to be prefixed with 'sinttest.' (*S*mack *Int*egration *Test* Framework). For example the `service`
 * property becomes `sinttest.service`.
 * </p>
 * <h3>Minimal example 'properties' file</h3>
 *
 * <pre>
 * service = example.org
 * </pre>
 *
 * <h3>Another example 'properties' file</h3>
 *
 * <pre>
 * service=example.org
 * serviceTlsPin=CERTSHA256:2F:92:C9:4D:30:58:E1:05:21:9A:57:59:5F:6E:25:9A:0F:BF:FF:64:1A:C3:4B:EC:06:7D:4A:6F:0A:D5:21:85
 * debugger=console
 * </pre>
 *
 * <h3>Framework properties</h3>
 * <table border="1">
 * <caption>Properties of the Smack Integration Test Framework</caption>
 * <tr>
 * <th>Name</th>
 * <th>Description</th>
 * </tr>
 * <tr>
 * <td>service</td>
 * <td>XMPP service to run the tests on</td>
 * </tr>
 * <tr>
 * <td>serviceTlsPin</td>
 * <td>TLS Pin (used by <a href="https://github.com/Flowdalic/java-pinning">java-pinning</a>)</td>
 * </tr>
 * <tr>
 * <td>securityMode</td>
 * <td>Either ‘required’ or ‘disabled’</td>
 * </tr>
 * <tr>
 * <td>replyTimeout</td>
 * <td>In milliseconds</td>
 * </tr>
 * <tr>
 * <td>adminAccountUsername</td>
 * <td>Username of the XEP-0133 Admin account</td>
 * </tr>
 * <tr>
 * <td>adminAccountPassword</td>
 * <td>Password of the XEP-0133 Admin account</td>
 * </tr>
 * <tr>
 * <td>accountOneUsername</td>
 * <td>Username of the first XMPP account</td>
 * </tr>
 * <tr>
 * <td>accountOnePassword</td>
 * <td>Password of the first XMPP account</td>
 * </tr>
 * <tr>
 * <td>accountTwoUsername</td>
 * <td>Username of the second XMPP account</td>
 * </tr>
 * <tr>
 * <td>accountTwoPassword</td>
 * <td>Password of the second XMPP account</td>
 * </tr>
 * <tr>
 * <td>accountThreeUsername</td>
 * <td>Username of the third XMPP account</td>
 * </tr>
 * <tr>
 * <td>accountThreePassword</td>
 * <td>Password of the third XMPP account</td>
 * </tr>
 * <tr>
 * <td>debugger</td>
 * <td>‘console’ for console debugger, ‘enhanced’ for the enhanced debugger</td>
 * </tr>
 * <tr>
 * <td>enabledTests</td>
 * <td>List of enabled tests</td>
 * </tr>
 * <tr>
 * <td>disabledTests</td>
 * <td>List of disabled tests</td>
 * </tr>
 * <tr>
 * <td>defaultConnection</td>
 * <td>Nickname of the default connection</td>
 * </tr>
 * <tr>
 * <td>enabledConnections</td>
 * <td>List of enabled connection’s nicknames</td>
 * </tr>
 * <tr>
 * <td>disabledConnections</td>
 * <td>List of disabled connection’s nicknames</td>
 * </tr>
 * <tr>
 * <td>testPackages</td>
 * <td>List of packages with tests</td>
 * </tr>
 * <tr>
 * <td>verbose</td>
 * <td>If <code>true</code> set output to verbose</td>
 * </tr>
 * <tr>
 * <td>dnsResolver</td>
 * <td>One of ‘minidns’, ‘javax’ or ‘dnsjava’. Defaults to ‘minidns’.</td>
 * </tr>
 * </table>
 * <h3>Where to place the properties file</h3>
 * <p>
 * The framework will first load the properties file from <code>~/.config/smack-integration-test/properties</code>.
 * </p>
 * <h3>Running selected tests only</h3>
 * <p>
 * Using <code>enabledTests</code> is is possible to run only selected tests. The tests can be selected on a per class
 * base or by specifying concrete test methods. In the latter case, the methods must be qualified by a (simple) class
 * name.
 * </p>
 * <p>
 * For example:
 * </p>
 *
 * <pre>
 * $ gradle integrationTest -Dsinttest.enabledTests=SoftwareInfoIntegrationTest.test
 * </pre>
 * <p>
 * will only run the <code>test()</code> method of <code>SoftwareInfoIntegrationTest</code>, whereas
 * </p>
 *
 * <pre>
 * $ gradle integrationTest -Dsinttest.enabledTests=SoftwareInfoIntegrationTest
 * </pre>
 * <p>
 * would run all tests defined in the <code>SoftwareInfoIntegrationTest</code> class.
 * </p>
 * <h2>Overview of the components</h2>
 * <p>
 * Package <code>org.igniterealtime.smack.inttest</code>
 * </p>
 * <h3>SmackIntegrationTestFramework</h3>
 * <p>
 * Contains <code>public static void main</code> method, i.e. the entry point for the framework. Here the available
 * integration tests are discovered by means of reflection, the configuration is read and a
 * <code>IntegrationTestEnvironment</code> instance created, which includes the XMPP connections.
 * </p>
 * <h3>AbstractSmackIntegrationTest</h3>
 * <p>
 * The base class that integration tests need to subclass.
 * </p>
 * <h3>AbstractSmackLowLevelIntegrationTest</h3>
 * <p>
 * Allows low level integration test, i.e. every test method will have its own exclusive XMPPTCPConnection instances.
 * </p>
 * <h3>AbstractSmackSpecificLowLevelIntegrationTest</h3>
 * <p>
 * Operates, like <code>AbstractSmackLowLevelIntegrationTest</code>, on its own <code>XMPPConnection</code> instances,
 * but is limited to a particular type of <code>XMPPConnection</code>.
 * </p>
 * <h3>IntegrationTestEnvironment</h3>
 * <p>
 * The environment, e.g. the `XMPPConnections` provided to the integration tests by the framework. Note that for
 * convenience `AbstractSmackIntegrationTest` contains some of those as protected members.
 * </p>
 * <h3>SmackIntegrationTest</h3>
 * <p>
 * An annotation that needs to be added to all methods that represent a single integration test. Annotated integration
 * test methods must not take any arguments, i.e., their parameter count is 0, and should return void as it's not
 * evaluated in any way. The methods are supposed to throw an exception if their integration test fails.
 * </p>
 * <h3>TestNotPossibleException</h3>
 * <p>
 * Can be thrown by test methods or constructors to signal that their test is not possible, e.g. because the service
 * does not support the required feature.
 * </p>
 * <h2>Running the integration tests</h2>
 * <p>
 * Smack's Gradle build system is configured with a special task called `integrationTest`, which means you can run the
 * tests simply with
 * </p>
 *
 * <pre>{@code
 * $ gradle integrationTest -Dsinttest.service=my.xmppservice.org
 * }</pre>
 * <p>
 * If one of <code>accountOneUsername</code>, <code>accountOnePassword</code>, <code>accountTwoUsername</code> or
 * <code>accountTwoPassword</code> is not configured, then the framework will automatically create the accounts on the
 * service. Of course this requires account registration (IBR) to be enabled. If the accounts got created automatically
 * by the framework, then they will also be deleted at the end of the test.
 * </p>
 * <h2>Implementing Integration Tests</h2>
 * <p>
 * Create a new class which extends <code>AbstractSmackIntegrationTest</code>. Every non-static method, including the
 * constructor, of this class will have two XMPPConnections available to perform the integration tests with:
 * <code>conOne</code> and <code>conTwo</code>. You can use the constructor to check if the XMPP service does provide
 * the required XMPP feature. If it does not, simply throw a <code>TestNotPossibleException</code>.
 * </p>
 * <p>
 * Test methods must be <code>public</code>, take zero arguments i.e. declare no parameters and be annoated with
 * <code>@SmackIntegrationTest</code>. If the test method is not able to perform a test then it should throw a
 * <code>TestNotPossibleException</code>.
 * </p>
 * <h3>Rules for integration tests</h3>
 * <p>
 * Tests should not leave any traces on the service if they are finished, i.e. the service state at the end of the test
 * must be equal to the state of the beginning. It must be possible to run the tests in parallel.
 * </p>
 * <h3>Why are there two mechanisms to signal that the test is not possible?</h3>
 * <p>
 * Because the XMPP service may provide a component that is required to perform a certain integration test, but that
 * component may not support all features. For example, the XMPP service may provides a PubSub (XEP-0060) component, but
 * this component may not support all features of XEP-0060.
 * </p>
 * <h3>Low-Level Integration Tests</h3>
 * <p>
 * Classes that implement low-level integration tests need to sublcass
 * {@link org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest}. The test methods can declare as many
 * parameters as they need to, but every parameter must be of type <code>XMPPTCPConnection</code>. The framework will
 * automatically create, register and login the connections. After the test is finished, the connections will be
 * unregistered with the XMPP service and terminated.
 * </p>
 * <h2>Debugging Integration Tests</h2>
 * <p>
 * A test, like any other code, may not be perfect on the first attempt, and you may require more information in order
 * to ascertain quite what's wrong.
 * </p>
 * <h3>Smack Debugger Options</h3>
 * <p>
 * As described in the package documentation of org.jivesoftware.smack.debugger, there are two built-in debuggers that
 * could surface you more information. Using the 'enhanced' debugger config option listed above, you'll get the Smack
 * Debug Window launching when your tests launch, and you'll get a stanza-by-stanza account of what happened on each
 * connection, hopefully enough to diagnose what went wrong.
 * </p>
 * <h3>Debugging in the IDE</h3>
 * <p>
 * If the output isn't enough, you may need to debug and inspect running code within the IDE. Depending on the IDE, in
 * order to get execution to pause at your breakpoints, you may need to switch your configuration. Instead of running
 * `gradle integrationTest`, instead run the `SmackIntegrationTestFramework` class directly with the same command-line
 * options.
 * </p>
 * <h2>Running Your Own Integration Tests</h2>
 * <p>
 * The framework can be used to run your own tests residing outside of the framework's default package scope. Simply set
 * the <code>testPackages</code> property to a comma separated list of package names where the framework should look for
 * integration tests.
 * </p>
 * <p>
 * Example:
 * </p>
 *
 * <pre>{@code
 * $ gradle integrationTest -Dsinttest.service=my.xmppserivce.org -Dsinttest.testPackages=org.mypackage,org.otherpackage
 * }</pre>
 */
package org.igniterealtime.smack.inttest;
