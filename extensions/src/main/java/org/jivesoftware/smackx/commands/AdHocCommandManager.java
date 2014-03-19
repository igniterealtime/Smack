/**
 *
 * Copyright 2005-2008 Jive Software.
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

package org.jivesoftware.smackx.commands;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.commands.AdHocCommand.Action;
import org.jivesoftware.smackx.commands.AdHocCommand.Status;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.NodeInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.xdata.Form;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * An AdHocCommandManager is responsible for keeping the list of available
 * commands offered by a service and for processing commands requests.
 *
 * Pass in a XMPPConnection instance to
 * {@link #getAddHocCommandsManager(org.jivesoftware.smack.XMPPConnection)} in order to
 * get an instance of this class. 
 * 
 * @author Gabriel Guardincerri
 */
public class AdHocCommandManager extends Manager {
    public static final String NAMESPACE = "http://jabber.org/protocol/commands";

    /**
     * The session time out in seconds.
     */
    private static final int SESSION_TIMEOUT = 2 * 60;

    /**
     * Map a XMPPConnection with it AdHocCommandManager. This map have a key-value
     * pair for every active connection.
     */
    private static Map<XMPPConnection, AdHocCommandManager> instances =
            Collections.synchronizedMap(new WeakHashMap<XMPPConnection, AdHocCommandManager>());

    /**
     * Register the listener for all the connection creations. When a new
     * connection is created a new AdHocCommandManager is also created and
     * related to that connection.
     */
    static {
        XMPPConnection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(XMPPConnection connection) {
                getAddHocCommandsManager(connection);
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
    public static synchronized AdHocCommandManager getAddHocCommandsManager(XMPPConnection connection) {
        AdHocCommandManager ahcm = instances.get(connection);
        if (ahcm == null) ahcm = new AdHocCommandManager(connection);
        return ahcm;
    }

    /**
     * Map a command node with its AdHocCommandInfo. Note: Key=command node,
     * Value=command. Command node matches the node attribute sent by command
     * requesters.
     */
    private final Map<String, AdHocCommandInfo> commands = new ConcurrentHashMap<String, AdHocCommandInfo>();

    /**
     * Map a command session ID with the instance LocalCommand. The LocalCommand
     * is the an objects that has all the information of the current state of
     * the command execution. Note: Key=session ID, Value=LocalCommand. Session
     * ID matches the sessionid attribute sent by command responders.
     */
    private final Map<String, LocalCommand> executingCommands = new ConcurrentHashMap<String, LocalCommand>();
    
    private final ServiceDiscoveryManager serviceDiscoveryManager;
    
    /**
     * Thread that reaps stale sessions.
     */
    private Thread sessionsSweeper;

    private AdHocCommandManager(XMPPConnection connection) {
        super(connection);
        this.serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);
        
        // Register the new instance and associate it with the connection
        instances.put(connection, this);

        // Add the feature to the service discovery manage to show that this
        // connection supports the AdHoc-Commands protocol.
        // This information will be used when another client tries to
        // discover whether this client supports AdHoc-Commands or not.
        ServiceDiscoveryManager.getInstanceFor(connection).addFeature(
                NAMESPACE);

        // Set the NodeInformationProvider that will provide information about
        // which AdHoc-Commands are registered, whenever a disco request is
        // received
        ServiceDiscoveryManager.getInstanceFor(connection)
                .setNodeInformationProvider(NAMESPACE,
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
                try {
                    processAdHocCommand(requestData);
                }
                catch (SmackException e) {
                    return;
                }
            }
        };

        PacketFilter filter = new PacketTypeFilter(AdHocCommandData.class);
        connection.addPacketListener(listener, filter);

        sessionsSweeper = null;
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
        AdHocCommandInfo commandInfo = new AdHocCommandInfo(node, name, connection().getUser(), factory);

        commands.put(node, commandInfo);
        // Set the NodeInformationProvider that will provide information about
        // the added command
        serviceDiscoveryManager.setNodeInformationProvider(node,
                new NodeInformationProvider() {
                    public List<DiscoverItems.Item> getNodeItems() {
                        return null;
                    }

                    public List<String> getNodeFeatures() {
                        List<String> answer = new ArrayList<String>();
                        answer.add(NAMESPACE);
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
     * @throws SmackException if there was no response from the server.
     */
    public DiscoverItems discoverCommands(String jid) throws XMPPException, SmackException {
        return serviceDiscoveryManager.discoverItems(jid, NAMESPACE);
    }

    /**
     * Publish the commands to an specific JID.
     *
     * @param jid the full JID to publish the commands to.
     * @throws XMPPException if the operation failed for some reason.
     * @throws SmackException if there was no response from the server.
     */
    public void publishCommands(String jid) throws XMPPException, SmackException {
        // Collects the commands to publish as items
        DiscoverItems discoverItems = new DiscoverItems();
        Collection<AdHocCommandInfo> xCommandsList = getRegisteredCommands();

        for (AdHocCommandInfo info : xCommandsList) {
            DiscoverItems.Item item = new DiscoverItems.Item(info.getOwnerJID());
            item.setName(info.getName());
            item.setNode(info.getNode());
            discoverItems.addItem(item);
        }

        serviceDiscoveryManager.publishItems(jid, NAMESPACE, discoverItems);
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
        return new RemoteCommand(connection(), node, jid);
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
     * @throws SmackException if there was no response from the server.
     */
    private void processAdHocCommand(AdHocCommandData requestData) throws SmackException {
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
                    if (sessionsSweeper == null) {
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
                        sessionsSweeper.start();
                    }
                }

                // Sends the response packet
                connection().sendPacket(response);

            }
            catch (XMPPErrorException e) {
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
                    // TODO: Check that all the required fields of the form are
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

                    connection().sendPacket(response);
                }
                catch (XMPPErrorException e) {
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
                }
            }
        }
    }

    /**
     * Responds an error with an specific condition.
     * 
     * @param response the response to send.
     * @param condition the condition of the error.
     * @throws NotConnectedException 
     */
    private void respondError(AdHocCommandData response,
            XMPPError.Condition condition) throws NotConnectedException {
        respondError(response, new XMPPError(condition));
    }

    /**
     * Responds an error with an specific condition.
     * 
     * @param response the response to send.
     * @param condition the condition of the error.
     * @param specificCondition the adhoc command error condition.
     * @throws NotConnectedException 
     */
    private void respondError(AdHocCommandData response, XMPPError.Condition condition,
            AdHocCommand.SpecificErrorCondition specificCondition) throws NotConnectedException
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
     * @throws NotConnectedException 
     */
    private void respondError(AdHocCommandData response, XMPPError error) throws NotConnectedException {
        response.setType(IQ.Type.ERROR);
        response.setError(error);
        connection().sendPacket(response);
    }

    /**
     * Creates a new instance of a command to be used by a new execution request
     * 
     * @param commandNode the command node that identifies it.
     * @param sessionID the session id of this execution.
     * @return the command instance to execute.
     * @throws XMPPErrorException if there is problem creating the new instance.
     */
    private LocalCommand newInstanceOfCmd(String commandNode, String sessionID) throws XMPPErrorException
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
            throw new XMPPErrorException(new XMPPError(
                    XMPPError.Condition.internal_server_error));
        }
        catch (IllegalAccessException e) {
            throw new XMPPErrorException(new XMPPError(
                    XMPPError.Condition.internal_server_error));
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
