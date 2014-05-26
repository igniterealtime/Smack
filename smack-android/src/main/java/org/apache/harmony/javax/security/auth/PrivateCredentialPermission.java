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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.security.Permission;
import java.security.PermissionCollection;
import java.security.Principal;
import java.util.Set;



/**
 * Protects private credential objects belonging to a {@code Subject}. It has
 * only one action which is "read". The target name of this permission has a
 * special syntax:
 *
 * <pre>
 * targetName = CredentialClass {PrincipalClass &quot;PrincipalName&quot;}*
 * </pre>
 *
 * First it states a credential class and is followed then by a list of one or
 * more principals identifying the subject.
 * <p>
 * The principals on their part are specified as the name of the {@code
 * Principal} class followed by the principal name in quotes. For example, the
 * following file may define permission to read the private credentials of a
 * principal named "Bob": "com.sun.PrivateCredential com.sun.Principal \"Bob\""
 * <p>
 * The syntax also allows the use of the wildcard "*" in place of {@code
 * CredentialClass} or {@code PrincipalClass} and/or {@code PrincipalName}.
 *
 * @see Principal
 */
public final class PrivateCredentialPermission extends Permission {

    private static final long serialVersionUID = 5284372143517237068L;

    // allowed action
    private static final String READ = "read"; //$NON-NLS-1$

    private String credentialClass;

    // current offset        
    private transient int offset;

    // owners set
    private transient CredOwner[] set;
    
    /**
     * Creates a new permission for private credentials specified by the target
     * name {@code name} and an {@code action}. The action is always
     * {@code "read"}.
     *
     * @param name
     *            the target name of the permission.
     * @param action
     *            the action {@code "read"}.
     */
    public PrivateCredentialPermission(String name, String action) {
        super(name);
        if (READ.equalsIgnoreCase(action)) {
            initTargetName(name);
        } else {
            throw new IllegalArgumentException("auth.11"); //$NON-NLS-1$
        }
    }

    /**
     * Creates a {@code PrivateCredentialPermission} from the {@code Credential}
     * class and set of principals.
     * 
     * @param credentialClass
     *            the credential class name.
     * @param principals
     *            the set of principals.
     */
    PrivateCredentialPermission(String credentialClass, Set<Principal> principals) {
        super(credentialClass);
        this.credentialClass = credentialClass;

        set = new CredOwner[principals.size()];
        for (Principal p : principals) {
            CredOwner element = new CredOwner(p.getClass().getName(), p.getName());
            // check for duplicate elements
            boolean found = false;
            for (int ii = 0; ii < offset; ii++) {
                if (set[ii].equals(element)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                set[offset++] = element;
            }
        }
    }

    /**
     * Initialize a PrivateCredentialPermission object and checks that a target
     * name has a correct format: CredentialClass 1*(PrincipalClass
     * "PrincipalName")
     */
    private void initTargetName(String name) {

        if (name == null) {
            throw new NullPointerException("auth.0E"); //$NON-NLS-1$
        }

        // check empty string
        name = name.trim();
        if (name.length() == 0) {
            throw new IllegalArgumentException("auth.0F"); //$NON-NLS-1$
        }

        // get CredentialClass
        int beg = name.indexOf(' ');
        if (beg == -1) {
            throw new IllegalArgumentException("auth.10"); //$NON-NLS-1$
        }
        credentialClass = name.substring(0, beg);

        // get a number of pairs: PrincipalClass "PrincipalName"
        beg++;
        int count = 0;
        int nameLength = name.length();
        for (int i, j = 0; beg < nameLength; beg = j + 2, count++) {
            i = name.indexOf(' ', beg);
            j = name.indexOf('"', i + 2);

            if (i == -1 || j == -1 || name.charAt(i + 1) != '"') {
                throw new IllegalArgumentException("auth.10"); //$NON-NLS-1$
            }
        }

        // name MUST have one pair at least
        if (count < 1) {
            throw new IllegalArgumentException("auth.10"); //$NON-NLS-1$
        }

        beg = name.indexOf(' ');
        beg++;

        // populate principal set with instances of CredOwner class
        String principalClass;
        String principalName;

        set = new CredOwner[count];
        for (int index = 0, i, j; index < count; beg = j + 2, index++) {
            i = name.indexOf(' ', beg);
            j = name.indexOf('"', i + 2);

            principalClass = name.substring(beg, i);
            principalName = name.substring(i + 2, j);

            CredOwner element = new CredOwner(principalClass, principalName);
            // check for duplicate elements
            boolean found = false;
            for (int ii = 0; ii < offset; ii++) {
                if (set[ii].equals(element)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                set[offset++] = element;
            }
        }
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
        initTargetName(getName());
    }

    /**
     * Returns the principal's classes and names associated with this {@code
     * PrivateCredentialPermission} as a two dimensional array. The first
     * dimension of the array corresponds to the number of principals. The
     * second dimension defines either the name of the {@code PrincipalClass}
     * [x][0] or the value of {@code PrincipalName} [x][1].
     * <p>
     * This corresponds to the the target name's syntax:
     *
     * <pre>
     * targetName = CredentialClass {PrincipalClass &quot;PrincipalName&quot;}*
     * </pre>
     *
     * @return the principal classes and names associated with this {@code
     *         PrivateCredentialPermission}.
     */
    public String[][] getPrincipals() {

        String[][] s = new String[offset][2];

        for (int i = 0; i < s.length; i++) {
            s[i][0] = set[i].principalClass;
            s[i][1] = set[i].principalName;
        }
        return s;
    }

    @Override
    public String getActions() {
        return READ;
    }

    /**
     * Returns the class name of the credential associated with this permission.
     *
     * @return the class name of the credential associated with this permission.
     */
    public String getCredentialClass() {
        return credentialClass;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        for (int i = 0; i < offset; i++) {
            hash = hash + set[i].hashCode();
        }
        return getCredentialClass().hashCode() + hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj == null || this.getClass() != obj.getClass()) {
            return false;
        }

        PrivateCredentialPermission that = (PrivateCredentialPermission) obj;

        return credentialClass.equals(that.credentialClass) && (offset == that.offset)
                && sameMembers(set, that.set, offset);
    }

    @Override
    public boolean implies(Permission permission) {

        if (permission == null || this.getClass() != permission.getClass()) {
            return false;
        }

        PrivateCredentialPermission that = (PrivateCredentialPermission) permission;

        if (!("*".equals(credentialClass) || credentialClass //$NON-NLS-1$
                .equals(that.getCredentialClass()))) {
            return false;
        }

        if (that.offset == 0) {
            return true;
        }

        CredOwner[] thisCo = set;
        CredOwner[] thatCo = that.set;
        int thisPrincipalsSize = offset;
        int thatPrincipalsSize = that.offset;
        for (int i = 0, j; i < thisPrincipalsSize; i++) {
            for (j = 0; j < thatPrincipalsSize; j++) {
                if (thisCo[i].implies(thatCo[j])) {
                    break;
                }
            }
            if (j == thatCo.length) {
                return false;
            }
        }
        return true;
    }

    @Override
    public PermissionCollection newPermissionCollection() {
        return null;
    }

    /**
     * Returns true if the two arrays have the same length, and every member of
     * one array is contained in another array
     */
    private boolean sameMembers(Object[] ar1, Object[] ar2, int length) {
        if (ar1 == null && ar2 == null) {
            return true;
        }
        if (ar1 == null || ar2 == null) {
            return false;
        }
        boolean found;
        for (int i = 0; i < length; i++) {
            found = false;
            for (int j = 0; j < length; j++) {
                if (ar1[i].equals(ar2[j])) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    private static final class CredOwner implements Serializable {

        private static final long serialVersionUID = -5607449830436408266L;

        String principalClass;

        String principalName;

        // whether class name contains wildcards
        private transient boolean isClassWildcard;

        // whether pname contains wildcards
        private transient boolean isPNameWildcard;

        // Creates a new CredOwner with the specified Principal Class and Principal Name 
        CredOwner(String principalClass, String principalName) {
            super();
            if ("*".equals(principalClass)) { //$NON-NLS-1$
                isClassWildcard = true;
            }

            if ("*".equals(principalName)) { //$NON-NLS-1$
                isPNameWildcard = true;
            }

            if (isClassWildcard && !isPNameWildcard) {
                throw new IllegalArgumentException("auth.12"); //$NON-NLS-1$
            }

            this.principalClass = principalClass;
            this.principalName = principalName;
        }

        // Checks if this CredOwner implies the specified Object. 
        boolean implies(Object obj) {
            if (obj == this) {
                return true;
            }

            CredOwner co = (CredOwner) obj;

            if (isClassWildcard || principalClass.equals(co.principalClass)) {
                if (isPNameWildcard || principalName.equals(co.principalName)) {
                    return true;
                }
            }
            return false;
        }

        // Checks two CredOwner objects for equality. 
        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }
            if (obj instanceof CredOwner) {
                CredOwner that = (CredOwner) obj;
                return principalClass.equals(that.principalClass)
                    && principalName.equals(that.principalName);
            }
            return false;
        }

        // Returns the hash code value for this object.
        @Override
        public int hashCode() {
            return principalClass.hashCode() + principalName.hashCode();
        }
    }
}
