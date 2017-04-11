/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.jingleold.nat;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingleold.JingleSession;

/**
 * A TransportResolver is used for obtaining a list of valid transport
 * candidates. A transport candidate is composed by an IP address and a port number.
 * It is called candidate, because it can be elected or not.
 *
 * @author Thiago Camargo
 * @author Alvaro Saurin <alvaro.saurin@gmail.com>
 */
public abstract class TransportResolver {

    private static final Logger LOGGER = Logger.getLogger(TransportResolver.class.getName());

    public enum Type {

        rawupd, ice
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Type type = Type.rawupd;

    // the time, in milliseconds, before a check aborts
    public static final int CHECK_TIMEOUT = 3000;

    // Listeners for events
    private final ArrayList<TransportResolverListener> listeners = new ArrayList<TransportResolverListener>();

    // TRue if the resolver is working
    private boolean resolving;

    // This will be true when all the transport candidates have been gathered...
    private boolean resolved;

    // This indicates that a transport resolver is initialized
    private boolean initialized = false;

    // We store a list of candidates internally, just in case there are several
    // possibilities. When the user asks for a transport, we return the best
    // one.
    protected final List<TransportCandidate> candidates = new ArrayList<TransportCandidate>();

    /**
     * Default constructor.
     */
    protected TransportResolver() {
        super();

        resolving = false;
        resolved = false;
    }

    /**
     * Initialize the Resolver.
     * @throws InterruptedException 
     */
    public abstract void initialize() throws XMPPException, SmackException, InterruptedException;

    /**
     * Start a the resolution.
     * @throws InterruptedException 
     */
    public abstract void resolve(JingleSession session) throws XMPPException, SmackException, InterruptedException;

    /**
     * Clear the list of candidates and start a new resolution process.
     *
     * @throws XMPPException
     */
    public void clear() throws XMPPException {
        cancel();
        candidates.clear();
    }

    /**
     * Cancel any asynchronous resolution operation.
     */
    public abstract void cancel() throws XMPPException;

    /**
     * Return true if the resolver is working.
     *
     * @return true if the resolver is working.
     */
    public boolean isResolving() {
        return resolving;
    }

    /**
     * Return true if the resolver has finished the search for transport
     * candidates.
     *
     * @return true if the search has finished
     */
    public boolean isResolved() {
        return resolved;
    }

    /**
     * Set the Transport Resolver as initialized.
     */
    public synchronized void setInitialized() {
        initialized = true;
    }

    /**
     * Chack if the Transport Resolver is initialized.
     *
     * @return true if initialized
     */
    public synchronized boolean isInitialized() {
        return initialized;
    }

    /**
     * Indicate the beggining of the resolution process. This method must be
     * used by subclasses at the begining of their resolve() method.
     */
    protected synchronized void setResolveInit() {
        resolved = false;
        resolving = true;

        triggerResolveInit();
    }

    /**
     * Indicate the end of the resolution process. This method must be used by
     * subclasses at the begining of their resolve() method.
     */
    protected synchronized void setResolveEnd() {
        resolved = true;
        resolving = false;

        triggerResolveEnd();
    }

    // Listeners management

    /**
     * Add a transport resolver listener.
     *
     * @param li The transport resolver listener to be added.
     */
    public void addListener(TransportResolverListener li) {
        synchronized (listeners) {
            listeners.add(li);
        }
    }

    /**
     * Removes a transport resolver listener.
     *
     * @param li The transport resolver listener to be removed
     */
    public void removeListener(TransportResolverListener li) {
        synchronized (listeners) {
            listeners.remove(li);
        }
    }

    /**
     * Get the list of listeners.
     *
     * @return the list of listeners
     */
    public ArrayList<TransportResolverListener> getListenersList() {
        synchronized (listeners) {
            return new ArrayList<TransportResolverListener>(listeners);
        }
    }

    /**
     * Trigger a new candidate added event.
     *
     * @param cand The candidate added to the list of candidates.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    protected void triggerCandidateAdded(TransportCandidate cand) throws NotConnectedException, InterruptedException {
        Iterator<TransportResolverListener> iter = getListenersList().iterator();
        while (iter.hasNext()) {
            TransportResolverListener trl = iter.next();
            if (trl instanceof TransportResolverListener.Resolver) {
                TransportResolverListener.Resolver li = (TransportResolverListener.Resolver) trl;
                LOGGER.fine("triggerCandidateAdded : " + cand.getLocalIp());
                li.candidateAdded(cand);
            }
        }
    }

    /**
     * Trigger a event notifying the initialization of the resolution process.
     */
    private void triggerResolveInit() {
        Iterator<TransportResolverListener> iter = getListenersList().iterator();
        while (iter.hasNext()) {
            TransportResolverListener trl = iter.next();
            if (trl instanceof TransportResolverListener.Resolver) {
                TransportResolverListener.Resolver li = (TransportResolverListener.Resolver) trl;
                li.init();
            }
        }
    }

    /**
     * Trigger a event notifying the obtention of all the candidates.
     */
    private void triggerResolveEnd() {
        Iterator<TransportResolverListener> iter = getListenersList().iterator();
        while (iter.hasNext()) {
            TransportResolverListener trl = iter.next();
            if (trl instanceof TransportResolverListener.Resolver) {
                TransportResolverListener.Resolver li = (TransportResolverListener.Resolver) trl;
                li.end();
            }
        }
    }

    // Candidates management

    /**
     * Clear the list of candidate
     */
    protected void clearCandidates() {
        synchronized (candidates) {
            candidates.clear();
        }
    }

    /**
     * Add a new transport candidate
     *
     * @param cand The candidate to add
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    protected void addCandidate(TransportCandidate cand) throws NotConnectedException, InterruptedException {
        synchronized (candidates) {
            if (!candidates.contains(cand))
                candidates.add(cand);
        }

        // Notify the listeners
        triggerCandidateAdded(cand);
    }

    /**
     * Get an iterator for the list of candidates.
     *
     * @return an iterator
     */
    public Iterator<TransportCandidate> getCandidates() {
        synchronized (candidates) {
            return Collections.unmodifiableList(new ArrayList<TransportCandidate>(candidates)).iterator();
        }
    }

    /**
     * Get the candididate with the highest preference.
     *
     * @return The best candidate, according to the preference order.
     */
    public TransportCandidate getPreferredCandidate() {
        TransportCandidate result = null;

        ArrayList<ICECandidate> cands = new ArrayList<ICECandidate>();
        for (TransportCandidate tpcan : getCandidatesList()) {
            if (tpcan instanceof ICECandidate)
                cands.add((ICECandidate) tpcan);
        }

        // (ArrayList<ICECandidate>) getCandidatesList();
        if (cands.size() > 0) {
            Collections.sort(cands);
            // Return the last candidate
            result = cands.get(cands.size() - 1);
            LOGGER.fine("Result: " + result.getIp());
        }

        return result;
    }

    /**
     * Get the numer of transport candidates.
     *
     * @return The length of the transport candidates list.
     */
    public int getCandidateCount() {
        synchronized (candidates) {
            return candidates.size();
        }
    }

    /**
     * Get the list of candidates.
     *
     * @return the list of transport candidates
     */
    public List<TransportCandidate> getCandidatesList() {
        List<TransportCandidate> result = null;

        synchronized (candidates) {
            result = new ArrayList<TransportCandidate>(candidates);
        }

        return result;
    }

    /**
     * Get the n-th candidate.
     *
     * @return a transport candidate
     */
    public TransportCandidate getCandidate(int i) {
        TransportCandidate cand;

        synchronized (candidates) {
            cand = candidates.get(i);
        }
        return cand;
    }

    /**
     * Initialize Transport Resolver and wait until it is complete unitialized.
     * @throws SmackException 
     * @throws InterruptedException 
     */
    public void initializeAndWait() throws XMPPException, SmackException, InterruptedException {
        this.initialize();
        try {
            LOGGER.fine("Initializing transport resolver...");
            while (!this.isInitialized()) {
                LOGGER.fine("Resolver init still pending");
                Thread.sleep(1000);
            }
            LOGGER.fine("Transport resolved");
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
    }

    /**
     * Obtain a free port we can use.
     *
     * @return A free port number.
     */
    protected int getFreePort() {
        ServerSocket ss;
        int freePort = 0;

        for (int i = 0; i < 10; i++) {
            freePort = (int) (10000 + Math.round(Math.random() * 10000));
            freePort = freePort % 2 == 0 ? freePort : freePort + 1;
            try {
                ss = new ServerSocket(freePort);
                freePort = ss.getLocalPort();
                ss.close();
                return freePort;
            }
            catch (IOException e) {
                LOGGER.log(Level.WARNING, "exception", e);
            }
        }
        try {
            ss = new ServerSocket(0);
            freePort = ss.getLocalPort();
            ss.close();
        }
        catch (IOException e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
        return freePort;
    }
}
