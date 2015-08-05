/*
* Copyright (C) 2015, BMW Car IT GmbH
*
* Author: Sebastian Mattheis <sebastian.mattheis@bmw-carit.de>
*
* Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
* in compliance with the License. You may obtain a copy of the License at
* http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law or agreed to in
* writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
* WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific
* language governing permissions and limitations under the License.
 */

package com.bmwcarit.barefoot.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.LinkedList;
import java.util.List;

import org.junit.Test;

public class AbstractServerTest {

    private static class TestServer extends AbstractServer {

        public TestServer(int portNumber, int maxRequestTime, int maxResponseTime,
                int maxConnectionCount, int numExecutorThreads, boolean success) {
            super(portNumber, maxRequestTime, maxResponseTime, maxConnectionCount,
                    numExecutorThreads, new ResponseFactory(success));
        }

        public static class ResponseFactory extends AbstractServer.ResponseFactory {
            private final boolean success;

            public ResponseFactory(boolean success) {
                this.success = success;
            }

            @Override
            protected ResponseHandler response(String request) {
                return new ResponseHandler(request) {
                    @Override
                    protected RESULT response(String request, StringBuilder response) {
                        if (!success) {
                            return RESULT.ERROR;
                        }
                        try {
                            Thread.sleep(Integer.parseInt(request));
                        } catch (InterruptedException | NumberFormatException e) {
                        }
                        response.append("work " + request + " ms");
                        return RESULT.SUCCESS;
                    }
                };
            }
        }
    }

    @Test
    public void StartStopTest() {
        final TestServer server = new TestServer(12345, 200, 400, 2, 2, true);
        Thread thread = new Thread() {
            @Override
            public void run() {
                server.runServer();
            }
        };

        try {
            // Start server
            thread.start();
            Thread.sleep(100);

            // Stop server
            server.stopServer();
            thread.join(100);
        } catch (InterruptedException | RuntimeException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void ReponseSuccessTest() {
        final TestServer server = new TestServer(12345, 200, 400, 2, 2, true);
        Thread thread = new Thread() {
            @Override
            public void run() {
                server.runServer();
            }
        };

        try {
            int workTime = 200;

            // Start server
            thread.start();
            Thread.sleep(200);

            // Connect to server
            Socket client = new Socket(InetAddress.getLocalHost(), 12345);

            // Send request
            PrintWriter writer = new PrintWriter(client.getOutputStream());
            writer.println(workTime);
            writer.flush();

            Thread.sleep(workTime + 100);

            // Receive response
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(client.getInputStream()));
            String rescode = reader.readLine();
            String response = reader.readLine();

            // Close connection
            writer.close();
            reader.close();
            client.close();

            // Stop server
            server.stopServer();
            thread.join(100);

            assertEquals("SUCCESS", rescode);
            assertEquals("work " + workTime + " ms", response);
        } catch (InterruptedException | RuntimeException | IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void ReponseErrorTest() {
        final TestServer server = new TestServer(12345, 200, 400, 2, 2, false);
        Thread thread = new Thread() {
            @Override
            public void run() {
                server.runServer();
            }
        };

        try {
            int workTime = 200;

            // Start server
            thread.start();
            Thread.sleep(200);

            // Connect to server
            Socket client = new Socket(InetAddress.getLocalHost(), 12345);

            // Send request
            PrintWriter writer = new PrintWriter(client.getOutputStream());
            writer.println(workTime);
            writer.flush();

            Thread.sleep(workTime + 100);

            // Receive response
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(client.getInputStream()));
            String rescode = reader.readLine();

            // Close connection
            writer.close();
            reader.close();
            client.close();

            // Stop server
            server.stopServer();
            thread.join(100);

            assertEquals("ERROR", rescode);
        } catch (InterruptedException | RuntimeException | IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void RequestTimeoutTest() {
        final TestServer server = new TestServer(12345, 200, 400, 2, 2, true);
        Thread thread = new Thread() {
            @Override
            public void run() {
                server.runServer();
            }
        };

        try {
            // Start server
            thread.start();
            Thread.sleep(200);

            // Connect to server
            Socket client = new Socket(InetAddress.getLocalHost(), 12345);

            Thread.sleep(300);

            // Receive response
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(client.getInputStream()));
            String rescode = reader.readLine();

            // Close connection
            reader.close();
            client.close();

            // Stop server
            server.stopServer();
            thread.join(100);

            assertEquals("TIMEOUT", rescode);
        } catch (InterruptedException | RuntimeException | IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void ReponseTimeoutTest() {
        final TestServer server = new TestServer(12345, 200, 400, 2, 2, true);
        Thread thread = new Thread() {
            @Override
            public void run() {
                server.runServer();
            }
        };

        try {
            int workTime = 500;

            // Start server
            thread.start();
            Thread.sleep(200);

            // Connect to server
            Socket client = new Socket(InetAddress.getLocalHost(), 12345);

            // Send request
            PrintWriter writer = new PrintWriter(client.getOutputStream());
            writer.println(workTime);
            writer.flush();

            Thread.sleep(workTime + 100);

            // Receive response
            BufferedReader reader =
                    new BufferedReader(new InputStreamReader(client.getInputStream()));
            String rescode = reader.readLine();

            // Close connection
            writer.close();
            reader.close();
            client.close();

            // Stop server
            server.stopServer();
            thread.join(100);

            assertEquals("TIMEOUT", rescode);
        } catch (InterruptedException | RuntimeException | IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void ReponseConnectionCountTest() {
        final TestServer server = new TestServer(12345, 200, 400, 2, 2, true);
        Thread thread = new Thread() {
            @Override
            public void run() {
                server.runServer();
            }
        };

        try {
            int workTime = 300;
            int numClients = 5;

            // Start server
            thread.start();
            Thread.sleep(200);

            int success = 0, error = 0, connect = 0, timeout = 0;

            List<Socket> clients = new LinkedList<Socket>();
            for (int i = 0; i < numClients; ++i) {
                // Connect to server
                Socket client = new Socket();
                client.connect(new InetSocketAddress(InetAddress.getLocalHost(), 12345), 100);

                // Send request
                PrintWriter writer = new PrintWriter(client.getOutputStream());
                writer.println(workTime);
                writer.flush();
                clients.add(client);
            }

            Stopwatch sw = new Stopwatch();
            sw.start();
            for (Socket client : clients) {

                // Receive response
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(client.getInputStream()));
                switch (reader.readLine()) {
                    case "SUCCESS":
                        success += 1;
                        break;
                    case "ERROR":
                        error += 1;
                        break;
                    case "TIMEOUT":
                        timeout += 1;
                        break;
                }
                sw.stop();

                if (sw.ms() > workTime + 100) {
                    connect += 1;
                }

                // Close connection
                client.close();
            }

            // Stop server
            server.stopServer();
            thread.join(100);

            assertEquals(5, clients.size());
            assertEquals(5, success);
            assertEquals(0, error);
            assertEquals(3, connect);
            assertEquals(0, timeout);

        } catch (InterruptedException | RuntimeException | IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            fail();
        }
    }
}
