/**
 *
 * Copyright 2014 Vyacheslav Blinov
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


package org.jivesoftware.smack.debugger;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReflectionDebuggerFactory implements SmackDebuggerFactory {
    private static final Logger LOGGER = Logger.getLogger(ReflectionDebuggerFactory.class.getName());
    private static final String DEBUGGER_CLASS_PROPERTY_NAME = "smack.debuggerClass";

    /**
     * Possible default debugger implementations. The order of enumeration is the one in which we try
     * to instantiate these.
     */
    private static final String[] DEFAULT_DEBUGGERS = new String[] {
            "org.jivesoftware.smackx.debugger.EnhancedDebugger",
            "org.jivesoftware.smackx.debugger.android.AndroidDebugger",
            "org.jivesoftware.smack.debugger.ConsoleDebugger",
            "org.jivesoftware.smack.debugger.LiteDebugger",
            "org.jivesoftware.smack.debugger.JulDebugger",
            };

    /**
     * Sets custom debugger class to be created by this factory
     * @param debuggerClass class to be used by this factory
     */
    public static void setDebuggerClass(Class<? extends SmackDebugger> debuggerClass) {
        if (debuggerClass == null) {
            System.clearProperty(DEBUGGER_CLASS_PROPERTY_NAME);
        } else {
            System.setProperty(DEBUGGER_CLASS_PROPERTY_NAME, debuggerClass.getCanonicalName());
        }
    }

    /**
     * Returns debugger class used by this factory
     * @return debugger class that will be used for instantiation by this factory
     */
    @SuppressWarnings("unchecked")
    public static Class<SmackDebugger> getDebuggerClass() {
        String customDebuggerClassName = getCustomDebuggerClassName();
        if (customDebuggerClassName == null) {
            return getOneOfDefaultDebuggerClasses();
        } else {
            try {
                return (Class<SmackDebugger>) Class.forName(customDebuggerClassName);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Unable to instantiate debugger class " + customDebuggerClassName, e);
            }
        }
        // no suitable debugger class found - give up
        return null;
    }

    @Override
    public SmackDebugger create(XMPPConnection connection, Writer writer, Reader reader) throws IllegalArgumentException {
        Class<SmackDebugger> debuggerClass = getDebuggerClass();
        if (debuggerClass != null) {
            // Create a new debugger instance using 3arg constructor
            try {
                Constructor<SmackDebugger> constructor = debuggerClass
                        .getConstructor(XMPPConnection.class, Writer.class, Reader.class);
                return constructor.newInstance(connection, writer, reader);
            } catch (Exception e) {
                throw new IllegalArgumentException("Can't initialize the configured debugger!", e);
            }
        }
        return null;
    }

    private static String getCustomDebuggerClassName() {
        try {
            // Use try block since we may not have permission to get a system
            // property (for example, when an applet).
            return System.getProperty(DEBUGGER_CLASS_PROPERTY_NAME);
        } catch (Throwable t) {
            // Ignore.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<SmackDebugger> getOneOfDefaultDebuggerClasses() {
        for (String debugger : DEFAULT_DEBUGGERS) {
            if (SmackConfiguration.isDisabledSmackClass(debugger)) {
                continue;
            }
            try {
                return (Class<SmackDebugger>) Class.forName(debugger);
            } catch (ClassNotFoundException cnfe) {
                LOGGER.fine("Did not find debugger class '" + debugger + "'");
            } catch (ClassCastException ex) {
                LOGGER.warning("Found debugger class that does not appears to implement SmackDebugger interface");
            } catch (Exception ex) {
                LOGGER.warning("Unable to instantiate either Smack debugger class");
            }
        }
        // did not found any of default debuggers - give up
        return null;
    }
}
