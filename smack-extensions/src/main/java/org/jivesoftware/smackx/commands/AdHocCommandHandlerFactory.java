/**
 *
 * Copyright 2008 Jive Software.
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
package org.jivesoftware.smackx.commands;

import java.lang.reflect.InvocationTargetException;

/**
 * A factory for creating ad-hoc command handlers. It's useful in cases where instantiation
 * of a command is more complicated than just using the default constructor. For example,
 * when arguments must be passed into the constructor or when using a dependency injection
 * framework. When a factory isn't used, you can provide the AdHocCommandManager
 * a Class object instead. For more details, see
 * {@link AdHocCommandManager#registerCommand(String, String, AdHocCommandHandlerFactory)}.
 *
 * @author Matt Tucker
 */
public interface AdHocCommandHandlerFactory {

    /**
     * Returns a new instance of an ad-hoc command handler.
     *
     * @param node the node of the ad-hoc command.
     * @param name the name of the ad-hoc command.
     * @param sessionId the session ID of the ad-hoc command.
     * @return a LocalCommand instance.
     * @throws InstantiationException if creating an instance failed.
     * @throws IllegalAccessException if creating an instance is not allowed.
     * @throws InvocationTargetException if a reflection-based method or constructor invocation threw.
     * @throws IllegalArgumentException if an illegal argument was given.
     */
    AdHocCommandHandler create(String node, String name, String sessionId)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException;

}
