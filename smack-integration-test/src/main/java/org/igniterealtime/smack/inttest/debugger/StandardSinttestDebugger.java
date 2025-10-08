/*
 *
 * Copyright 2024-2025 Florian Schmaus
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
package org.igniterealtime.smack.inttest.debugger;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.debugger.SimpleAbstractDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.util.ExceptionUtil;

import org.igniterealtime.smack.inttest.AbstractSmackIntTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.ConcreteTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework.TestRunResult;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint.ResultSyncPointTimeoutException;

public class StandardSinttestDebugger implements SinttestDebugger {

    protected static final Logger LOGGER = Logger.getLogger(StandardSinttestDebugger.class.getName());

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final Object currentWriterLock = new Object();
    private Writer currentWriter;

    private Path currentTestMethodDirectory;

    private final Path basePath;
    private final Writer completeWriter;
    private final Writer outsideTestWriter;
    private final Writer testsWriter;
    private final boolean console;

    public StandardSinttestDebugger(ZonedDateTime restRunStart, String testRunId, String options) {
        String tmpdir = System.getProperty("java.io.tmpdir");
        if ("/tmp".equals(tmpdir)) {
            // We don't want to fill up the memory.
            tmpdir = "/var/tmp";
        }
        String basePath = tmpdir
                        + File.separator
                        + "sinttest-" + System.getProperty("user.name")
                        + File.separator
                        + DATE_TIME_FORMATTER.format(restRunStart) + "-" + testRunId
                        ;
        boolean console = true;

        if (options != null) {
            for (String keyValue : options.split(",")) {
                String[] keyValueArray = keyValue.split("=");
                if (keyValueArray.length != 2) {
                    throw new IllegalArgumentException("Illegal key/value string: " + keyValue);
                }

                String key = keyValueArray[0];
                String value = keyValueArray[1];
                switch (key) {
                case "console":
                    switch (value) {
                    case "on":
                        console = true;
                        break;
                    case "off":
                        console = false;
                        break;
                    default:
                        throw new IllegalArgumentException(
                                        "Invalid argument console=" + value + ", only off/on are allowed");
                    }
                    break;
                case "dir":
                    switch (value) {
                    case "off":
                        basePath = null;
                        break;
                    default:
                        basePath = value;
                        break;
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unknown key: " + key);
                }
            }
        }

        if (basePath != null) {
            this.basePath = Path.of(basePath);
            Path completeLogFile = this.basePath.resolve("completeLog");
            Path outsideTestLogFile = this.basePath.resolve("outsideTestLog");
            Path testsFile = this.basePath.resolve("tests");
            try {
                mkdirs(this.basePath);

                completeWriter = Files.newBufferedWriter(completeLogFile);
                outsideTestWriter = currentWriter = Files.newBufferedWriter(outsideTestLogFile);
                testsWriter = Files.newBufferedWriter(testsFile);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        } else {
            this.basePath = null;
            completeWriter = null;
            outsideTestWriter = null;
            testsWriter = null;
        }
        this.console = console;
    }

    private class StandardSinttestSmackDebugger extends SimpleAbstractDebugger {
        StandardSinttestSmackDebugger(XMPPConnection connection) {
            super(connection);
        }

        @Override
        protected void logSink(String logMessage) {
            StandardSinttestDebugger.this.logSink(logMessage);
        }
    }

    private void logSink(String logMessage) {
        if (basePath != null) {
            try {
                synchronized (currentWriterLock) {
                    currentWriter.append(logMessage).append('\n');
                }

                completeWriter.append(logMessage).append('\n');
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, e + " while appending log message", e);
            }
        }

        if (console) {
            // CHECKSTYLE:OFF
            System.out.println(logMessage);
            // CHECKSTYLE:ON
        }
    }

    @Override
    public SmackDebuggerFactory getSmackDebuggerFactory() {
        return c -> new StandardSinttestSmackDebugger(c);
    }

    @Override
    public void onTestClassConstruction(Constructor<? extends AbstractSmackIntTest> constructor) throws IOException {
        if (basePath == null) {
            return;
        }

        Path testClassDirectory = basePath.resolve(constructor.getDeclaringClass().getSimpleName());
        mkdirs(testClassDirectory);

        Path logFile = testClassDirectory.resolve("log");
        Writer newWriter = Files.newBufferedWriter(logFile);
        synchronized (currentWriterLock) {
            currentWriter = newWriter;
        }

        completeWriter.append("Constructing: " + constructor.getDeclaringClass() + "\n");
    };

    @Override
    public void onTestStart(ConcreteTest test, ZonedDateTime startTime) throws IOException {
        if (basePath == null) {
            return;
        }

        Method testMethod = test.getMethod();

        Path testClassDirectory = basePath.resolve(testMethod.getDeclaringClass().getSimpleName());

        StringBuilder testName = new StringBuilder(testMethod.getName());
        for (String subdescription : test.getSubdescriptons()) {
            testName.append('-').append(subdescription);
        }
        currentTestMethodDirectory = testClassDirectory.resolve(testName.toString());

        mkdirs(currentTestMethodDirectory);

        Path logFile = currentTestMethodDirectory.resolve("log");
        Writer newWriter = Files.newBufferedWriter(logFile);

        synchronized (currentWriterLock) {
            currentWriter = newWriter;
        }

        completeWriter.append("START: " + test + "\n");

        testsWriter.append(test.toString());
    }

    private void onTestEnd(Throwable throwable) throws IOException {
        if (basePath == null) {
            return;
        }

        Writer oldWriter;
        synchronized (currentWriterLock) {
            oldWriter = currentWriter;
            currentWriter = outsideTestWriter;
        }
        if (oldWriter != null) {
            oldWriter.close();
        }

        if (throwable == null) {
            testsWriter.append(" ✓");
        } else {
            testsWriter.append(" ✗ [FAILED: ").append(throwable.getClass().getSimpleName()).append(']');
        }
        testsWriter.append('\n');
    }

    private Path createTestMarkerFile(String name) throws IOException {
        if (currentTestMethodDirectory == null) {
            return null;
        }

        Path failedMarker = currentTestMethodDirectory.resolve(name);
        return Files.createFile(failedMarker);
    }

    @Override
    public void onTestSuccess(ConcreteTest test, ZonedDateTime endTime) throws IOException {
        logSink("TEST SUCCESSFUL: " + test);

        createTestMarkerFile("successful");

        onTestEnd(null);
    }

    @Override
    public void onTestFailure(ConcreteTest test, ZonedDateTime endTime, Throwable throwable) throws IOException {
        String stacktrace = ExceptionUtil.getStackTrace(throwable);

        logSink("TEST FAILED: " + test + "\n" + stacktrace);

        Path markerFile = createTestMarkerFile("failed");
        if (markerFile != null) {
            Files.writeString(markerFile, stacktrace);
        }
        if (currentTestMethodDirectory != null) {
            if (throwable instanceof ResultSyncPointTimeoutException) {
                var resultSyncPointTimeoutException = (ResultSyncPointTimeoutException) throwable;
                var threadDump = resultSyncPointTimeoutException.getThreadDump();
                var threadDumpFile = currentTestMethodDirectory.resolve("thread-dump");
                Files.writeString(threadDumpFile, threadDump);

                logSink("Wrote thread dump to file://" + threadDumpFile);
            }
        }

        onTestEnd(throwable);
    }

    @Override
    public void onSinttestFinished(TestRunResult testRunResult) throws IOException {
        if (basePath == null) {
            return;
        }

        outsideTestWriter.close();
        completeWriter.close();
        testsWriter.close();

        var failedTestsFile = this.basePath.resolve("tests-failed");
        try (var failedTestsWriter = Files.newBufferedWriter(failedTestsFile)) {
            for (var failed : testRunResult.getFailedTests()) {
                failedTestsWriter.append(failed.concreteTest.toString()).append(" ✗ [FAILED: ").append(failed.failureReason.getClass().getSimpleName()).append("]\n");
            }
        }

        LOGGER.info("Test data file://" + basePath);
    }

    private static void mkdirs(Path path) throws IOException {
        var dir = path.toFile();
        if (dir.exists())
            return;

        boolean created = dir.mkdirs();
        if (!created)
            throw new IOException("Could not create directory " + path);
    }
}
