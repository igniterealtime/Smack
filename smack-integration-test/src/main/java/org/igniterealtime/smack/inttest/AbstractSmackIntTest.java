/*
 *
 * Copyright 2015-2024 Florian Schmaus
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

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import javax.net.ssl.HttpsURLConnection;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaFilter;

import org.igniterealtime.smack.inttest.util.MultiResultSyncPoint;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;

import org.opentest4j.AssertionFailedError;

public abstract class AbstractSmackIntTest {

    protected static final Logger LOGGER = Logger.getLogger(AbstractSmackIntTest.class.getName());

    protected static final Random INSECURE_RANDOM = new Random();

    protected final String testRunId;

    protected final long timeout;

    protected final Configuration sinttestConfiguration;

    protected AbstractSmackIntTest(SmackIntegrationTestEnvironment environment) {
        this.testRunId = environment.testRunId;
        this.sinttestConfiguration = environment.configuration;
        this.timeout = environment.configuration.replyTimeout;
    }

    protected void performActionAndWaitUntilStanzaReceived(Runnable action, XMPPConnection connection, StanzaFilter filter)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        StanzaCollector.Configuration configuration = StanzaCollector.newConfiguration().setStanzaFilter(
                        filter).setSize(1);
        try (StanzaCollector collector = connection.createStanzaCollector(configuration)) {
            action.run();
            collector.nextResultOrThrow(timeout);
        }
    }

    protected void waitUntilTrue(Condition condition) throws TimeoutException, NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final long deadline = System.currentTimeMillis() + timeout;
        do {
            if (condition.evaluate()) {
                return;
            }
            Thread.sleep(15);
        } while (System.currentTimeMillis() <= deadline);
        throw new TimeoutException("Timeout waiting for condition to become true. Timeout was " + timeout + " ms.");
    }

    protected interface Condition {
        boolean evaluate() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException;
    }

    protected File createNewTempFile() throws IOException {
        File file = File.createTempFile("smack-integration-test-" + testRunId + "-temp-file", null);
        file.deleteOnExit();
        return file;
    }

    protected HttpURLConnection getHttpUrlConnectionFor(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        if (sinttestConfiguration.sslContextFactory != null && urlConnection instanceof HttpsURLConnection) {
            HttpsURLConnection httpsUrlConnection = (HttpsURLConnection) urlConnection;
            httpsUrlConnection.setSSLSocketFactory(sinttestConfiguration.sslContextFactory.createSslContext().getSocketFactory());
        }
        return urlConnection;
    }

    public <R> R assertResult(ResultSyncPoint<R, ?> syncPoint, String message) throws InterruptedException, TimeoutException, AssertionFailedError {
        return assertResult(syncPoint, timeout, message);
    }

    public static <R> R assertResult(ResultSyncPoint<R, ?> syncPoint, long timeout, String message) throws InterruptedException, TimeoutException, AssertionFailedError {
        try {
            return syncPoint.waitForResult(timeout, message);
        } catch (InterruptedException | TimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionFailedError(message, e);
        }
    }

    public <R> List<R> assertResult(MultiResultSyncPoint<R, ?> syncPoint, String message) throws InterruptedException, TimeoutException, AssertionFailedError {
        return assertResult(syncPoint, timeout, message);
    }

    public static <R> List<R> assertResult(MultiResultSyncPoint<R, ?> syncPoint, long timeout, String message) throws InterruptedException, TimeoutException, AssertionFailedError {
        try {
            return syncPoint.waitForResults(timeout, message);
        } catch (InterruptedException | TimeoutException e) {
            throw e;
        } catch (Exception e) {
            throw new AssertionFailedError(message, e);
        }
    }
}
