/**
 * $RCSfile: HttpServer.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:07 $
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx.jingle.nat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.StringTokenizer;

import org.jivesoftware.smackx.jingle.SmackLogger;

/**
 * A very Simple HTTP Server
 */
public class HttpServer {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(HttpServer.class);

	public HttpServer(int port) {
        ServerSocket server_socket;

        try {

            server_socket = new ServerSocket(port);
            LOGGER.debug("httpServer running on port " +
                    server_socket.getLocalPort());

            while (true) {
                Socket socket = server_socket.accept();
                LOGGER.debug("New connection accepted " +
                        socket.getInetAddress() +
                        ":" + socket.getPort());

                try {
                    HttpRequestHandler request =
                            new HttpRequestHandler(socket);

                    Thread thread = new Thread(request);

                    thread.start();
                }
                catch (Exception e) {
                    LOGGER.debug("", e);
                }
            }
        }

        catch (IOException e) {
            LOGGER.debug("", e);
        }

    }

    public static void main(String args[]) {
        HttpServer httpServer = new HttpServer(Integer.parseInt(args[0]));
    }

    class HttpRequestHandler implements Runnable {

        final static String CRLF = "\r\n";
        Socket socket;
        InputStream input;
        OutputStream output;
        BufferedReader br;

        public HttpRequestHandler(Socket socket) throws Exception {
            this.socket = socket;
            this.input = socket.getInputStream();
            this.output = socket.getOutputStream();
            this.br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        }

        public void run() {
            try {
                processRequest();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }

        private void processRequest() throws Exception {
            while (true) {

                String headerLine = br.readLine();
                LOGGER.debug(headerLine);
                if (headerLine.equals(CRLF) || headerLine.equals("")) break;

                StringTokenizer s = new StringTokenizer(headerLine);
                String temp = s.nextToken();

                if (temp.equals("GET")) {

                    String serverLine = "Server: Simple httpServer";
                    String contentTypeLine = "text";
                    String entityBody = "";
                    String contentLengthLine;
                    String statusLine = "HTTP/1.0 200 OK" + CRLF;
                    contentLengthLine = "Content-Length: "
                            + (new Integer(entityBody.length())).toString() + CRLF;
                    contentTypeLine = "text/html";

                    output.write(statusLine.getBytes());

                    output.write(serverLine.getBytes());

                    output.write(contentTypeLine.getBytes());
                    output.write(contentLengthLine.getBytes());

                    output.write(CRLF.getBytes());

                    output.write(entityBody.getBytes());

                }
            }

            try {
                output.close();
                br.close();
                socket.close();
            }
            catch (Exception e) {
                // Do Nothing
                e.printStackTrace();
            }
        }
    }
}