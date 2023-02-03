/**
 *
 * Copyright 2002-2008 Jive Software, 2015-2022 Florian Schmaus
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
 * Smack includes built-in debugging consoles that will let you track all XML traffic between the client and server.
 * Further debuggers, besides those provide by smack-core, can be found in org.jivesoftware.smackx.debugger (note, that
 * this uses the smackx namespace, and not smack) provided by smack-debug.
 *
 * Debugging mode can be enabled in two different ways.
 *
 * Add the following line of code before creating new connections
 *
 * {@code SmackConfiguration.DEBUG = true;}
 *
 * Set the Java system property smack.debugEnabled to {@code true}. The system property can be set on the command line such as
 *
 * <pre>java -Dsmack.debugEnabled=true SomeApp</pre>
 */
package org.jivesoftware.smack.debugger;
