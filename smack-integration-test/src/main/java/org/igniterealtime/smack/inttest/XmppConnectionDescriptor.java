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
package org.igniterealtime.smack.inttest;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.Consumer;

public final class XmppConnectionDescriptor<
    C extends AbstractXMPPConnection,
    CC extends ConnectionConfiguration,
    CCB extends ConnectionConfiguration.Builder<?, CC>
> {

    private final Class<C> connectionClass;
    private final Class<CC> connectionConfigurationClass;

    private final Constructor<C> connectionConstructor;
    private final Method builderMethod;

    private final Consumer<CCB> extraBuilder;

    private final String nickname;

    private XmppConnectionDescriptor(Builder<C, CC, CCB> builder) throws NoSuchMethodException, SecurityException {
        connectionClass = builder.connectionClass;
        connectionConfigurationClass = builder.connectionConfigurationClass;
        extraBuilder = builder.extraBuilder;
        nickname = builder.nickname;

        connectionConstructor = getConstructor(connectionClass, connectionConfigurationClass);
        builderMethod = getBuilderMethod(connectionConfigurationClass);
    }

    public C construct(Configuration sinttestConfiguration)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return construct(sinttestConfiguration, Collections.emptyList());
    }

    public C construct(Configuration sinttestConfiguration,
                    ConnectionConfigurationBuilderApplier... customConnectionConfigurationAppliers)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException {
        List<ConnectionConfigurationBuilderApplier> customConnectionConfigurationAppliersList = new ArrayList<ConnectionConfigurationBuilderApplier>(
                        Arrays.asList(customConnectionConfigurationAppliers));
        return construct(sinttestConfiguration, customConnectionConfigurationAppliersList);
    }

    public C construct(Configuration sinttestConfiguration,
            Collection<ConnectionConfigurationBuilderApplier> customConnectionConfigurationAppliers)
            throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        CCB connectionConfigurationBuilder = getNewBuilder();
        if (extraBuilder != null) {
            extraBuilder.accept(connectionConfigurationBuilder);
        }
        for (ConnectionConfigurationBuilderApplier customConnectionConfigurationApplier : customConnectionConfigurationAppliers) {
            customConnectionConfigurationApplier.applyConfigurationTo(connectionConfigurationBuilder);
        }
        sinttestConfiguration.configurationApplier.applyConfigurationTo(connectionConfigurationBuilder);
        ConnectionConfiguration connectionConfiguration = connectionConfigurationBuilder.build();
        CC concreteConnectionConfiguration = connectionConfigurationClass.cast(connectionConfiguration);

        C connection = connectionConstructor.newInstance(concreteConnectionConfiguration);

        connection.setReplyTimeout(sinttestConfiguration.replyTimeout);

        return connection;
    }

    @SuppressWarnings("unchecked")
    public CCB getNewBuilder() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        return (CCB) builderMethod.invoke(null);
    }

    public Class<C> getConnectionClass() {
        return connectionClass;
    }

    public String getNickname() {
        return nickname;
    }

    private static <C extends XMPPConnection> Constructor<C> getConstructor(Class<C> connectionClass,
            Class<? extends ConnectionConfiguration> connectionConfigurationClass)
            throws NoSuchMethodException, SecurityException {
        return connectionClass.getConstructor(connectionConfigurationClass);
    }

    private static <CC extends ConnectionConfiguration> Method getBuilderMethod(Class<CC> connectionConfigurationClass)
            throws NoSuchMethodException, SecurityException {
        Method builderMethod = connectionConfigurationClass.getMethod("builder");
        if (!Modifier.isStatic(builderMethod.getModifiers())) {
            throw new IllegalArgumentException();
        }
        Class<?> returnType = builderMethod.getReturnType();
        if (!ConnectionConfiguration.Builder.class.isAssignableFrom(returnType)) {
            throw new IllegalArgumentException();
        }
        return builderMethod;
    }

    public static <C extends AbstractXMPPConnection, CC extends ConnectionConfiguration, CCB extends ConnectionConfiguration.Builder<?, CC>>
            Builder<C, CC, CCB> buildWith(Class<C> connectionClass, Class<CC> connectionConfigurationClass) {
        return buildWith(connectionClass, connectionConfigurationClass, null);
    }

    public static <C extends AbstractXMPPConnection, CC extends ConnectionConfiguration, CCB extends ConnectionConfiguration.Builder<?, CC>>
            Builder<C, CC, CCB> buildWith(Class<C> connectionClass, Class<CC> connectionConfigurationClass, Class<CCB> connectionConfigurationBuilderClass) {
        return new Builder<>(connectionClass, connectionConfigurationClass, connectionConfigurationBuilderClass);
    }

    public static final class Builder<C extends AbstractXMPPConnection, CC extends ConnectionConfiguration, CCB extends ConnectionConfiguration.Builder<?, CC>> {
        private final Class<C> connectionClass;
        private final Class<CC> connectionConfigurationClass;

        private Consumer<CCB> extraBuilder;

        private String nickname;

        // The connectionConfigurationBuilderClass merely exists for type-checking purposes.
        @SuppressWarnings("UnusedVariable")
        private Builder(Class<C> connectionClass, Class<CC> connectionConfigurationClass,
                        Class<CCB> connectionConfigurationBuilderClass) {
            this.connectionClass = connectionClass;
            this.connectionConfigurationClass = connectionConfigurationClass;

            nickname = connectionClass.getSimpleName();
        }

        public Builder<C, CC, CCB> applyExtraConfguration(Consumer<CCB> extraBuilder) {
            this.extraBuilder = extraBuilder;
            return this;
        }

        public Builder<C, CC, CCB> withNickname(String nickname) {
            this.nickname = nickname;
            return this;
        }

        public XmppConnectionDescriptor<C, CC, CCB> build() throws NoSuchMethodException, SecurityException {
            return new XmppConnectionDescriptor<>(this);
        }
    }
}
