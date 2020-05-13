/**
 *
 * Copyright 2016-2017 Fernando Ramirez
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
package org.jivesoftware.smackx.mam;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.junit.jupiter.api.BeforeAll;

public class MamTest extends SmackTestSuite {

    protected static XMPPConnection connection;
    protected static String queryId;
    protected static MamManager mamManager;

    @BeforeAll
    public static void setup() {
        // mock connection
        connection = new DummyConnection();

        // test query id
        queryId = "testid";

        // MamManager instance
        mamManager = MamManager.getInstanceFor(connection);
    }

    protected DataForm getNewMamForm() throws NoSuchMethodException, SecurityException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException {
        Method methodGetNewMamForm = MamManager.class.getDeclaredMethod("getNewMamForm");
        methodGetNewMamForm.setAccessible(true);
        DataForm.Builder dataFormBuilder = (DataForm.Builder) methodGetNewMamForm.invoke(mamManager);
        return dataFormBuilder.build();
    }

}
