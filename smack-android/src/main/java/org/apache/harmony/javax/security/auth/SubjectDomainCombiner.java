/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.javax.security.auth;

import java.security.DomainCombiner;
import java.security.Principal;
import java.security.ProtectionDomain;
import java.util.Set;

/**
 * Merges permissions based on code source and code signers with permissions
 * granted to the specified {@link Subject}.
 */
public class SubjectDomainCombiner implements DomainCombiner {

    // subject to be associated
    private Subject subject;

    // permission required to get a subject object
    private static final AuthPermission _GET = new AuthPermission(
            "getSubjectFromDomainCombiner"); //$NON-NLS-1$

    /**
     * Creates a domain combiner for the entity provided in {@code subject}.
     *
     * @param subject
     *            the entity to which this domain combiner is associated.
     */
    public SubjectDomainCombiner(Subject subject) {
        super();
        if (subject == null) {
            throw new NullPointerException();
        }
        this.subject = subject;
    }

    /**
     * Returns the entity to which this domain combiner is associated.
     *
     * @return the entity to which this domain combiner is associated.
     */
    public Subject getSubject() {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            sm.checkPermission(_GET);
        }

        return subject;
    }

    /**
     * Merges the {@code ProtectionDomain} with the {@code Principal}s
     * associated with the subject of this {@code SubjectDomainCombiner}.
     *
     * @param currentDomains
     *            the {@code ProtectionDomain}s associated with the context of
     *            the current thread. The domains must be sorted according to
     *            the execution order, the most recent residing at the
     *            beginning.
     * @param assignedDomains
     *            the {@code ProtectionDomain}s from the parent thread based on
     *            code source and signers.
     * @return a single {@code ProtectionDomain} array computed from the two
     *         provided arrays, or {@code null}.
     * @see ProtectionDomain
     */
    public ProtectionDomain[] combine(ProtectionDomain[] currentDomains,
            ProtectionDomain[] assignedDomains) {
        // get array length for combining protection domains
        int len = 0;
        if (currentDomains != null) {
            len += currentDomains.length;
        }
        if (assignedDomains != null) {
            len += assignedDomains.length;
        }
        if (len == 0) {
            return null;
        }

        ProtectionDomain[] pd = new ProtectionDomain[len];

        // for each current domain substitute set of principal with subject's
        int cur = 0;
        if (currentDomains != null) {

            Set<Principal> s = subject.getPrincipals();
            Principal[] p = s.toArray(new Principal[s.size()]);

            for (cur = 0; cur < currentDomains.length; cur++) {
                if (currentDomains[cur] != null) {
                    ProtectionDomain newPD;
                    newPD = new ProtectionDomain(currentDomains[cur].getCodeSource(),
                            currentDomains[cur].getPermissions(), currentDomains[cur]
                                    .getClassLoader(), p);
                    pd[cur] = newPD;
                }
            }
        }

        // copy assigned domains
        if (assignedDomains != null) {
            System.arraycopy(assignedDomains, 0, pd, cur, assignedDomains.length);
        }

        return pd;
    }
}
