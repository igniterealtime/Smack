/**
 *
 * Copyright 2018-2020 Florian Schmaus
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
package org.jivesoftware.smack.fsm;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;

public abstract class StateDescriptor {

    public enum Property {
        multiVisitState,
        finalState,
        notImplemented,
    }

    private final String stateName;
    private final int xepNum;
    private final String rfcSection;
    private final Set<Property> properties;

    private final Class<? extends State> stateClass;
    private final Constructor<? extends State> stateClassConstructor;

    private final Set<Class<? extends StateDescriptor>> successors = new HashSet<>();

    private final Set<Class<? extends StateDescriptor>> predecessors = new HashSet<>();

    private final Set<Class<? extends StateDescriptor>> precedenceOver = new HashSet<>();

    private final Set<Class<? extends StateDescriptor>> inferiorTo = new HashSet<>();

    protected StateDescriptor() {
        this(NoOpState.class, (Property) null);
    }

    protected StateDescriptor(Property... properties) {
        this(NoOpState.class, properties);
    }

    protected StateDescriptor(Class<? extends State> stateClass) {
        this(stateClass, -1, null, Collections.emptySet());
    }

    protected StateDescriptor(Class<? extends State> stateClass, Property... properties) {
        this(stateClass, -1, null, new HashSet<>(Arrays.asList(properties)));
    }

    protected StateDescriptor(Class<? extends State> stateClass, int xepNum) {
        this(stateClass, xepNum, null, Collections.emptySet());
    }

    protected StateDescriptor(Class<? extends State> stateClass, int xepNum,
                    Property... properties) {
        this(stateClass, xepNum, null, new HashSet<>(Arrays.asList(properties)));
    }

    protected StateDescriptor(Class<? extends State> stateClass, String rfcSection) {
        this(stateClass, -1, rfcSection, Collections.emptySet());
    }

    @SuppressWarnings("unchecked")
    private StateDescriptor(Class<? extends State> stateClass, int xepNum,
                    String rfcSection, Set<Property> properties) {
        this.stateClass = stateClass;
        if (rfcSection != null && xepNum > 0) {
            throw new IllegalArgumentException("Must specify either RFC or XEP");
        }
        this.xepNum = xepNum;
        this.rfcSection = rfcSection;
        this.properties = properties;

        Constructor<? extends State> selectedConstructor = null;
        Constructor<?>[] constructors = stateClass.getDeclaredConstructors();
        for (Constructor<?> constructor : constructors) {
            Class<?>[] parameterTypes = constructor.getParameterTypes();
            if (parameterTypes.length != 3) {
                continue;
            }
            if (!ModularXmppClientToServerConnection.class.isAssignableFrom(parameterTypes[0])) {
                continue;
            }
            if (!StateDescriptor.class.isAssignableFrom(parameterTypes[1])) {
                continue;
            }
            if (!ModularXmppClientToServerConnectionInternal.class.isAssignableFrom(parameterTypes[2])) {
                continue;
            }
            selectedConstructor = (Constructor<? extends State>) constructor;
            break;
        }

        stateClassConstructor = selectedConstructor;
        if (stateClassConstructor != null) {
            stateClassConstructor.setAccessible(true);
        } else {
            // TODO: Add validation check that if stateClassConstructor is 'null' the cosntructState() method is overriden.
        }

        String className = getClass().getSimpleName();
        stateName = className.replaceFirst("StateDescriptor", "");
    }

    protected void addSuccessor(Class<? extends StateDescriptor> successor) {
        addAndCheckNonExistent(successors, successor);
    }

    public void addPredeccessor(Class<? extends StateDescriptor> predeccessor) {
        addAndCheckNonExistent(predecessors, predeccessor);
    }

    protected void declarePrecedenceOver(Class<? extends StateDescriptor> subordinate) {
        addAndCheckNonExistent(precedenceOver, subordinate);
    }

    protected void declareInferiorityTo(Class<? extends StateDescriptor> superior) {
        addAndCheckNonExistent(inferiorTo, superior);
    }

    private static <E> void addAndCheckNonExistent(Set<E> set, E e) {
        boolean newElement = set.add(e);
        if (!newElement) {
            throw new IllegalArgumentException("Element already exists in set");
        }
    }

    public Set<Class<? extends StateDescriptor>> getSuccessors() {
        return Collections.unmodifiableSet(successors);
    }

    public Set<Class<? extends StateDescriptor>> getPredeccessors() {
        return Collections.unmodifiableSet(predecessors);
    }

    public Set<Class<? extends StateDescriptor>> getSubordinates() {
        return Collections.unmodifiableSet(precedenceOver);
    }

    public Set<Class<? extends StateDescriptor>> getSuperiors() {
        return Collections.unmodifiableSet(inferiorTo);
    }

    public String getStateName() {
        return stateName;
    }

    public String getFullStateName(boolean breakStateName) {
        String reference = getReference();
        if (reference != null) {
            char sep;
            if (breakStateName) {
                sep = '\n';
            } else {
                sep = ' ';
            }
            return getStateName() + sep + '(' + reference + ')';
        }
        else {
            return getStateName();
        }
    }

    private transient String referenceCache;

    public String getReference()  {
        if (referenceCache == null) {
            if (xepNum > 0) {
                referenceCache = "XEP-" + String.format("%04d", xepNum);
            } else if (rfcSection != null) {
                referenceCache = rfcSection;
            }
        }
        return referenceCache;
    }

    public Class<? extends State> getStateClass() {
        return stateClass;
    }

    public boolean isMultiVisitState() {
        return properties.contains(Property.multiVisitState);
    }

    public boolean isNotImplemented() {
        return properties.contains(Property.notImplemented);
    }

    public boolean isFinalState() {
        return properties.contains(Property.finalState);
    }

    protected State constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
        ModularXmppClientToServerConnection connection = connectionInternal.connection;
        try {
            // If stateClassConstructor is null here, then you probably forgot to override the
            // StateDescriptor.constructState() method?
            return stateClassConstructor.newInstance(connection, this, connectionInternal);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                        | InvocationTargetException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String toString() {
        return "StateDescriptor " + stateName;
    }
}
