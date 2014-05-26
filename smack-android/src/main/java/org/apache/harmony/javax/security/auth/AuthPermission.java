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

import java.security.BasicPermission;



/**
 * Governs the use of methods in this package and also its subpackages. A
 * <i>target name</i> of the permission specifies which methods are allowed
 * without specifying the concrete action lists. Possible target names and
 * associated authentication permissions are:
 *
 * <pre>
 *    doAs                      invoke Subject.doAs methods.
 *    doAsPrivileged            invoke the Subject.doAsPrivileged methods.
 *    getSubject                invoke Subject.getSubject().
 *    getSubjectFromDomainCombiner    invoke SubjectDomainCombiner.getSubject().
 *    setReadOnly               invoke Subject.setReadonly().
 *    modifyPrincipals          modify the set of principals
 *                              associated with a Subject.
 *    modifyPublicCredentials   modify the set of public credentials
 *                              associated with a Subject.
 *    modifyPrivateCredentials  modify the set of private credentials
 *                              associated with a Subject.
 *    refreshCredential         invoke the refresh method on a credential of a
 *                              refreshable credential class.
 *    destroyCredential         invoke the destroy method on a credential of a
 *                              destroyable credential class.
 *    createLoginContext.<i>name</i>   instantiate a LoginContext with the
 *                              specified name. The wildcard name ('*')
 *                              allows to a LoginContext of any name.
 *    getLoginConfiguration     invoke the getConfiguration method of
 *                              javax.security.auth.login.Configuration.
 *    refreshLoginConfiguration Invoke the refresh method of
 *                              javax.security.auth.login.Configuration.
 * </pre>
 */
public final class AuthPermission extends BasicPermission {

    private static final long serialVersionUID = 5806031445061587174L;

    private static final String CREATE_LOGIN_CONTEXT = "createLoginContext"; //$NON-NLS-1$

    private static final String CREATE_LOGIN_CONTEXT_ANY = "createLoginContext.*"; //$NON-NLS-1$

    // inits permission name.
    private static String init(String name) {

        if (name == null) {
            throw new NullPointerException("auth.13"); //$NON-NLS-1$
        }

        if (CREATE_LOGIN_CONTEXT.equals(name)) {
            return CREATE_LOGIN_CONTEXT_ANY;
        }
        return name;
    }

    /**
     * Creates an authentication permission with the specified target name.
     *
     * @param name
     *            the target name of this authentication permission.
     */
    public AuthPermission(String name) {
        super(init(name));
    }

    /**
     * Creates an authentication permission with the specified target name.
     *
     * @param name
     *            the target name of this authentication permission.
     * @param actions
     *            this parameter is ignored and should be {@code null}.
     */
    public AuthPermission(String name, String actions) {
        super(init(name), actions);
    }
}