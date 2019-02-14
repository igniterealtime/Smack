/**
 *
 * Copyright 2018 Florian Schmaus
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
package org.igniterealtime.smack.smackrepl;

import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;

import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateDescriptorGraph;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;
import org.jivesoftware.smack.tcp.XmppNioTcpConnection;

public class StateGraph {

    @SuppressWarnings("DefaultCharset")
    public static void main(String[] args) throws InstantiationException, IllegalAccessException,
            IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        GraphVertex<StateDescriptor> stateGraph = StateDescriptorGraph.constructStateDescriptorGraph(XmppNioTcpConnection.getBackwardEdgesStateDescriptors());

        PrintWriter pw = new PrintWriter(System.out);

        boolean breakStateName = args.length == 0;

        StateDescriptorGraph.stateDescriptorGraphToDot(Collections.singleton(stateGraph), pw, breakStateName);

        pw.flush();
    }

}
