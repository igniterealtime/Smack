/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2007 Jive Software.
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
/**
 * $RCSfile: HttpServer.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:07 $
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2007 Jive Software.
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