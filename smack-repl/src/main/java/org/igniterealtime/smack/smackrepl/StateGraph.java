/**
 *
 * Copyright 2018 Florian Schmaus
 *
 * This file is part of smack-repl.
 *
 * smack-repl is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
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
