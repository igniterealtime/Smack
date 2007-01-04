package org.jivesoftware.smackx.jingle.nat;

/**
 * Transport resolver Interface 
 */
public abstract interface TransportResolverListener {
    /**
     * Resolver listener.
     */
    public interface Resolver extends TransportResolverListener {
        /**
         * The resolution process has been started.
         */
        public void init();

        /**
         * A transport candidate has been added
         *
         * @param cand The transport candidate.
         */
        public void candidateAdded(TransportCandidate cand);

        /**
         * All the transport candidates have been obtained.
         */
        public void end();
    }

    /**
     * Resolver checker.
     */
    public interface Checker extends TransportResolverListener {
        /**
         * A transport candidate has been checked.
         *
         * @param cand The transport candidate that has been checked.
         * @param result True if the candidate is usable.
         */
        public void candidateChecked(TransportCandidate cand, boolean result);

        /**
         * A transport candidate is being checked.
         *
         * @param cand The transport candidate that is being checked.
         */
        public void candidateChecking(TransportCandidate cand);
    }
}
