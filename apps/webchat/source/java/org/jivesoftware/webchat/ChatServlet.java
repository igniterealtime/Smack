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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.packet.Message;

import javax.servlet.http.*;
import javax.servlet.*;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * For now, it's assumed the all JSP pages are in the context root. <p>
 *
 * Init params:<ul>
 *      <li> host:
 *      <li> port (optional):
 *      <li> SSLEnabled (optional):
 * </ul>
 *
 *
 */
public class ChatServlet extends HttpServlet implements HttpSessionListener {

    private static Map chatData = new HashMap();
    private static EmoticonFilter emoticonFilter = new EmoticonFilter();
    private static URLFilter urlFilter = new URLFilter();

    private String host;
    private int port = -1;
    private boolean SSLEnabled;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        ServletContext context = config.getServletContext();
        host = context.getInitParameter("host");
        if (host == null) {
            throw new ServletException("Init parameter \"host\" must be set.");
        }
        String portString = context.getInitParameter("port");
        if (portString != null) {
            try {
                port = Integer.parseInt(portString);
            }
            catch (NumberFormatException nfe) {
                throw new ServletException("Init parameter \"port\" must be a valid number.", nfe);
            }
        }
        SSLEnabled = Boolean.valueOf(context.getInitParameter("SSLEnabled")).booleanValue();
    }

    public void destroy() {
        super.destroy();
        // Clean up.
        for (Iterator i=chatData.values().iterator(); i.hasNext(); ) {
            ChatData data = (ChatData)i.next();
            if (data.groupChat != null) {
                data.groupChat.leave();
            }
            data.connection.close();
        }
    }

    protected void service(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException
    {
        HttpSession session = request.getSession();
        String sessionID = session.getId();
        String path = request.getContextPath();

        String command = request.getParameter("command");
        if ("login".equals(command)) {
            String returnCode = handleLogin(request, response);
            if ("success".equals(returnCode)) {
                response.sendRedirect(path + "/chat-launcher.jsp");
                return;
            }
            else {
                // error, so go back to index page (so errors can be printed)
                RequestDispatcher rd = request.getRequestDispatcher("/index.jsp");
                rd.forward(request, response);
                return;
            }
        }
        else if ("logout".equals(command)) {
            XMPPConnection con = (XMPPConnection)chatData.get(sessionID);

        }
        else if ("write".equals(command)) {
            String message = request.getParameter("message");
            ChatData data = (ChatData)chatData.get(sessionID);
            if (message == null) {
                writeData("Parameter \"message\" is required.", response);
                return;
            }
            else if (data == null) {
                writeData("Must login first.", response);
                return;
            }
            try {
                data.groupChat.sendMessage(message);
                StringBuffer reply = new StringBuffer();
                reply.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");

                reply.append("<html><head>\n");
                reply.append("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n");
                reply.append("<title>Chat Form</title>");
                reply.append("</head><body>\n");
                reply.append("<form action=\"ChatServlet\" name=\"chatform\" method=\"post\">\n");
                reply.append("<input type=\"hidden\" name=\"command\" value=\"write\">\n");
                reply.append("<input type=\"hidden\" name=\"message\" value=\"\">\n");
                reply.append("</form></body></html>");
                writeData(reply.toString(), response);
            }
            catch (XMPPException e) {
                e.printStackTrace();
            }
        }
        else if ("read".equals(command))  {
            ChatData data = (ChatData)chatData.get(sessionID);
            if (data == null) {
                writeData("Must login first.", response);
                return;
            }
            StringBuffer reply = new StringBuffer();
            reply.append("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 Transitional//EN\">\n");
            reply.append("<html>\n<head>\n");
            reply.append("<title>Chat Read Page</title>\n");
            reply.append("<meta http-equiv=\"refresh\" content=\"2\">\n");
            reply.append("<script language=\"JavaScript\" type=\"text/javascript\">\n");
            reply.append("var data = window.parent.frames['data'].data;\n");
            reply.append("if (data != null) {\n");
            boolean foundData = false;
            Message message = data.groupChat.pollMessage();
            while (message != null) {
                foundData = true;
                String from = message.getFrom();
                // We want the the user's nickname
                from = StringUtils.parseResource(from);
                String body = message.getBody();
                // escape HTML
                body = replace(body, "&", "&amp;");
                body = replace(body, "<", "&lt;");
                body = replace(body, ">", "&gt;");
                // replace newlines in the body:
                body = replace(body, "\r", "");
                body = replace(body, "\n", "<br>");
                // escape quotes
                body = replace(body, "\"", "&quot;");
                // Apply emoticons
                body = urlFilter.applyFilter(body);
                body = emoticonFilter.applyFilter(body);
                reply.append("\tdata[data.length] = new Array(\"" + from + "\", \"" + body + "\");\n");
                message = data.groupChat.pollMessage();
            }
            if (foundData) {
                reply.append("window.parent.frames['main'].location.reload();\n");
            }
            reply.append("}\n");
            reply.append("</script>\n");
            reply.append("</head>\n");
            reply.append("<body></body>\n");
            reply.append("</html>");
            writeData(reply.toString(), response);
        }
        else if (command != null) {
            writeData("Invalid command.", response);
        }
        else {
            writeData("Jive Messenger Chat Servlet", response);
        }
    }

    /**
     * Handles all login logic.
     *
     * @param request
     * @param response
     */
    private String handleLogin(HttpServletRequest request, HttpServletResponse response) {
        // The session ID - used to identify different chat sessions
        String sessionID = request.getSession().getId();
        // Get expected parameters
        String username = request.getParameter("username");
        String password = request.getParameter("password");
        String room = request.getParameter("room");
        String nickname = request.getParameter("nickname");
        // Validate parameters
        Map errors = new HashMap();
        if (username == null || "".equals(username.trim())) {
            errors.put("username","");
        }
        if (password == null || "".equals(password.trim())) {
            errors.put("password","");
        }
        if (room == null || "".equals(room.trim())) {
            errors.put("room","");
        }
        if (nickname == null || "".equals(nickname.trim())) {
            errors.put("nickname","");
        }
        // If there were no errors, continue
        if (errors.size() == 0) {
            ChatData data = (ChatData)chatData.get(sessionID);
            // If a connection already exists for this session, close it before creating
            // another.
            if (data != null) {
                if (data.groupChat != null) {
                    data.groupChat.leave();
                }
                data.connection.close();
            }
            // Otherwise, create new a new connection.
            else {
                data = new ChatData();
            }
            try {
                // Create connection.
                if (!SSLEnabled) {
                    if (port != -1) {
                        data.connection = new XMPPConnection(host, port);
                    }
                    else {
                        data.connection = new XMPPConnection(host);
                    }
                }
                else {
                     if (port != -1) {
                        data.connection = new SSLXMPPConnection(host, port);
                    }
                    else {
                        data.connection = new SSLXMPPConnection(host);
                    }
                }
                // Login
                data.connection.login(username, password, "WebChat");
                // Join groupChat room.
                data.groupChat = data.connection.createGroupChat(room);
                data.groupChat.join(nickname);

                // Save chat data in the map.
                chatData.put(sessionID, data);

                // Put the user's nickname in the session - this is used by the view to correctly
                // display the user's messages in a different color:
                request.getSession().setAttribute("messenger.servlet.nickname", nickname);
            }
            catch (XMPPException e) {
                errors.put("general", e.getMessage());
            }
            catch (ClassCastException e) {
                // TODO: remove this catch - it's just a bug workaround for now
                errors.put("general","");
                e.printStackTrace();
            }
        }

        if (errors.size() > 0) {
            // Add the error map to the request as an attribute:
            request.setAttribute("messenger.servlet.errors",errors);
            return "error";
        }
        else {
            return "success";
        }
    }

    // HttpSessionListener Interface

    public void sessionCreated(HttpSessionEvent event) {
        // Do nothing.
    }

    public void sessionDestroyed(HttpSessionEvent event) {
        // Close the XMPPConnection if there is one.
        String sessionID = event.getSession().getId();
        ChatData data = (ChatData)chatData.get(sessionID);
        if (data.groupChat != null) {
            data.groupChat.leave();
        }
        data.connection.close();
    }

    private void writeData(String data, HttpServletResponse response) {
        try {
            PrintWriter out = response.getWriter();
            response.setContentType("text/html");
            out.println(data);
            out.close();
        }
        catch (IOException ioe) {}
    }

    private class ChatData {
        public XMPPConnection connection;
        public GroupChat groupChat;
    }

    /**
     * Replaces all instances of oldString with newString in string.
     *
     * @param string the String to search to perform replacements on
     * @param oldString the String that should be replaced by newString
     * @param newString the String that will replace all instances of oldString
     *
     * @return a String will all instances of oldString replaced by newString
     */
    public static final String replace(String string, String oldString, String newString) {
        if (string == null) {
            return null;
        }
        // If the newString is null  just return the string since there's nothing
        // to replace.
        if (newString == null) {
            return string;
        }
        int i=0;
        // Make sure that oldString appears at least once before doing any processing.
        if ( ( i=string.indexOf(oldString, i) ) >= 0 ) {
            // Use char []'s, as they are more efficient to deal with.
            char [] string2 = string.toCharArray();
            char [] newString2 = newString.toCharArray();
            int oLength = oldString.length();
            StringBuffer buf = new StringBuffer(string2.length);
            buf.append(string2, 0, i).append(newString2);
            i += oLength;
            int j = i;
            // Replace all remaining instances of oldString with newString.
            while ( ( i=string.indexOf(oldString, i) ) > 0 ) {
                buf.append(string2, j, i-j).append(newString2);
                i += oLength;
                j = i;
            }
            buf.append(string2, j, string2.length - j);
            return buf.toString();
        }
        return string;
    }
}