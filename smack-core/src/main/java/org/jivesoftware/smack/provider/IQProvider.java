/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.provider;

import org.jivesoftware.smack.packet.IQ;

/**
 * An abstract class for parsing custom IQ packets. Each IQProvider must be registered with
 * the ProviderManager class for it to be used. Every implementation of this
 * abstract class <b>must</b> have a public, no-argument constructor.
 *
 * @author Matt Tucker
 */
public abstract class IQProvider<I extends IQ> extends Provider<I> {

}
