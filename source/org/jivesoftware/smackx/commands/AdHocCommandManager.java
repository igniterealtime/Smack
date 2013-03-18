/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2005-2008 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.commands;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.commands.AdHocCommand.Action;
import org.jivesoftware.smackx.commands.AdHocCommand.Status;
import org.jivesoftware.smackx.packet.AdHocCommandData;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An AdHocCommandManager is responsible for keeping the list of available
 * commands offered by a service and for processing commands requests.
 *
 * Pass in a Connection instance to
 * {@link #getAddHocCommandsManager(org.jivesoftware.smack.Connection)} in order to
 * get an instance of this class. 
 * 
 * @author Gabriel Guardincerri
 */
public class AdHocCommandManager {

    private static final String DISCO_NAMESPACE = "http://jabber.org/protocol/commands";

    private static final String discoNode = DISCO_NAMESPACE;

    /**
     * The session time out in seconds.
     */
    private static final int SESSION_TIMEOUT = 2 * 60;

    /**
     * Map a Connection with it AdHocCommandManager. This map have a key-value
     * pair for every active connection.
     */
    private static Map<Connection, AdHocCommandManager> instances =
            new ConcurrentHashMap<Connection, AdHocCommandManager>();

    /**
     * Register the listener for all the connection creations. When a new
     * connection is created a new AdHocCommandManager is also created and
     * related to that connection.
     */
    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new AdHocCommandManager(connection);
            }
        });
    }

    /**
     * Returns the <code>AdHocCommandManager</code> related to the
     * <code>connection</code>.
     *
     * @param connection the XMPP connection.
     * @return the AdHocCommandManager associated with the connection.
     */
    public static AdHocCommandManager getAddHocCommandsManager(Connection connection) {
        return instances.get(connection);
    }

    /**
     * Thread that reaps stale sessions.
     */
    private Thread sessionsSweeper;

    /**
     * The Connection that this instances of AdHocCommandManager manages
     */
    private Connection connection;

    /**
     * Map a command node with its AdHocCommandInfo. Note: Key=command node,
     * Value=command. Command node matches the node attribute sent by command
     * requesters.
     */
    private Map<String, AdHocCommandInfo> commands = new ConcurrentHashMap<String, AdHocCommandInfo>();

    /**
     * Map a command session ID with the instance LocalCommand. The LocalCommand
     * is the an objects that has all the information of the current state of
     * the command execution. Note: Key=session ID, Value=LocalCommand. Session
     * ID matches the sessionid attribute sent by command responders.
     */
    private Map<String, LocalCommand> executingCommands = new ConcurrentHashMap<String, LocalCommand>();

    private AdHocCommandManager(Connection connection) {
        super();
        this.connection = connection;
        init();
    }

    /**
     * Registers a new command with this command manager, which is related to a
     * connection. The <tt>node</tt> is an unique identifier of that command for
     * the connection related to this command manager. The <tt>name</tt> is the
     * human readable name of the command. The <tt>class</tt> is the class of
     * the command, which must extend {@link LocalCommand} and have a default
     * constructor.
     *
     * @param node the unique identifier of the command.
     * @param name the human readable name of the command.
     * @param clazz the class of the command, which must extend {@link LocalCommand}.
     */
    public void registerCommand(String node, String name, final Class<? extends LocalCommand> clazz) {
        registerCommand(node, name, new LocalCommandFactory() {
            public LocalCommand getInstance() throws InstantiationException, IllegalAccessException  {
                return clazz.newInstance();
            }
        });
    }

    /**
     * Registers a new command with this command manager, which is related to a
     * connection. The <tt>node</tt> is an unique identifier of that
     * command for the connection related to this command manager. The <tt>name</tt>
     * is the human readeale name of the command. The <tt>factory</tt> generates
     * new instances of the command.
     *
     * @param node the unique identifier of the command.
     * @param name the human readable name of the command.
     * @param factory a factory to create new instances of the command.
     */
    public void registerCommand(String node, final String name, LocalCommandFactory factory) {
        AdHocCommandInfo commandInfo = new AdHocCommandInfo(node, name, connection.getUser(), factory);

        commands.put(node, commandInfo);
        // Set the NodeInformationProvider that will provide information about
        // the added command
        ServiceDiscoveryManager.getInstanceFor(connection).setNodeInformationProvider(node,
                new NodeInformationProvider() {
                    public List<DiscoverItems.Item> getNodeItems() {
                        return null;
                    }

                    public List<String> getNodeFeatures() {
                        List<String> answer = new ArrayList<String>();
                        answer.add(DISCO_NAMESPACE);
                        // TODO: check if this service is provided by the
                        // TODO: current connection.
                        answer.add("jabber:x:data");
                        return answer;
                    }

                    public List<DiscoverInfo.Identity> getNodeIdentities() {
                        List<DiscoverInfo.Identity> answer = new ArrayList<DiscoverInfo.Identity>();
                        DiscoverInfo.Identity identity = new DiscoverInfo.Identity(
                                "automation", name, "command-node");
                        answer.add(identity);
                        return answer;
                    }

                    @Override
                    public List<PacketExtension> getNodePacketExtensions() {
                        return null;
                    }

                });
    }

    /**
     * Discover the commands of an specific JID. The <code>jid</code> is a
     * full JID.
     *
     * @param jid the full JID to retrieve the commands for.
     * @return the discovered items.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverItems discoverCommands(String jid) throws XMPPException {
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager
                .getInstanceFor(connection);
        return serviceDiscoveryManager.discoverItems(jid, discoNode);
    }

    /**
     * Publish the commands to an specific JID.
     *
     * @param jid the full JID to publish the commands to.
     * @throws XMPPException if the operation failed for some reason.
     */
    public void publishCommands(String jid) throws XMPPException {
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager
                .getInstanceFor(connection);

        // Collects the commands to publish as items
        DiscoverItems discoverItems = new DiscoverItems();
        Collection<AdHocCommandInfo> xCommandsList = getRegisteredCommands();

        for (AdHocCommandInfo info : xCommandsList) {
            DiscoverItems.Item item = new DiscoverItems.Item(info.getOwnerJID());
            item.setName(info.getName());
            item.setNode(info.getNode());
            discoverItems.addItem(item);
        }

        serviceDiscoveryManager.publishItems(jid, discoNode, discoverItems);
    }

    /**
     * Returns a command that represents an instance of a command in a remote
     * host. It is used to execute remote commands. The concept is similar to
     * RMI. Every invocation on this command is equivalent to an invocation in
     * the remote command.
     *
     * @param jid the full JID of the host of the remote command
     * @param node the identifier of the command
     * @return a local instance equivalent to the remote command.
     */
    public RemoteCommand getRemoteCommand(String jid, String node) {
        return new RemoteCommand(connection, node, jid);
    }

    /**
     * <ul>
     * <li>Adds listeners to the connection</li>
     * <li>Registers the ad-hoc command feature to the ServiceDiscoveryManager</li>
     * <li>Registers the items of the feature</li>
     * <li>Adds packet listeners to handle execution requests</li>
     * <li>Creates and start the session sweeper</li>
     * </ul>
     */
    private void init() {
        // Register the new instance and associate it with the connection
        instances.put(connection, this);

        // Add a listener to the connection that removes the registered instance
        // when the connection is closed
        connection.addConnectionListener(new ConnectionListener() {
            public void connectionClosed() {
                // Unregister this instance since the connection has been closed
                instances.remove(connection);
            }

            public void connectionClosedOnError(Exception e) {
                // Unregister this instance since the connection has been closed
                instances.remove(connection);
            }

            public void reconnectionSuccessful() {
                // Register this instance since the connection has been
                // reestablished
                instances.put(connection, AdHocCommandManager.this);
            }

            public void reconnectingIn(int seconds) {
                // Nothing to do
            }

            public void reconnectionFailed(Exception e) {
                // Nothing to do
            }
        });

        // Add the feature to the service discovery manage to show that this
        // connection supports the AdHoc-Commands protocol.
        // This information will be used when another client tries to
        // discover whether this client supports AdHoc-Commands or not.
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(
                DISCO_NAMESPACE);

        // Set the NodeInformationProvider that will provide information about
        // which AdHoc-Commands are registered, whenever a disco request is
        // received
        ServiceDiscoveryManager.getInstanceFor(connection)
                .setNodeInformationProvider(discoNode,
                        new NodeInformationProvider() {
                            public List<DiscoverItems.Item> getNodeItems() {

                                List<DiscoverItems.Item> answer = new ArrayList<DiscoverItems.Item>();
                                Collection<AdHocCommandInfo> commandsList = getRegisteredCommands();

                                for (AdHocCommandInfo info : commandsList) {
                                    DiscoverItems.Item item = new DiscoverItems.Item(
                                            info.getOwnerJID());
                                    item.setName(info.getName());
                                    item.setNode(info.getNode());
                                    answer.add(item);
                                }

                                return answer;
                            }

                            public List<String> getNodeFeatures() {
                                return null;
                            }

                            public List<Identity> getNodeIdentities() {
                                return null;
                            }

                            @Override
                            public List<PacketExtension> getNodePacketExtensions() {
                                return null;
                            }
                        });

        // The packet listener and the filter for processing some AdHoc Commands
        // Packets
        PacketListener listener = new PacketListener() {
            public void processPacket(Packet packet) {
                AdHocCommandData requestData = (AdHocCommandData) packet;
                processAdHocCommand(requestData);
            }
        };

        PacketFilter filter = new PacketTypeFilter(AdHocCommandData.class);
        connection.addPacketListener(listener, filter);

        // Create a thread to reap sessions. But, we'll only start it later when commands are
        // actually registered.
        sessionsSweeper = new Thread(new Runnable() {
            public void run() {
                while (true) {
                    for (String sessionId : executingCommands.keySet()) {
                        LocalCommand command = executingCommands.get(sessionId);
                        // Since the command could be removed in the meanwhile
                        // of getting the key and getting the value - by a
                        // processed packet. We must check if it still in the
                        // map.
                        if (command != null) {
                            long creationStamp = command.getCreationDate();
                            // Check if the Session data has expired (default is
                            // 10 minutes)
                            // To remove it from the session list it waits for
                            // the double of the of time out time. This is to
                            // let
                            // the requester know why his execution request is
                            // not accepted. If the session is removed just
                            // after the time out, then whe the user request to
                            // continue the execution he will recieved an
                            // invalid session error and not a time out error.
                            if (System.currentTimeMillis() - creationStamp > SESSION_TIMEOUT * 1000 * 2) {
                                // Remove the expired session
                                executingCommands.remove(sessionId);
                            }
                        }
                    }
                    try {
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException ie) {
                        // Ignore.
                    }
                }
            }

        });
        sessionsSweeper.setDaemon(true);
    }

    /**
     * Process the AdHoc-Command packet that request the execution of some
     * action of a command. If this is the first request, this method checks,
     * before executing the command, if:
     * <ul>
     *  <li>The requested command exists</li>
     *  <li>The requester has permissions to execute it</li>
     *  <li>The command has more than one stage, if so, it saves the command and
     *      session ID for further use</li>
     * </ul>
     * 
     * <br>
     * <br>
     * If this is not the first request, this method checks, before executing
     * the command, if:
     * <ul>
     *  <li>The session ID of the request was stored</li>
     *  <li>The session life do not exceed the time out</li>
     *  <li>The action to execute is one of the available actions</li>
     * </ul>
     *
     * @param requestData
     *            the packet to process.
     */
    private void processAdHocCommand(AdHocCommandData requestData) {
        // Only process requests of type SET
        if (requestData.getType() != IQ.Type.SET) {
            return;
        }

        // Creates the response with the corresponding data
        AdHocCommandData response = new AdHocCommandData();
        response.setTo(requestData.getFrom());
        response.setPacketID(requestData.getPacketID());
        response.setNode(requestData.getNode());
        response.setId(requestData.getTo());

        String sessionId = requestData.getSessionID();
        String commandNode = requestData.getNode();

        if (sessionId == null) {
            // A new execution request has been received. Check that the
            // command exists
            if (!commands.containsKey(commandNode)) {
                // Requested command does not exist so return
                // item_not_found error.
                respondError(response, XMPPError.Condition.item_not_found);
                return;
            }

            // Create new session ID
            sessionId = StringUtils.randomString(15);

            try {
                // Create a new instance of the command with the
                // corresponding sessioid
                LocalCommand command = newInstanceOfCmd(commandNode, sessionId);

                response.setType(IQ.Type.RESULT);
                command.setData(response);

                // Check that the requester has enough permission.
                // Answer forbidden error if requester permissions are not
                // enough to execute the requested command
                if (!command.hasPermission(requestData.getFrom())) {
                    respondError(response, XMPPError.Condition.forbidden);
                    return;
                }

                Action action = requestData.getAction();

                // If the action is unknown then respond an error.
                if (action != null && action.equals(Action.unknown)) {
                    respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.malformedAction);
                    return;
                }

                // If the action is not execute, then it is an invalid action.
                if (action != null && !action.equals(Action.execute)) {
                    respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.badAction);
                    return;
                }

                // Increase the state number, so the command knows in witch
                // stage it is
                command.incrementStage();
                // Executes the command
                command.execute();

                if (command.isLastStage()) {
                    // If there is only one stage then the command is completed
                    response.setStatus(Status.completed);
                }
                else {
                    // Else it is still executing, and is registered to be
                    // available for the next call
                    response.setStatus(Status.executing);
                    executingCommands.put(sessionId, command);
                    // See if the session reaping thread is started. If not, start it.
                    if (!sessionsSweeper.isAlive()) {
                        sessionsSweeper.start();
                    }
                }

                // Sends the response packet
                connection.sendPacket(response);

            }
            catch (XMPPException e) {
                // If there is an exception caused by the next, complete,
                // prev or cancel method, then that error is returned to the
                // requester.
                XMPPError error = e.getXMPPError();

                // If the error type is cancel, then the execution is
                // canceled therefore the status must show that, and the
                // command be removed from the executing list.
                if (XMPPError.Type.CANCEL.equals(error.getType())) {
                    response.setStatus(Status.canceled);
                    executingCommands.remove(sessionId);
                }
                respondError(response, error);
                e.printStackTrace();
            }
        }
        else {
            LocalCommand command = executingCommands.get(sessionId);

            // Check that a command exists for the specified sessionID
            // This also handles if the command was removed in the meanwhile
            // of getting the key and the value of the map.
            if (command == null) {
                respondError(response, XMPPError.Condition.bad_request,
                        AdHocCommand.SpecificErrorCondition.badSessionid);
                return;
            }

            // Check if the Session data has expired (default is 10 minutes)
            long creationStamp = command.getCreationDate();
            if (System.currentTimeMillis() - creationStamp > SESSION_TIMEOUT * 1000) {
                // Remove the expired session
                executingCommands.remove(sessionId);

                // Answer a not_allowed error (session-expired)
                respondError(response, XMPPError.Condition.not_allowed,
                        AdHocCommand.SpecificErrorCondition.sessionExpired);
                return;
            }

            /*
             * Since the requester could send two requests for the same
             * executing command i.e. the same session id, all the execution of
             * the action must be synchronized to avoid inconsistencies.
             */
            synchronized (command) {
                Action action = requestData.getAction();

                // If the action is unknown the respond an error
                if (action != null && action.equals(Action.unknown)) {
                    respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.malformedAction);
                    return;
                }

                // If the user didn't specify an action or specify the execute
                // action then follow the actual default execute action
                if (action == null || Action.execute.equals(action)) {
                    action = command.getExecuteAction();
                }

                // Check that the specified action was previously
                // offered
                if (!command.isValidAction(action)) {
                    respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.badAction);
                    return;
                }

                try {
                    // TODO: Check that all the requierd fields of the form are
                    // TODO: filled, if not throw an exception. This will simplify the
                    // TODO: construction of new commands

                    // Since all errors were passed, the response is now a
                    // result
                    response.setType(IQ.Type.RESULT);

                    // Set the new data to the command.
                    command.setData(response);

                    if (Action.next.equals(action)) {
                        command.incrementStage();
                        command.next(new Form(requestData.getForm()));
                        if (command.isLastStage()) {
                            // If it is the last stage then the command is
                            // completed
                            response.setStatus(Status.completed);
                        }
                        else {
                            // Otherwise it is still executing
                            response.setStatus(Status.executing);
                        }
                    }
                    else if (Action.complete.equals(action)) {
                        command.incrementStage();
                        command.complete(new Form(requestData.getForm()));
                        response.setStatus(Status.completed);
                        // Remove the completed session
                        executingCommands.remove(sessionId);
                    }
                    else if (Action.prev.equals(action)) {
                        command.decrementStage();
                        command.prev();
                    }
                    else if (Action.cancel.equals(action)) {
                        command.cancel();
                        response.setStatus(Status.canceled);
                        // Remove the canceled session
                        executingCommands.remove(sessionId);
                    }

                    connection.sendPacket(response);
                }
                catch (XMPPException e) {
                    // If there is an exception caused by the next, complete,
                    // prev or cancel method, then that error is returned to the
                    // requester.
                    XMPPError error = e.getXMPPError();

                    // If the error type is cancel, then the execution is
                    // canceled therefore the status must show that, and the
                    // command be removed from the executing list.
                    if (XMPPError.Type.CANCEL.equals(error.getType())) {
                        response.setStatus(Status.canceled);
                        executingCommands.remove(sessionId);
                    }
                    respondError(response, error);

                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Responds an error with an specific condition.
     * 
     * @param response the response to send.
     * @param condition the condition of the error.
     */
    private void respondError(AdHocCommandData response,
            XMPPError.Condition condition) {
        respondError(response, new XMPPError(condition));
    }

    /**
     * Responds an error with an specific condition.
     * 
     * @param response the response to send.
     * @param condition the condition of the error.
     * @param specificCondition the adhoc command error condition.
     */
    private void respondError(AdHocCommandData response, XMPPError.Condition condition,
            AdHocCommand.SpecificErrorCondition specificCondition)
    {
        XMPPError error = new XMPPError(condition);
        error.addExtension(new AdHocCommandData.SpecificError(specificCondition));
        respondError(response, error);
    }

    /**
     * Responds an error with an specific error.
     * 
     * @param response the response to send.
     * @param error the error to send.
     */
    private void respondError(AdHocCommandData response, XMPPError error) {
        response.setType(IQ.Type.ERROR);
        response.setError(error);
        connection.sendPacket(response);
    }

    /**
     * Creates a new instance of a command to be used by a new execution request
     * 
     * @param commandNode the command node that identifies it.
     * @param sessionID the session id of this execution.
     * @return the command instance to execute.
     * @throws XMPPException if there is problem creating the new instance.
     */
    private LocalCommand newInstanceOfCmd(String commandNode, String sessionID)
            throws XMPPException
    {
        AdHocCommandInfo commandInfo = commands.get(commandNode);
        LocalCommand command;
        try {
            command = (LocalCommand) commandInfo.getCommandInstance();
            command.setSessionID(sessionID);
            command.setName(commandInfo.getName());
            command.setNode(commandInfo.getNode());
        }
        catch (InstantiationException e) {
            e.printStackTrace();
            throw new XMPPException(new XMPPError(
                    XMPPError.Condition.interna_server_error));
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            throw new XMPPException(new XMPPError(
                    XMPPError.Condition.interna_server_error));
        }
        return command;
    }

    /**
     * Returns the registered commands of this command manager, which is related
     * to a connection.
     * 
     * @return the registered commands.
     */
    private Collection<AdHocCommandInfo> getRegisteredCommands() {
        return commands.values();
    }

    /**
     * Stores ad-hoc command information.
     */
    private static class AdHocCommandInfo {

        private String node;
        private String name;
        private String ownerJID;
        private LocalCommandFactory factory;

        public AdHocCommandInfo(String node, String name, String ownerJID,
                LocalCommandFactory factory)
        {
            this.node = node;
            this.name = name;
            this.ownerJID = ownerJID;
            this.factory = factory;
        }

        public LocalCommand getCommandInstance() throws InstantiationException,
                IllegalAccessException
        {
            return factory.getInstance();
        }

        public String getName() {
            return name;
        }

        public String getNode() {
            return node;
        }

        public String getOwnerJID() {
            return ownerJID;
        }
    }
}