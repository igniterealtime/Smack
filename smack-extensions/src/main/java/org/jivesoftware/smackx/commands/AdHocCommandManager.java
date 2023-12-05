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

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.commands.packet.AdHocCommandData;
import org.jivesoftware.smackx.commands.packet.AdHocCommandData.AllowedAction;
import org.jivesoftware.smackx.commands.packet.AdHocCommandDataBuilder;
import org.jivesoftware.smackx.disco.AbstractNodeInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.xdata.form.SubmitForm;
import org.jivesoftware.smackx.xdata.packet.DataForm;

import org.jxmpp.jid.EntityFullJid;
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
 * @author Florian Schmaus
 */
public final class AdHocCommandManager extends Manager {
    public static final String NAMESPACE = "http://jabber.org/protocol/commands";

    private static final Logger LOGGER = Logger.getLogger(AdHocCommandManager.class.getName());

    /**
     * The session time out in seconds.
     */
    private static int DEFAULT_SESSION_TIMEOUT_SECS = 7 * 60;

    /**
     * Map an XMPPConnection with it AdHocCommandManager. This map have a key-value
     * pair for every active connection.
     */
    private static final Map<XMPPConnection, AdHocCommandManager> instances = new WeakHashMap<>();

    /**
     * Register the listener for all the connection creations. When a new
     * connection is created a new AdHocCommandManager is also created and
     * related to that connection.
     */
    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstance(connection);
            }
        });
    }

    /**
     * Returns the <code>AdHocCommandManager</code> related to the
     * <code>connection</code>.
     *
     * @param connection the XMPP connection.
     * @return the AdHocCommandManager associated with the connection.
     * @deprecated use {@link #getInstance(XMPPConnection)} instead.
     */
    @Deprecated
    public static AdHocCommandManager getAddHocCommandsManager(XMPPConnection connection) {
        return getInstance(connection);
    }

    /**
     * Returns the <code>AdHocCommandManager</code> related to the
     * <code>connection</code>.
     *
     * @param connection the XMPP connection.
     * @return the AdHocCommandManager associated with the connection.
     */
    public static synchronized AdHocCommandManager getInstance(XMPPConnection connection) {
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
    // TODO: Change to Map once Smack's minimum Android API level is 24 or higher.
    private final ConcurrentHashMap<String, AdHocCommandInfo> commands = new ConcurrentHashMap<>();

    /**
     * Map a command session ID with the instance LocalCommand. The LocalCommand
     * is the an objects that has all the information of the current state of
     * the command execution. Note: Key=session ID, Value=LocalCommand. Session
     * ID matches the sessionid attribute sent by command responders.
     */
    private final Map<String, AdHocCommandHandler> executingCommands = new ConcurrentHashMap<>();

    private final ServiceDiscoveryManager serviceDiscoveryManager;

    private int sessionTimeoutSecs = DEFAULT_SESSION_TIMEOUT_SECS;

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
                                List<DiscoverItems.Item> answer = new ArrayList<>();
                                Collection<AdHocCommandInfo> commandsList = commands.values();

                                EntityFullJid ourJid = connection().getUser();
                                if (ourJid == null) {
                                    LOGGER.warning("Local connection JID not available, can not respond to " + NAMESPACE + " node information");
                                    return null;
                                }

                                for (AdHocCommandInfo info : commandsList) {
                                    DiscoverItems.Item item = new DiscoverItems.Item(ourJid);
                                    item.setName(info.getName());
                                    item.setNode(info.getNode());
                                    answer.add(item);
                                }

                                return answer;
                            }
                        });

        // The packet listener and the filter for processing some AdHoc Commands
        // Packets
        // TODO: This handler being async means that requests for the same command could be handled out of order. Nobody
        // complained so far, and I could imagine that it does not really matter in practice. But it is certainly
        // something to keep in mind.
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(AdHocCommandData.ELEMENT,
                        AdHocCommandData.NAMESPACE, IQ.Type.set, Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                AdHocCommandData requestData = (AdHocCommandData) iqRequest;
                AdHocCommandData response = processAdHocCommand(requestData);
                assert response.getStatus() != null || response.getType() == IQ.Type.error;
                return response;
            }
        });
    }

    /**
     * Registers a new command with this command manager, which is related to a
     * connection. The <code>node</code> is an unique identifier of that command for
     * the connection related to this command manager. The <code>name</code> is the
     * human readable name of the command. The <code>class</code> is the class of
     * the command, which must extend {@link AdHocCommandHandler} and have a default
     * constructor.
     *
     * @param node the unique identifier of the command.
     * @param name the human readable name of the command.
     * @param clazz the class of the command, which must extend {@link AdHocCommandHandler}.
     * @throws SecurityException if there was a security violation.
     * @throws NoSuchMethodException if no such method is declared.
     */
    public void registerCommand(String node, String name, final Class<? extends AdHocCommandHandler> clazz) throws NoSuchMethodException, SecurityException {
        Constructor<? extends AdHocCommandHandler> constructor = clazz.getConstructor(String.class, String.class, String.class);
        registerCommand(node, name, new AdHocCommandHandlerFactory() {
            @Override
            public AdHocCommandHandler create(String node, String name, String sessionId) throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                return constructor.newInstance(node, name, sessionId);
            }
        });
    }

    /**
     * Registers a new command with this command manager, which is related to a
     * connection. The <code>node</code> is an unique identifier of that
     * command for the connection related to this command manager. The <code>name</code>
     * is the human readable name of the command. The <code>factory</code> generates
     * new instances of the command.
     *
     * @param node the unique identifier of the command.
     * @param name the human readable name of the command.
     * @param factory a factory to create new instances of the command.
     */
    public synchronized void registerCommand(String node, final String name, AdHocCommandHandlerFactory factory) {
        AdHocCommandInfo commandInfo = new AdHocCommandInfo(node, name, factory);

        AdHocCommandInfo existing = commands.putIfAbsent(node, commandInfo);
        if (existing != null) throw new IllegalArgumentException("There is already an ad-hoc command registered for " + node);

        // Set the NodeInformationProvider that will provide information about
        // the added command
        serviceDiscoveryManager.setNodeInformationProvider(node,
                new AbstractNodeInformationProvider() {
                    @Override
                    public List<String> getNodeFeatures() {
                        List<String> answer = new ArrayList<>();
                        answer.add(NAMESPACE);
                        // TODO: check if this service is provided by the
                        // TODO: current connection.
                        answer.add("jabber:x:data");
                        return answer;
                    }
                    @Override
                    public List<DiscoverInfo.Identity> getNodeIdentities() {
                        List<DiscoverInfo.Identity> answer = new ArrayList<>();
                        DiscoverInfo.Identity identity = new DiscoverInfo.Identity(
                                "automation", name, "command-node");
                        answer.add(identity);
                        return answer;
                    }
                });
    }

    public synchronized boolean unregisterCommand(String node) {
        AdHocCommandInfo commandInfo = commands.remove(node);
        if (commandInfo == null) return false;

        serviceDiscoveryManager.removeNodeInformationProvider(node);
        return true;
    }

    /**
     * Discover the commands of an specific JID. The <code>jid</code> is a
     * full JID.
     *
     * @param jid the full JID to retrieve the commands for.
     * @return the discovered items.
     * @throws XMPPException if the operation failed for some reason.
     * @throws SmackException if there was no response from the server.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public DiscoverItems discoverCommands(Jid jid) throws XMPPException, SmackException, InterruptedException {
        return serviceDiscoveryManager.discoverItems(jid, NAMESPACE);
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
    public AdHocCommand getRemoteCommand(Jid jid, String node) {
        return new AdHocCommand(connection(), node, jid);
    }

    /**
     * Process the AdHoc-Command stanza that request the execution of some
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
     * @param request the incoming AdHoc command request.
     */
    private AdHocCommandData processAdHocCommand(AdHocCommandData request) {
        String sessionId = request.getSessionId();

        final AdHocCommandHandler command;
        if (sessionId == null) {
            String commandNode = request.getNode();

            // A new execution request has been received. Check that the
            // command exists
            AdHocCommandInfo commandInfo = commands.get(commandNode);
            if (commandInfo == null) {
                // Requested command does not exist so return
                // item_not_found error.
                return respondError(request, null, StanzaError.Condition.item_not_found);
            }

            assert commandInfo.getNode().equals(commandNode);

            // Create a new instance of the command with the
            // corresponding session ID.
            try {
                command = commandInfo.getCommandInstance();
            }
            catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                LOGGER.log(Level.WARNING, "Could not instanciate ad-hoc command server", e);
                StanzaError xmppError = StanzaError.getBuilder()
                                .setCondition(StanzaError.Condition.internal_server_error)
                                .setDescriptiveEnText(e.getMessage())
                                .build();
                return respondError(request, null, xmppError);
            }
        } else {
            command = executingCommands.get(sessionId);
            // Check that a command exists for the specified sessionID
            // This also handles if the command was removed in the meanwhile
            // of getting the key and the value of the map.
            if (command == null) {
                return respondError(request, null, StanzaError.Condition.bad_request,
                        SpecificErrorCondition.badSessionid);
            }
        }


        final AdHocCommandDataBuilder responseBuilder = AdHocCommandDataBuilder.buildResponseFor(request)
                        .setSessionId(command.getSessionId());

        final AdHocCommandData response;
        /*
         * Since the requester could send two requests for the same
         * executing command i.e. the same session id, all the execution of
         * the action must be synchronized to avoid inconsistencies.
         */
        synchronized (command) {
            command.addRequest(request);

            if (sessionId == null) {
                response = processAdHocCommandOfNewSession(request, command, responseBuilder);
            } else {
                response = processAdHocCommandOfExistingSession(request, command, responseBuilder);
            }


            AdHocCommandResult commandResult = AdHocCommandResult.from(response);
            command.addResult(commandResult);
        }

        return response;
    }

    private AdHocCommandData createResponseFrom(AdHocCommandData request, AdHocCommandDataBuilder response, XMPPErrorException exception, String sessionId) {
        StanzaError error = exception.getStanzaError();

        // If the error type is cancel, then the execution is
        // canceled therefore the status must show that, and the
        // command be removed from the executing list.
        if (error.getType() == StanzaError.Type.CANCEL) {
            response.setStatus(AdHocCommandData.Status.canceled);

            executingCommands.remove(sessionId);

            return response.build();
        }

        return respondError(request, response, error);
    }

    private static AdHocCommandData createResponseFrom(AdHocCommandData request, AdHocCommandDataBuilder response, Exception exception) {
        StanzaError error = StanzaError.from(StanzaError.Condition.internal_server_error, exception.getMessage())
                        .build();
        return respondError(request, response, error);
    }

    private AdHocCommandData processAdHocCommandOfNewSession(AdHocCommandData request, AdHocCommandHandler command, AdHocCommandDataBuilder responseBuilder) {
        // Check that the requester has enough permission.
        // Answer forbidden error if requester permissions are not
        // enough to execute the requested command
        if (!command.hasPermission(request.getFrom())) {
            return respondError(request, responseBuilder, StanzaError.Condition.forbidden);
        }

        AdHocCommandData.Action action = request.getAction();

        // If the action is not execute, then it is an invalid action.
        if (action != null && !action.equals(AdHocCommandData.Action.execute)) {
            return respondError(request, responseBuilder, StanzaError.Condition.bad_request,
                    SpecificErrorCondition.badAction);
        }

        // Increase the state number, so the command knows in witch
        // stage it is
        command.incrementStage();

        final AdHocCommandData response;
         try {
            // Executes the command
            response = command.execute(responseBuilder);
        } catch (XMPPErrorException e) {
            return createResponseFrom(request, responseBuilder, e, command.getSessionId());
        } catch (NoResponseException | NotConnectedException | InterruptedException | IllegalStateException e) {
            return createResponseFrom(request, responseBuilder, e);
        }

        if (response.isExecuting()) {
            executingCommands.put(command.getSessionId(), command);
            // See if the session sweeper thread is scheduled. If not, start it.
            maybeWindUpSessionSweeper();
        }

        return response;
    }

    private AdHocCommandData processAdHocCommandOfExistingSession(AdHocCommandData request, AdHocCommandHandler command, AdHocCommandDataBuilder responseBuilder) {
        // Check if the Session data has expired (default is 10 minutes)
        long creationStamp = command.getCreationDate();
        if (System.currentTimeMillis() - creationStamp > sessionTimeoutSecs * 1000) {
            // Remove the expired session
            executingCommands.remove(command.getSessionId());

            // Answer a not_allowed error (session-expired)
            return respondError(request, responseBuilder, StanzaError.Condition.not_allowed,
                    SpecificErrorCondition.sessionExpired);
        }

        AdHocCommandData.Action action = request.getAction();

        // If the user didn't specify an action or specify the execute
        // action then follow the actual default execute action
        if (action == null || AdHocCommandData.Action.execute.equals(action)) {
            AllowedAction executeAction = command.getExecuteAction();
            if (executeAction != null) {
                action = executeAction.action;
            }
        }

        // Check that the specified action was previously
        // offered
        if (!command.isValidAction(action)) {
            return respondError(request, responseBuilder, StanzaError.Condition.bad_request,
                    SpecificErrorCondition.badAction);
        }

        AdHocCommandData response;
        try {
           DataForm dataForm;
           switch (action) {
           case next:
               command.incrementStage();
               dataForm = request.getForm();
               response = command.next(responseBuilder, new SubmitForm(dataForm));
               break;
           case complete:
                command.incrementStage();
                dataForm = request.getForm();
                responseBuilder.setStatus(AdHocCommandData.Status.completed);
                response = command.complete(responseBuilder, new SubmitForm(dataForm));
                // Remove the completed session
                executingCommands.remove(command.getSessionId());
                break;
            case prev:
                command.decrementStage();
                response = command.prev(responseBuilder);
                break;
            case cancel:
                command.cancel();
                responseBuilder.setStatus(AdHocCommandData.Status.canceled);
                response = responseBuilder.build();
                // Remove the canceled session
                executingCommands.remove(command.getSessionId());
                break;
            default:
                return respondError(request, responseBuilder, StanzaError.Condition.bad_request,
                                SpecificErrorCondition.badAction);
            }
        } catch (XMPPErrorException e) {
            return createResponseFrom(request, responseBuilder, e, command.getSessionId());
        } catch (NoResponseException | NotConnectedException | InterruptedException | IllegalStateException e) {
            return createResponseFrom(request, responseBuilder, e);
        }

        return response;
    }

    private boolean sessionSweeperScheduled;

    private int getSessionRemovalTimeoutSecs() {
        return sessionTimeoutSecs * 2;
    }

    private void sessionSweeper() {
        final long currentTime = System.currentTimeMillis();
        synchronized (this) {
            for (Iterator<Map.Entry<String, AdHocCommandHandler>> it = executingCommands.entrySet().iterator(); it.hasNext();) {
                Map.Entry<String, AdHocCommandHandler> entry = it.next();
                AdHocCommandHandler command = entry.getValue();

                long creationStamp = command.getCreationDate();
                // Check if the Session data has expired.
                // To remove it from the session list it waits for the double of
                // the of time out time. This is to let
                // the requester know why his execution request is
                // not accepted. If the session is removed just
                // after the time out, then once the user requests to
                // continue the execution he will received an
                // invalid session error and not a time out error.
                if (currentTime - creationStamp > getSessionRemovalTimeoutSecs() * 1000) {
                    // Remove the expired session
                    it.remove();
                }
            }

            sessionSweeperScheduled = false;
        }

        if (!executingCommands.isEmpty()) {
            maybeWindUpSessionSweeper();
        }
    };

    private synchronized void maybeWindUpSessionSweeper() {
        if (sessionSweeperScheduled) {
            return;
        }

        sessionSweeperScheduled = true;
        schedule(this::sessionSweeper, getSessionRemovalTimeoutSecs() + 1, TimeUnit.SECONDS);
    }

    /**
     * Responds an error with an specific condition.
     *
     * @param request the request that caused the error response.
     * @param condition the condition of the error.
     */
    private static AdHocCommandData respondError(AdHocCommandData request, AdHocCommandDataBuilder response,
            StanzaError.Condition condition) {
        return respondError(request, response, StanzaError.getBuilder(condition).build());
    }

    /**
     * Responds an error with an specific condition.
     *
     * @param request the request that caused the error response.
     * @param condition the condition of the error.
     * @param specificCondition the adhoc command error condition.
     */
    private static AdHocCommandData respondError(AdHocCommandData request, AdHocCommandDataBuilder response, StanzaError.Condition condition,
            SpecificErrorCondition specificCondition) {
        StanzaError error = StanzaError.getBuilder(condition)
                        .addExtension(new AdHocCommandData.SpecificError(specificCondition))
                        .build();
        return respondError(request, response, error);
    }

    /**
     * Responds an error with an specific error.
     *
     * @param request the request that caused the error response.
     * @param error the error to send.
     */
    private static AdHocCommandData respondError(AdHocCommandData request, AdHocCommandDataBuilder response, StanzaError error) {
        if (response == null) {
            return AdHocCommandDataBuilder.buildResponseFor(request, IQ.ResponseType.error).setError(error).build();
        }

        // Response may be not of IQ type error here, so switch that.
        return response.ofType(IQ.Type.error)
            .setError(error)
            .build();
    }

    public static void setDefaultSessionTimeoutSecs(int seconds) {
        if (seconds < 10) {
            throw new IllegalArgumentException();
        }
        DEFAULT_SESSION_TIMEOUT_SECS = seconds;
    }

    public void setSessionTimeoutSecs(int seconds) {
        if (seconds < 10) {
            throw new IllegalArgumentException();
        }

        sessionTimeoutSecs = seconds;
    }

    /**
     * Stores ad-hoc command information.
     */
    private final class AdHocCommandInfo {

        private final String node;
        private final String name;
        private final AdHocCommandHandlerFactory factory;

        private static final int MAX_SESSION_GEN_ATTEMPTS = 3;

        private AdHocCommandInfo(String node, String name, AdHocCommandHandlerFactory factory) {
            this.node = node;
            this.name = name;
            this.factory = factory;
        }

        public AdHocCommandHandler getCommandInstance() throws InstantiationException,
                IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            String sessionId;
            // TODO: The code below contains a race condition. Use CopncurrentHashMap.computeIfAbsent() to remove the
            // race condition once Smack's minimum Android API level 24 or higher.
            int attempt = 0;
            do {
                attempt++;
                if (attempt > MAX_SESSION_GEN_ATTEMPTS) {
                    throw new RuntimeException("Failed to compute unique session ID");
                }
                // Create new session ID
                sessionId = StringUtils.randomString(15);
            } while (executingCommands.containsKey(sessionId));

            return factory.create(node, name, sessionId);
        }

        public String getName() {
            return name;
        }

        public String getNode() {
            return node;
        }

    }
}
