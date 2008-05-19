/**
 * $RCSfile: TransportResolverListener.java,v $
 * $Revision: 1.1 $
 * $Date: 15/11/2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
