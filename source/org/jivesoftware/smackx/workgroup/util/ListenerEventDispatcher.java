/**
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

package org.jivesoftware.smackx.workgroup.util;

import java.lang.reflect.Method;
import java.util.*;

/**
 * This class is a very flexible event dispatcher which implements Runnable so that it can
 * dispatch easily from a newly created thread. The usage of this in code is more or less:
 * create a new instance of this class, use addListenerTriplet to add as many listeners
 * as desired to be messaged, create a new Thread using the instance of this class created
 * as the argument to the constructor, start the new Thread instance.<p>
 *
 * Also, this is intended to be used to message methods that either return void, or have
 * a return which the developer using this class is uninterested in receiving.
 *
 * @author loki der quaeler
 */
public class ListenerEventDispatcher
    implements Runnable {

    protected transient ArrayList triplets;

    protected transient boolean hasFinishedDispatching;
    protected transient boolean isRunning;

    public ListenerEventDispatcher () {
        super();

        this.triplets = new ArrayList();

        this.hasFinishedDispatching = false;
        this.isRunning = false;
    }

    /**
     * Add a listener triplet - the instance of the listener to be messaged, the Method on which
     *  the listener should be messaged, and the Object array of arguments to be supplied to the
     *  Method. No attempts are made to determine whether this triplet was already added.<br>
     *
     * Messages are dispatched in the order in which they're added via this method; so if triplet
     *  X is added after triplet Z, then triplet Z will undergo messaging prior to triplet X.<br>
     *
     * This method should not be called once the owning Thread instance has been started; if it
     *  is called, the triplet will not be added to the messaging queue.<br>
     *
     * @param listenerInstance the instance of the listener to receive the associated notification
     * @param listenerMethod the Method instance representing the method through which notification
     *                          will occur
     * @param methodArguments the arguments supplied to the notification method
     */
    public void addListenerTriplet(Object listenerInstance, Method listenerMethod,
            Object[] methodArguments)
    {
        if (!this.isRunning) {
            this.triplets.add(new TripletContainer(listenerInstance, listenerMethod,
                    methodArguments));
        }
    }

    /**
     * @return whether this instance has finished dispatching its messages
     */
    public boolean hasFinished() {
        return this.hasFinishedDispatching;
    }

    public void run() {
        ListIterator li = null;

        this.isRunning = true;

        li = this.triplets.listIterator();
        while (li.hasNext()) {
            TripletContainer tc = (TripletContainer)li.next();

            try {
                tc.getListenerMethod().invoke(tc.getListenerInstance(), tc.getMethodArguments());
            } catch (Exception e) {
                System.err.println("Exception dispatching an event: " + e);

                e.printStackTrace();
            }
        }

        this.hasFinishedDispatching = true;
    }


    protected class TripletContainer {

        protected Object listenerInstance;
        protected Method listenerMethod;
        protected Object[] methodArguments;

        protected TripletContainer (Object inst, Method meth, Object[] args) {
            super();

            this.listenerInstance = inst;
            this.listenerMethod = meth;
            this.methodArguments = args;
        }

        protected Object getListenerInstance() {
            return this.listenerInstance;
        }

        protected Method getListenerMethod() {
            return this.listenerMethod;
        }

        protected Object[] getMethodArguments() {
            return this.methodArguments;
        }
    }
}