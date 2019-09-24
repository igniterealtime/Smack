/**
 *
 * Copyright 2018-2019 Florian Schmaus
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
package org.jivesoftware.smack;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.util.XmppElementUtil;

public class NonzaCallback {

    protected final AbstractXMPPConnection connection;
    protected final Map<QName, ClassAndConsumer<? extends Nonza>> filterAndListeners;

    private NonzaCallback(Builder builder) {
        this.connection = builder.connection;
        this.filterAndListeners = builder.filterAndListeners;
        install();
    }

    void onNonzaReceived(Nonza nonza) throws IOException {
        QName key = nonza.getQName();
        ClassAndConsumer<? extends Nonza> classAndConsumer = filterAndListeners.get(key);

        classAndConsumer.accept(nonza);
    }

    public void cancel() {
        for (Map.Entry<QName, ClassAndConsumer<? extends Nonza>> entry : filterAndListeners.entrySet()) {
            QName filterKey = entry.getKey();
            synchronized (connection.nonzaCallbacksMap) {
                connection.nonzaCallbacksMap.removeOne(filterKey, this);
            }
        }
    }

    protected void install() {
        if (filterAndListeners.isEmpty()) {
            return;
        }

        for (QName key : filterAndListeners.keySet()) {
            synchronized (connection.nonzaCallbacksMap) {
                connection.nonzaCallbacksMap.put(key, this);
            }
        }
    }

    private static final class NonzaResponseCallback<SN extends Nonza, FN extends Nonza> extends NonzaCallback {

        private SN successNonza;
        private FN failedNonza;

        private NonzaResponseCallback(Class<SN> successNonzaClass, Class<FN> failedNonzaClass,
                        Builder builder) {
            super(builder);

            final QName successNonzaKey = XmppElementUtil.getQNameFor(successNonzaClass);
            final QName failedNonzaKey = XmppElementUtil.getQNameFor(failedNonzaClass);

            final NonzaListener<SN> successListener = new NonzaListener<SN>() {
                @Override
                public void accept(SN successNonza) {
                    NonzaResponseCallback.this.successNonza = successNonza;
                    notifyResponse();
                }
            };
            final ClassAndConsumer<SN> successClassAndConsumer = new ClassAndConsumer<>(successNonzaClass,
                            successListener);

            final NonzaListener<FN> failedListener = new NonzaListener<FN>() {
                @Override
                public void accept(FN failedNonza) {
                    NonzaResponseCallback.this.failedNonza = failedNonza;
                    notifyResponse();
                }
            };
            final ClassAndConsumer<FN> failedClassAndConsumer = new ClassAndConsumer<>(failedNonzaClass,
                            failedListener);

            filterAndListeners.put(successNonzaKey, successClassAndConsumer);
            filterAndListeners.put(failedNonzaKey, failedClassAndConsumer);

            install();
        }

        private void notifyResponse() {
            synchronized (this) {
                notifyAll();
            }
        }

        private boolean hasReceivedSuccessOrFailedNonza() {
            return successNonza != null || failedNonza != null;
        }

        private SN waitForResponse() throws NoResponseException, InterruptedException, FailedNonzaException {
            final long deadline = System.currentTimeMillis() + connection.getReplyTimeout();
            synchronized (this) {
                while (!hasReceivedSuccessOrFailedNonza()) {
                    final long now = System.currentTimeMillis();
                    if (now >= deadline) break;
                    wait(deadline - now);
                }
            }

            if (!hasReceivedSuccessOrFailedNonza()) {
                throw NoResponseException.newWith(connection, "Nonza Listener");
            }

            if (failedNonza != null) {
                throw new XMPPException.FailedNonzaException(failedNonza);
            }

            assert successNonza != null;
            return successNonza;
        }
    }

    public static final class Builder {
        private final AbstractXMPPConnection connection;

        private Map<QName, ClassAndConsumer<? extends Nonza>> filterAndListeners = new HashMap<>();

        Builder(AbstractXMPPConnection connection) {
            this.connection = connection;
        }

        public <N extends Nonza> Builder listenFor(Class<N> nonza, NonzaListener<N> nonzaListener) {
            QName key = XmppElementUtil.getQNameFor(nonza);
            ClassAndConsumer<N> classAndConsumer = new ClassAndConsumer<>(nonza, nonzaListener);
            filterAndListeners.put(key, classAndConsumer);
            return this;
        }

        public NonzaCallback install() {
            return new NonzaCallback(this);
        }
    }

    public interface NonzaListener<N extends Nonza> {
        void accept(N nonza) throws IOException;
    }

    private static final class ClassAndConsumer<N extends Nonza> {
        private final Class<N> clazz;
        private final NonzaListener<N> consumer;

        private ClassAndConsumer(Class<N> clazz, NonzaListener<N> consumer) {
            this.clazz = clazz;
            this.consumer = consumer;
        }

        private void accept(Object object) throws IOException {
            N nonza = clazz.cast(object);
            consumer.accept(nonza);
        }
    }

    static <SN extends Nonza, FN extends Nonza> SN sendAndWaitForResponse(NonzaCallback.Builder builder, Nonza nonza, Class<SN> successNonzaClass,
                    Class<FN> failedNonzaClass)
                    throws NoResponseException, NotConnectedException, InterruptedException, FailedNonzaException {
        NonzaResponseCallback<SN, FN> nonzaCallback = new NonzaResponseCallback<>(successNonzaClass,
                        failedNonzaClass, builder);

        SN successNonza;
        try {
            nonzaCallback.connection.sendNonza(nonza);
            successNonza = nonzaCallback.waitForResponse();
        }
        finally {
            nonzaCallback.cancel();
        }

        return successNonza;
    }
}
