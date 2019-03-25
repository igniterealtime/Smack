/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.util;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.lang.ref.PhantomReference;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.Manager;

import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Utility class to test for memory leaks caused by Smack.
 * <p>
 * Note that this test is based on the assumption that it is possible to trigger a full garbage collection run, which is
 * not the case. See also this
 * <a href="https://stackoverflow.com/questions/1481178/how-to-force-garbage-collection-in-java">stackoverflow
 * question</a>. Hence the {@link #triggerGarbageCollection()} method defined in this class is not portable and depends
 * on implementation depended Java Virtual Machine behavior.
 * </p>
 *
 * @see <a href="https://issues.igniterealtime.org/browse/SMACK-383">SMACK-383 Jira Issue</a>
 */
public class MemoryLeakTestUtil {

    private static final Logger LOGGER = Logger.getLogger(MemoryLeakTestUtil.class.getName());

    public static <M extends Manager> void noResourceLeakTest(Function<DummyConnection, M> managerSupplier)
            throws XmppStringprepException, IllegalArgumentException, InterruptedException {
        final int numConnections = 10;

        ReferenceQueue<DummyConnection> connectionsReferenceQueue = new ReferenceQueue<>();
        ReferenceQueue<Manager> managerReferenceQueue = new ReferenceQueue<>();

        // Those two sets ensure that we hold a strong reference to the created PhantomReferences until the end of the
        // test.
        @SuppressWarnings("ModifiedButNotUsed")
        Set<PhantomReference<DummyConnection>> connectionsPhantomReferences = new HashSet<>();
        @SuppressWarnings("ModifiedButNotUsed")
        Set<PhantomReference<Manager>> managersPhantomReferences = new HashSet<>();

        List<DummyConnection> connections = new ArrayList<>(numConnections);
        for (int i = 0; i < numConnections; i++) {
            DummyConnection connection = new DummyConnection("foo" + i, "bar", "baz");

            PhantomReference<DummyConnection> connectionPhantomReference = new PhantomReference<>(connection, connectionsReferenceQueue);
            connectionsPhantomReferences.add(connectionPhantomReference);

            Manager manager = managerSupplier.apply(connection);
            PhantomReference<Manager> managerPhantomReference = new PhantomReference<Manager>(manager, managerReferenceQueue);
            managersPhantomReferences.add(managerPhantomReference);

            connections.add(connection);
        }

        // Clear the only references to the created connections.
        connections = null;

        triggerGarbageCollection();

        // Now the connections should have been gc'ed, but not managers not yet.
        assertReferencesQueueSize(connectionsReferenceQueue, numConnections);
        assertReferencesQueueIsEmpty(managerReferenceQueue);

        // We new create another connection and explicitly a new Manager. This will trigger the cleanup mechanism in the
        // WeakHashMaps used by the Manager's iNSTANCE field. This should clean up all references to the Managers.
        DummyConnection connection = new DummyConnection("last", "bar", "baz");
        @SuppressWarnings("unused")
        Manager manager = managerSupplier.apply(connection);

        // The previous Managers should now be reclaimable by the garbage collector. First trigger a GC run.
        triggerGarbageCollection();

        // Now the Managers should have been freed and this means we should see their phantom references in the
        // reference queue.
        assertReferencesQueueSize(managerReferenceQueue, numConnections);
    }

    private static void assertReferencesQueueSize(ReferenceQueue<?> referenceQueue, int expectedSize) throws IllegalArgumentException, InterruptedException {
        final int timeout = 60000;
        for (int itemsRemoved = 0; itemsRemoved < expectedSize; ++itemsRemoved) {
            Reference<?> reference = referenceQueue.remove(timeout);
            assertNotNull("No reference found after " + timeout + "ms", reference);
            reference.clear();
        }

        Reference<?> reference = referenceQueue.poll();
        assertNull("Reference queue is not empty when it should be", reference);
    }

    private static void assertReferencesQueueIsEmpty(ReferenceQueue<?> referenceQueue) {
        Reference<?> reference = referenceQueue.poll();
        assertNull(reference);
    }

    private static void triggerGarbageCollection() {
        Object object = new Object();
        WeakReference<Object> weakReference = new WeakReference<>(object);
        object = null;

        int gcCalls = 0;
        do {
            if (gcCalls > 1000) {
                throw new AssertionError("No observed gargabe collection after " + gcCalls + " calls of System.gc()");
            }
            System.gc();
            gcCalls++;
        } while (weakReference.get() != null);

        // Note that this is no guarantee that a *full* garbage collection run has been made, which is what we actually
        // need here in order to prevent false negatives.
        LOGGER.finer("Observed garbage collection after " + gcCalls + " calls of System.gc()");
    }
}
