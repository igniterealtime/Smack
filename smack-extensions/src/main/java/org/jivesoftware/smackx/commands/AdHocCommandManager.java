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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.commands.AdHocCommand.Action;
import org.jivesoftware.smackx.commands.AdHocCommand.Status;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.disco.AbstractNodeInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.Form;
import org.jxmpp.jid.Jid;

/**
 * An AdHocCommandManager is responsible for keeping the list of available
 * commands offered by a service and for processing commands requests.
 *
 * Pass in an XMPPConnection instance to
 * {@link #getAddHocCommandsManager(XMPPConnection)} in order to
 * get an instance of this class. 
 * 
 * @author Gabriel Guardincerri
 */
public final class AdHocCommandManager extends Manager {
    public static final String NAMESPACE = "http://jabber.org/protocol/commands";

    private static final Logger LOGGER = Logger.getLogger(AdHocCommandManager.class.getName());

    /**
     * The session time out in seconds.
     */
    private static final int SESSION_TIMEOUT = 2 * 60;

    /**
     * Map an XMPPConnection with it AdHocCommandManager. This map have a key-value
     * pair for every active connection.
     */
    private static Map<XMPPConnection, AdHocCommandManager> instances = new WeakHashMap<>();

    /**
     * Register the listener for all the connection creations. When a new
     * connection is created a new AdHocCommandManager is also created and
     * related to that connection.
     */
    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
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
        if (ahcm == null) {
            ahcm = new AdHocCommandManager(connection);
            instances.put(connection, ahcm);
        }
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
    // FIXME The session sweeping is horrible implemented. The thread will never stop running. A different approach must
    // be implemented. For example one that does stop reaping sessions and the thread if there are no more, and restarts
    // the reaping process on demand. Or for every command a scheduled task should be created that removes the session
    // if it's timed out. See SMACK-624.
    private Thread sessionsSweeper;

    private AdHocCommandManager(XMPPConnection connection) {
        super(connection);
        this.serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

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
                        new AbstractNodeInformationProvider() {
                            @Override
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
                        });

        // The packet listener and the filter for processing some AdHoc Commands
        // Packets
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(AdHocCommandData.ELEMENT,
                        AdHocCommandData.NAMESPACE, IQ.Type.set, Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                AdHocCommandData requestData = (AdHocCommandData) iqRequest;
                try {
                    return processAdHocCommand(requestData);
                }
                catch (InterruptedException | NoResponseException | NotConnectedException e) {
                    LOGGER.log(Level.INFO, "processAdHocCommand threw exceptino", e);
                    return null;
                }
            }
        });

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
            @Override
            public LocalCommand getInstance() throws InstantiationException, IllegalAccessException  {
                try {
                    return clazz.getConstructor().newInstance();
                }
                catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException
                                | SecurityException e) {
                    // TODO: Throw those method in Smack 4.3.
                    throw new IllegalStateException(e);
                }
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
                new AbstractNodeInformationProvider() {
                    @Override
                    public List<String> getNodeFeatures() {
                        List<String> answer = new ArrayList<String>();
                        answer.add(NAMESPACE);
                        // TODO: check if this service is provided by the
                        // TODO: current connection.
                        answer.add("jabber:x:data");
                        return answer;
                    }
                    @Override
                    public List<DiscoverInfo.Identity> getNodeIdentities() {
                        List<DiscoverInfo.Identity> answer = new ArrayList<DiscoverInfo.Identity>();
                        DiscoverInfo.Identity identity = new DiscoverInfo.Identity(
                                "automation", name, "command-node");
                        answer.add(identity);
                        return answer;
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
     * @throws InterruptedException 
     */
    public DiscoverItems discoverCommands(Jid jid) throws XMPPException, SmackException, InterruptedException {
        return serviceDiscoveryManager.discoverItems(jid, NAMESPACE);
    }

    /**
     * Publish the commands to an specific JID.
     *
     * @param jid the full JID to publish the commands to.
     * @throws XMPPException if the operation failed for some reason.
     * @throws SmackException if there was no response from the server.
     * @throws InterruptedException 
     */
    public void publishCommands(Jid jid) throws XMPPException, SmackException, InterruptedException {
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
    public RemoteCommand getRemoteCommand(Jid jid, String node) {
        return new RemoteCommand(connection(), node, jid);
    }

    /**
     * Process the AdHoc-Command stanza(/packet) that request the execution of some
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
     *            the stanza(/packet) to process.
     * @throws NotConnectedException
     * @throws NoResponseException
     * @throws InterruptedException 
     */
    private IQ processAdHocCommand(AdHocCommandData requestData) throws NoResponseException, NotConnectedException, InterruptedException {
        // Creates the response with the corresponding data
        AdHocCommandData response = new AdHocCommandData();
        response.setTo(requestData.getFrom());
        response.setStanzaId(requestData.getStanzaId());
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
                return respondError(response, XMPPError.Condition.item_not_found);
            }

            // Create new session ID
            sessionId = StringUtils.randomString(15);

            try {
                // Create a new instance of the command with the
                // corresponding sessioid
                LocalCommand command = newInstanceOfCmd(commandNode, sessionId);

                response.setType(IQ.Type.result);
                command.setData(response);

                // Check that the requester has enough permission.
                // Answer forbidden error if requester permissions are not
                // enough to execute the requested command
                if (!command.hasPermission(requestData.getFrom())) {
                    return respondError(response, XMPPError.Condition.forbidden);
                }

                Action action = requestData.getAction();

                // If the action is unknown then respond an error.
                if (action != null && action.equals(Action.unknown)) {
                    return respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.malformedAction);
                }

                // If the action is not execute, then it is an invalid action.
                if (action != null && !action.equals(Action.execute)) {
                    return respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.badAction);
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
                            @Override
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
                return response;

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
                return respondError(response, XMPPError.getBuilder(error));
            }
        }
        else {
            LocalCommand command = executingCommands.get(sessionId);

            // Check that a command exists for the specified sessionID
            // This also handles if the command was removed in the meanwhile
            // of getting the key and the value of the map.
            if (command == null) {
                return respondError(response, XMPPError.Condition.bad_request,
                        AdHocCommand.SpecificErrorCondition.badSessionid);
            }

            // Check if the Session data has expired (default is 10 minutes)
            long creationStamp = command.getCreationDate();
            if (System.currentTimeMillis() - creationStamp > SESSION_TIMEOUT * 1000) {
                // Remove the expired session
                executingCommands.remove(sessionId);

                // Answer a not_allowed error (session-expired)
                return respondError(response, XMPPError.Condition.not_allowed,
                        AdHocCommand.SpecificErrorCondition.sessionExpired);
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
                    return respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.malformedAction);
                }

                // If the user didn't specify an action or specify the execute
                // action then follow the actual default execute action
                if (action == null || Action.execute.equals(action)) {
                    action = command.getExecuteAction();
                }

                // Check that the specified action was previously
                // offered
                if (!command.isValidAction(action)) {
                    return respondError(response, XMPPError.Condition.bad_request,
                            AdHocCommand.SpecificErrorCondition.badAction);
                }

                try {
                    // TODO: Check that all the required fields of the form are
                    // TODO: filled, if not throw an exception. This will simplify the
                    // TODO: construction of new commands

                    // Since all errors were passed, the response is now a
                    // result
                    response.setType(IQ.Type.result);

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

                    return response;
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
                    return respondError(response, XMPPError.getBuilder(error));
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
    private static IQ respondError(AdHocCommandData response,
            XMPPError.Condition condition) {
        return respondError(response, XMPPError.getBuilder(condition));
    }

    /**
     * Responds an error with an specific condition.
     * 
     * @param response the response to send.
     * @param condition the condition of the error.
     * @param specificCondition the adhoc command error condition.
     * @throws NotConnectedException 
     */
    private static IQ respondError(AdHocCommandData response, XMPPError.Condition condition,
            AdHocCommand.SpecificErrorCondition specificCondition)
    {
        XMPPError.Builder error = XMPPError.getBuilder(condition).addExtension(new AdHocCommandData.SpecificError(specificCondition));
        return respondError(response, error);
    }

    /**
     * Responds an error with an specific error.
     * 
     * @param response the response to send.
     * @param error the error to send.
     * @throws NotConnectedException 
     */
    private static IQ respondError(AdHocCommandData response, XMPPError.Builder error) {
        response.setType(IQ.Type.error);
        response.setError(error);
        return response;
    }

    /**
     * Creates a new instance of a command to be used by a new execution request
     * 
     * @param commandNode the command node that identifies it.
     * @param sessionID the session id of this execution.
     * @return the command instance to execute.
     * @throws XMPPErrorException if there is problem creating the new instance.
     */
    @SuppressWarnings("deprecation")
    private LocalCommand newInstanceOfCmd(String commandNode, String sessionID) throws XMPPErrorException
    {
        AdHocCommandInfo commandInfo = commands.get(commandNode);
        LocalCommand command;
        try {
            command = commandInfo.getCommandInstance();
            command.setSessionID(sessionID);
            command.setName(commandInfo.getName());
            command.setNode(commandInfo.getNode());
        }
        catch (InstantiationException e) {
            throw new XMPPErrorException(XMPPError.getBuilder(
                    XMPPError.Condition.internal_server_error));
        }
        catch (IllegalAccessException e) {
            throw new XMPPErrorException(XMPPError.getBuilder(
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
        private final Jid ownerJID;
        private LocalCommandFactory factory;

        public AdHocCommandInfo(String node, String name, Jid ownerJID,
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

        public Jid getOwnerJID() {
            return ownerJID;
        }
    }
}
