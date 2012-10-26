/**
 * $RCSfile$
 * $Revision: 7071 $
 * $Date: 2007-02-11 16:59:05 -0800 (Sun, 11 Feb 2007) $
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.jingle;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.apache.commons.logging.Log;

// --------------------------------------------------------------------------
/**
 *  SmackLogger attempts to use Apache commons-logging if it's available.
 *  
 *  When you request an instance of SmackLogger we make an attempt to create an instance of org.apache.commons.logging.Log.
 *  If we are able to make an instance of Log then we dispatch all log requests to commons-logging.
 *  If we are not able to make an instance of Log then we dispatch all log requests to System.out/err.
 *  
 *  @author jeffw
 */
public class SmackLogger {

	private Log	commonsLogger;

	// --------------------------------------------------------------------------
	/**
	 *  This static method is the only way to get an instance of a SmackLogger.
	 *  @param classToLog	This is the class that wants to log.  (This gives commons-logging a means to control log-level by class.)
	 *  @return	An instance of a SmackLogger for the class that wants logging.
	 */
	public static SmackLogger getLogger(Class<?> classToLog) {
		return new SmackLogger(classToLog);
	}

	// --------------------------------------------------------------------------
	/**
	 *  This is private to make it impossible to instantiate a new SmackLogger outside of the getLogger() static method.
	 *  @param classToLog	This is the class that wants to log.  (This gives commons-logging a means to control log-level by class.)
	 */
	public SmackLogger(Class<?> classToLog) {
		setupSmackLogger(classToLog);
	}

	// --------------------------------------------------------------------------
	/**
	 *  The actual attempt to create an instance of commons-logging Log.
	 *  @param classToLog
	 *  @return
	 */
	private void setupSmackLogger(Class<?> classToLog) {
		try {
			Class<?> logFactoryClass = SmackLogger.class.getClassLoader().loadClass("org.apache.commons.logging.LogFactory");
			Method method = logFactoryClass.getMethod("getLog", Class.class);
			//Constructor<Log> constructor = Log.class.getConstructor(new Class[] { Object.class });
			commonsLogger = (Log) method.invoke(null, classToLog);

		// We don't care to do anything about exceptions.  
		// If we can't create a commons-logger then just use our simple one.
		} catch (ClassNotFoundException e) {
		} catch (SecurityException e) {
		} catch (NoSuchMethodException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		} catch (InvocationTargetException e) {
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging error(Object msg, Exception exception)
	 *  @param inDebugMsg
	 */
	public void error(String inErrorMsg, Exception inException) {
		if (commonsLogger != null) {
			commonsLogger.error(inErrorMsg, inException);
		} else {
			System.err.println(inErrorMsg);
			inException.printStackTrace(System.err);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging error(Object msg)
	 *  @param inDebugMsg
	 */
	public void error(String inErrorMsg) {
		if (commonsLogger != null) {
			commonsLogger.error(inErrorMsg);
		} else {
			System.err.println(inErrorMsg);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging debug(Object msg)
	 *  @param inDebugMsg
	 */
	public void debug(String inDebugMsg, Exception inException) {
		if (commonsLogger != null) {
			commonsLogger.debug(inDebugMsg, inException);
		} else {
			System.out.println(inDebugMsg);
			inException.printStackTrace(System.out);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging debug(Object msg)
	 *  @param inDebugMsg
	 */
	public void debug(String inDebugMsg) {
		if (commonsLogger != null) {
			commonsLogger.debug(inDebugMsg);
		} else {
			System.out.println(inDebugMsg);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging warn(Object msg)
	 *  @param inDebugMsg
	 */
	public void warn(String inDebugMsg, Exception inException) {
		if (commonsLogger != null) {
			commonsLogger.warn(inDebugMsg, inException);
		} else {
			System.out.println(inDebugMsg);
			inException.printStackTrace(System.out);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging warn(Object msg)
	 *  @param inDebugMsg
	 */
	public void warn(String inDebugMsg) {
		if (commonsLogger != null) {
			commonsLogger.warn(inDebugMsg);
		} else {
			System.out.println(inDebugMsg);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging info(Object msg)
	 *  @param inDebugMsg
	 */
	public void info(String inDebugMsg, Exception inException) {
		if (commonsLogger != null) {
			commonsLogger.info(inDebugMsg, inException);
		} else {
			System.out.println(inDebugMsg);
			inException.printStackTrace(System.out);
		}
	}

	// --------------------------------------------------------------------------
	/**
	 *  Wrapper for commons-logging info(Object msg)
	 *  @param inDebugMsg
	 */
	public void info(String inDebugMsg) {
		if (commonsLogger != null) {
			commonsLogger.info(inDebugMsg);
		} else {
			System.out.println(inDebugMsg);
		}
	}

}
