/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package org.jivesoftware.webchat;

import java.io.*;
import java.util.*;

import javax.servlet.*;
import javax.servlet.http.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.util.StringUtils;


/**
 * An extension of HttpServlet customized to handle transactions between N webclients
 *  and M chats located on a given XMPP server. While N >= M in the case of group chats,
 *  the code will currently, never the less, hold onto N connections to the XMPP server.<br>
 *
 * It is assumed that all JSP pages are in the context root. The init params should be:
 * <ul>
 *      <li> host</li>
 *      <li> port (optional)</li>
 *      <li> SSLEnabled (optional)</li>
 * </ul>
 *
 * @author Bill Lynch
 * @author loki der quaeler
 */
public class JiveChatServlet
    extends HttpServlet
    implements HttpSessionListener, PacketListener {

    static public final String JIVE_WEB_CHAT_RESOURCE_NAME = "WebChat";

    static protected long PACKET_RESPONSE_TIMEOUT_MS = 5000;

    static protected String CHAT_LAUNCHER_URI_SUFFIX = "/chat-launcher.jsp";
    static protected String CREATE_ACCOUNT_URI = "/account_creation.jsp";
    static protected String LOGIN_URI = "/index.jsp";

    static protected String ERRORS_ATTRIBUTE_STRING = "messenger.servlet.errors";
    static protected String NICKNAME_ATTRIBUTE_STRING = "messenger.servlet.nickname";
    static protected String ROOM_ATTRIBUTE_STRING = "messenger.servlet.room";

    static protected String HOST_PARAM_STRING = "host";
    static protected String PORT_PARAM_STRING = "port";
    static protected String SSL_PARAM_STRING = "SSLEnabled";

    static protected String COMMAND_PARAM_STRING = "command";
    static protected String NICKNAME_PARAM_STRING = "nickname";
    static protected String PASSWORD_PARAM_STRING = "password";
    static protected String RETYPED_PASSWORD_PARAM_STRING = "password_zwei";
    static protected String ROOM_PARAM_STRING = "room";
    static protected String USERNAME_PARAM_STRING = "username";

    static protected String ANON_LOGIN_COMMAND_STRING = "anon_login";
    static protected String CREATE_ACCOUNT_COMMAND_STRING = "create_account";
    static protected String LOGIN_COMMAND_STRING = "login";
    static protected String LOGOUT_COMMAND_STRING = "logout";
    static protected String READ_COMMAND_STRING = "read";
    static protected String SILENCE_COMMAND_STRING = "silence";
    static protected String WRITE_COMMAND_STRING = "write";

    static protected String MESSAGE_REQUEST_STRING = "message";

    // is this value used elsewhere? (if not, why a string?) PENDING
    static protected String ERROR_RETURN_CODE_STRING = "error";
    static protected String SUCCESS_RETURN_CODE_STRING = "success";

    // k/v :: S(session id) / ChatData
    static protected Map SESSION_CHATDATA_MAP = new HashMap();
    // k/v :: S(unique root of packet ids) / ChatData
    static protected Map PACKET_ROOT_CHATDATA_MAP = new HashMap();

    static protected EmoticonFilter EMOTICONFILTER = new EmoticonFilter();
    static protected URLFilter URLFILTER = new URLFilter();


    protected String host;
    protected int port;
    protected boolean sslEnabled;

    public void init (ServletConfig config)
        throws ServletException {
        ServletContext context = null;
        String portParameter = null;

        super.init(config);

// XMPPConnection.DEBUG_ENABLED = true;        

        context = config.getServletContext();

        this.host = context.getInitParameter(HOST_PARAM_STRING);
        if (this.host == null) {
            throw new ServletException("Init parameter \"" + HOST_PARAM_STRING + "\" must be set.");
        }

        this.port = -1;

        portParameter = context.getInitParameter(PORT_PARAM_STRING);
        if (portParameter != null) {
            try {
                this.port = Integer.parseInt(portParameter);
            } catch (NumberFormatException nfe) {
                throw new ServletException("Init parameter \"" + PORT_PARAM_STRING
                                                + "\" must be a valid number.", nfe);
            }
        }

        this.sslEnabled
                    = Boolean.valueOf(context.getInitParameter(SSL_PARAM_STRING)).booleanValue();
    }

    /**
     * Take care of closing down everything we're holding on to, then bubble up the destroy
     *  to our superclass.
     */
    public void destroy () {
        synchronized (SESSION_CHATDATA_MAP) {
            for (Iterator i = SESSION_CHATDATA_MAP.values().iterator(); i.hasNext(); ) {
                ChatData chatData = (ChatData)i.next();

                if (chatData.groupChat != null) {
                    chatData.groupChat.leave();
                }

                chatData.connection.close();
            }
        }

        super.destroy();
    }

    protected void service (HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException {
        HttpSession session = request.getSession();
        String sessionID = session.getId();
        String path = request.getContextPath();
        String command = request.getParameter(COMMAND_PARAM_STRING);

        if (READ_COMMAND_STRING.equals(command))  {
            ChatData chatData = (ChatData)SESSION_CHATDATA_MAP.get(sessionID);
            StringBuffer reply = null;
            boolean foundData = false;
            Message message = null;
            int i = 0;

            if (chatData == null) {
                this.writeData("Must login first.", response);

                return;
            }

            reply = new StringBuffer();
            reply.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
            reply.append("<html><head><title>Chat Read Page</title>\n");
            reply.append("<meta http-equiv=\"refresh\" content=\"2\">\n");
            reply.append("<script language=\"Javascript\" src=\"common.js\"></script>");
            reply.append("<script language=\"JavaScript\" type=\"text/javascript\">\n");
            reply.append("  var nickname = \"");
            reply.append(request.getSession().getAttribute(NICKNAME_ATTRIBUTE_STRING));
            reply.append("\";\n");

            message = chatData.groupChat.pollMessage();
            while (message != null) {
                String from = message.getFrom();
                String body = message.getBody();

                // Get the the user's nickname
                from = StringUtils.parseResource(from);

                // PENDING - stop using the replace method

                // encode the HTML special characters
                body = this.replace(body, "&", "&amp;");
                body = this.replace(body, "<", "&lt;");
                body = this.replace(body, ">", "&gt;");

                // replace newlines in the body:
                body = this.replace(body, "\r", "");
                body = this.replace(body, "\n", "<br>");

                // encode the quotes
                body = this.replace(body, "\"", "&quot;");

                // encode the embedded urls
                body = URLFILTER.applyFilter(body);

                // Apply emoticons
                body = EMOTICONFILTER.applyFilter(body);

                if (from.length() == 0) {
                    reply.append("  var body").append(i).append(" = \"").append(body);
                    reply.append("\";\n  addChatText(body").append(i).append(", true);\n");
                } else {
                    reply.append("  var from").append(i).append(" = \"").append(from);
                    reply.append("\";\n  var body").append(i).append(" = \"").append(body);
                    reply.append("\";\n  addUserName(from").append(i);
                    reply.append(");\n  addChatText(body").append(i).append(", false);\n");
                }

                message = chatData.groupChat.pollMessage();

                foundData = true;

                i++;
            }

            synchronized (chatData.newJoins) {
                synchronized (chatData.newDepartures) {
                    Iterator it = chatData.newJoins.iterator();

                    i = 0;

                    while (it.hasNext()) {
                        reply.append("  var joined").append(i).append(" = \"").append(it.next());
                        reply.append("\";\n  userJoined(joined").append(i).append(");\n");

                        i++;
                    }

                    i = 0;
                    it = chatData.newDepartures.iterator();
                    while (it.hasNext()) {
                        reply.append("  var departed").append(i).append(" = \"").append(it.next());
                        reply.append("\";\n  userDeparted(departed").append(i).append(");\n");

                        i++;
                    }

                    chatData.newJoins.clear();
                    chatData.newDepartures.clear();
                }
            }

            reply.append("</script>\n</head><body></body></html>");

            this.writeData(reply.toString(), response);
        } else if (WRITE_COMMAND_STRING.equals(command)) {
            String message = request.getParameter(MESSAGE_REQUEST_STRING);
            ChatData chatData = (ChatData)SESSION_CHATDATA_MAP.get(sessionID);

            if (message == null) {
                this.writeData("Parameter \"" + MESSAGE_REQUEST_STRING + "\" is required.",
                               response);

                return;
            } else if (chatData == null) {
                this.writeData("Must login first.", response);

                return;
            }

            try {
                StringBuffer reply = new StringBuffer();

                chatData.groupChat.sendMessage(message.trim());

                reply.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
                reply.append("<html><head>\n");
                reply.append(
                       "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
                reply.append("<title>Chat Form</title>");
                reply.append("</head><body>\n");
                reply.append("<form name=\"chatform\" action=\"").append(path);
                reply.append("/ChatServlet\" method=\"post\">\n");
                reply.append("<input type=\"hidden\" name=\"command\" value=\"write\">\n");
                reply.append("<input type=\"hidden\" name=\"message\" value=\"\">\n");
                reply.append("</form></body></html>");

                this.writeData(reply.toString(), response);
            } catch (XMPPException e) {
                // PENDING - better handling
                e.printStackTrace();
            }
        } else if (LOGOUT_COMMAND_STRING.equals(command)) {
            ChatData chatData = null;

            synchronized (SESSION_CHATDATA_MAP) {
                chatData = (ChatData)SESSION_CHATDATA_MAP.remove(sessionID);
            }

            if (chatData != null) {
                if (chatData.groupChat != null) {
                    chatData.groupChat.leave();
                }

                synchronized (PACKET_ROOT_CHATDATA_MAP) {
                    Packet p = new IQ();
                    String root = this.getPacketIDRoot(p);

                    PACKET_ROOT_CHATDATA_MAP.remove(root);
                }

                chatData.connection.close();
            }
        } else if (ANON_LOGIN_COMMAND_STRING.equals(command)) {
            String returnCode = this.handleLogin(request, true);

            if (SUCCESS_RETURN_CODE_STRING.equals(returnCode)) {
                response.sendRedirect(path + CHAT_LAUNCHER_URI_SUFFIX);
            } else {
                // error, return to the original page to display errors and allow re-attempts
                RequestDispatcher rd = request.getRequestDispatcher(LOGIN_URI);

                rd.forward(request, response);
            }
        } else if (LOGIN_COMMAND_STRING.equals(command)) {
            String returnCode = this.handleLogin(request, false);

            if (SUCCESS_RETURN_CODE_STRING.equals(returnCode)) {
                response.sendRedirect(path + CHAT_LAUNCHER_URI_SUFFIX);
            } else {
                // error, return to the original page to display errors and allow re-attempts
                RequestDispatcher rd = request.getRequestDispatcher(LOGIN_URI);

                rd.forward(request, response);
            }
        } else if (CREATE_ACCOUNT_COMMAND_STRING.equals(command)) {
            String returnCode = this.createAccount(request);

            if (SUCCESS_RETURN_CODE_STRING.equals(returnCode)) {
                response.sendRedirect(path + LOGIN_URI);
            } else {
                // error, return to the original page to display errors and allow re-attempts
                RequestDispatcher rd = request.getRequestDispatcher(CREATE_ACCOUNT_URI);

                rd.forward(request, response);
            }
        } else if (SILENCE_COMMAND_STRING.equals(command)) {
            // do nothing
        } else if (command != null) {
            this.writeData(("Invalid command: " + command), response);
        } else {
            this.writeData("Jive Messenger Chat Servlet", response);
        }
    }

    protected String getPacketIDRoot (Packet p) {
        if (p == null) {
            return null;
        }

        return p.getPacketID().substring(0, 5);
    }

    /**
     * Creates an account for the user data specified.
     */
    private String createAccount (HttpServletRequest request) {
        String sessionID = request.getSession().getId();
        String username = request.getParameter(USERNAME_PARAM_STRING);
        String password = request.getParameter(PASSWORD_PARAM_STRING);
        String retypedPassword = request.getParameter(RETYPED_PASSWORD_PARAM_STRING);
        Map errors = new HashMap();

        // PENDING: validate already taken username

        if ((username == null) || (username.trim().length() == 0)) {
            errors.put("empty_username", "");
        }

        if ((password == null) || (password.trim().length() == 0)) {
            errors.put("empty_password", "");
        }

        if ((retypedPassword == null) || (retypedPassword.trim().length() == 0)) {
            errors.put("empty_password_two", "");
        }

        if ((retypedPassword != null) && (password != null)
                && (! retypedPassword.equals(password))) {
            errors.put("mismatch_password", "");
        }

        // If there were no errors, continue
        if (errors.size() == 0) {
            ChatData chatData = (ChatData)SESSION_CHATDATA_MAP.get(sessionID);

            // If a connection already exists for this session, close it before creating
            //  another.
            if (chatData != null) {
                if (chatData.groupChat != null) {
                    chatData.groupChat.leave();
                }

                chatData.connection.close();
            }

            chatData = new ChatData();

            try {
                AccountManager am = null;

                // Create connection.
                if (! this.sslEnabled) {
                    if (port != -1) {
                        chatData.connection = new XMPPConnection(this.host, this.port);
                    } else {
                        chatData.connection = new XMPPConnection(this.host);
                    }
                } else {
                     if (port != -1) {
                        chatData.connection = new SSLXMPPConnection(this.host, this.port);
                    }
                    else {
                        chatData.connection = new SSLXMPPConnection(this.host);
                    }
                }

                am = chatData.connection.getAccountManager();

                // PENDING check whether the server even supports account creation
                am.createAccount(username, password);
            } catch (XMPPException e) {
                errors.put("general", "The server reported an error in account creation: "
                                           + e.getXMPPError().getMessage());
            }
        }

        if (errors.size() > 0) {
            request.setAttribute(ERRORS_ATTRIBUTE_STRING, errors);

            return ERROR_RETURN_CODE_STRING;
        }

        return SUCCESS_RETURN_CODE_STRING;
    }

    /**
     * Handles login logic.
     */
    private String handleLogin (HttpServletRequest request, boolean anonymous) {
        String sessionID = request.getSession().getId();
        String username = request.getParameter(USERNAME_PARAM_STRING);
        String password = request.getParameter(PASSWORD_PARAM_STRING);
        String room = request.getParameter(ROOM_PARAM_STRING);
        String nickname = request.getParameter(NICKNAME_PARAM_STRING);
        Map errors = new HashMap();

        // Validate parameters
        if ((! anonymous) && ((username == null) || (username.trim().length() == 0))) {
            errors.put(USERNAME_PARAM_STRING, "");
        }

        if ((! anonymous) && ((password == null) || (password.trim().length() == 0))) {
            errors.put(PASSWORD_PARAM_STRING, "");
        }

        if ((room == null) || (room.trim().length() == 0)) {
            errors.put(ROOM_PARAM_STRING, "");
        }

        if ((nickname == null) || (nickname.trim().length() == 0)) {
            errors.put(NICKNAME_PARAM_STRING, "");
        }

        // If there were no errors, continue
        if (errors.size() == 0) {
            ChatData chatData = (ChatData)SESSION_CHATDATA_MAP.get(sessionID);

            // If a connection already exists for this session, close it before creating
            //  another.
            if (chatData != null) {
                if (chatData.groupChat != null) {
                    chatData.groupChat.leave();
                }

                chatData.connection.close();
            }

            chatData = new ChatData();

            try {
                // Create connection.
                if (! this.sslEnabled) {
                    if (port != -1) {
                        chatData.connection = new XMPPConnection(this.host, this.port);
                    } else {
                        chatData.connection = new XMPPConnection(this.host);
                    }
                } else {
                     if (port != -1) {
                        chatData.connection = new SSLXMPPConnection(this.host, this.port);
                    }
                    else {
                        chatData.connection = new SSLXMPPConnection(this.host);
                    }
                }

                if (anonymous) {
                    Authentication a = new Authentication();
                    PacketCollector pc = chatData.connection.createPacketCollector(
                                                               new PacketIDFilter(a.getPacketID()));
                    Authentication responsePacket = null;

                    a.setType(IQ.Type.SET);

                    chatData.connection.sendPacket(a);

                    responsePacket = (Authentication)pc.nextResult(PACKET_RESPONSE_TIMEOUT_MS);
                    if (responsePacket == null) {
//                        throw new XMPPException("No response from the server.");
                    }
                    // check for error response

                    pc.cancel();

                    // since GroupChat isn't setting the 'from' in it's message sends,
                    //  i can't see a problem in not doing anything with the unique resource
                    //  we've just been given by the server. if GroupChat starts setting the
                    //  from, it would probably grab the information from the XMPPConnection
                    //  instance it holds, and as such we would then need to introduce the
                    //  concept of anonymous logins to XMPPConnection, or tell GroupChat what
                    //  to do what username is null or blank but a resource exists... PENDING
                } else {
                    chatData.connection.login(username, password, JIVE_WEB_CHAT_RESOURCE_NAME);
                }

                chatData.connection.addPacketListener(this,
                                                      new PacketTypeFilter(Presence.class));

                synchronized (SESSION_CHATDATA_MAP) {
                    SESSION_CHATDATA_MAP.put(sessionID, chatData);
                }

                synchronized (PACKET_ROOT_CHATDATA_MAP) {
                    Packet p = new IQ();
                    String root = this.getPacketIDRoot(p);

                    // PENDING -- we won't do anything with this packet, so it will ultimately look
                    //  to the server as though a packet has disappeared -- is this ok with the
                    //  server?
                    PACKET_ROOT_CHATDATA_MAP.put(root, chatData);
                }

                // Join groupChat room.
                chatData.groupChat = chatData.connection.createGroupChat(room);
                chatData.groupChat.join(nickname);

                // Put the user's nickname in the session - this is used by the view to correctly
                //  display the user's messages in a different color:
                request.getSession().setAttribute(NICKNAME_ATTRIBUTE_STRING, nickname);
                request.getSession().setAttribute(ROOM_ATTRIBUTE_STRING, room);
            } catch (XMPPException e) {
                XMPPError err = e.getXMPPError();

                errors.put("general", ((err != null) ? err.getMessage() : e.getMessage()));

                if (chatData.groupChat != null) {
                    chatData.groupChat.leave();
                }
            }
        }

        if (errors.size() > 0) {
            request.setAttribute(ERRORS_ATTRIBUTE_STRING, errors);

            return ERROR_RETURN_CODE_STRING;
        }

        return SUCCESS_RETURN_CODE_STRING;
    }

    private void writeData (String data, HttpServletResponse response) {
        try {
            PrintWriter responseWriter = response.getWriter();

            response.setContentType("text/html");

            responseWriter.println(data);
            responseWriter.close();
        } catch (IOException ioe) {
            // PENDING
        }
    }

    // a hack class to hold a data glom (really hacky)
    private class ChatData {

        private XMPPConnection connection;
        private GroupChat groupChat;
        private Set newJoins = new HashSet();
        private Set newDepartures = new HashSet();

    }

    /**
     * Replaces all instances of oldString with newString in string.
     *
     * PENDING - why is this final?
     * PENDING - take this out -- it fails under some cases...
     *
     * @param string the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     *
     * @return a String will all instances of oldString replaced by newString
     */
    static public final String replace (String string, String oldString, String newString) {
        int i = 0;

        // MAY RETURN THIS BLOCK
        if (string == null) {
            return null;
        }

        if (newString == null) {
            return string;
        }

        // Make sure that oldString appears at least once before doing any processing.
        if (( i=string.indexOf(oldString, i)) >= 0) {
            // Use char []'s, as they are more efficient to deal with.
            char[] string2 = string.toCharArray();
            char[] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(string2.length);
            int j = 1;

            buf.append(string2, 0, i).append(newString2);

            i += oLength;

            // Replace all remaining instances of oldString with newString.
            while ((i=string.indexOf(oldString, i)) > 0) {
                buf.append(string2, j, (i - j)).append(newString2);

                i += oLength;
                j = i;
            }

            buf.append(string2, j, (string2.length - j));

            return buf.toString();
        }

        return string;
    }

    /**
     *
     * HttpSessionListener implementation
     *
     */
    public void sessionCreated (HttpSessionEvent event) { }

    public void sessionDestroyed (HttpSessionEvent event) {
        String sessionID = event.getSession().getId();
        ChatData chatData = null;

        synchronized (SESSION_CHATDATA_MAP) {
            chatData = (ChatData)SESSION_CHATDATA_MAP.remove(sessionID);
        }

        if (chatData != null) {
            if (chatData.groupChat != null) {
                chatData.groupChat.leave();
            }

            synchronized (PACKET_ROOT_CHATDATA_MAP) {
                Packet p = new IQ();
                String root = this.getPacketIDRoot(p);

                PACKET_ROOT_CHATDATA_MAP.remove(root);
            }

            chatData.connection.close();
        }
    }

    /**
     *
     * PacketListener implementation
     *
     */
    public void processPacket (Packet packet) {
        Presence presence = (Presence)packet;
        String root = null;
        ChatData chatData = null;
        String userName = null;

        // MAY RETURN THIS BLOCK
        if (presence.getMode() == Presence.Mode.INVISIBLE) {
            return;
        }

        root = this.getPacketIDRoot(presence);
        chatData = (ChatData)PACKET_ROOT_CHATDATA_MAP.get(root);

        // MAY RETURN THIS BLOCK
        if (chatData == null) {
            return;
        }

        userName = StringUtils.parseResource(packet.getFrom());

        if (presence.getType() == Presence.Type.UNAVAILABLE) {
            synchronized (chatData.newDepartures) {
                synchronized (chatData.newJoins) {
                    chatData.newJoins.remove(userName);

                    chatData.newDepartures.add(userName);
                }
            }
        } else if (presence.getType() == Presence.Type.AVAILABLE) {
            synchronized (chatData.newJoins) {
                synchronized (chatData.newDepartures) {
                    chatData.newDepartures.remove(userName);

                    chatData.newJoins.add(userName);
                }
            }
        }
    }

}
