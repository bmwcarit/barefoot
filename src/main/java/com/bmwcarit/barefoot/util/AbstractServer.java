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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract server implementation for hosting a service that can be accessed via TCP/IP socket.
 */
public class AbstractServer {
    private final static Logger logger = LoggerFactory.getLogger(AbstractServer.class);

    private final int portNumber;
    private final int maxRequestTime;
    private final int maxResponseTime;
    private final int maxConnectionCount;

    private final ResponseFactory responseFactory;

    private ServerSocket server = null;
    private ExecutorService executor = null;
    private AtomicInteger openConnectionCount = null;

    /**
     * Result types of {@link ResponseHandler} objects.
     */
    public enum RESULT {
        SUCCESS, ERROR, TIMEOUT
    }

    /**
     * Creates a {@link AbstractServer} object. It expects the following properties:
     * <ul>
     * <li>server.port (optional, default: 1234)</li>
     * <li>server.time.request (milliseconds, optional, default: 15000)</li>
     * <li>server.time.response (milliseconds, optional, default: 60000)</li>
     * <li>server.connections (optional, default: 20)</li>
     * </ul>
     *
     * @param serverProperties {@link Properties} object containing all necessary server settings.
     * @param responseFactory {@link ResponseFactory} object that generates a
     *        {@link ResponseHandler} for each request message which, in turn, processes requests
     *        and creates response messages.
     */
    public AbstractServer(Properties serverProperties, ResponseFactory responseFactory) {
        this.portNumber = Integer.parseInt(serverProperties.getProperty("server.port", "1234"));
        this.maxRequestTime =
                Integer.parseInt(serverProperties.getProperty("server.timeout.request", "15000"));
        this.maxResponseTime =
                Integer.parseInt(serverProperties.getProperty("server.timeout.response", "60000"));
        this.maxConnectionCount =
                Integer.parseInt(serverProperties.getProperty("server.connections", "20"));
        this.responseFactory = responseFactory;

        logger.info("server.port={}", portNumber);
        logger.info("server.timeout.request={}", maxRequestTime);
        logger.info("server.timeout.response={}", maxResponseTime);
        logger.info("server.connections={}", maxConnectionCount);
    }

    private static class RequestHandler implements Callable<String> {
        private final BufferedReader reader;

        public RequestHandler(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public String call() throws IOException {
            return reader.readLine();
        }
    }

    /**
     * Abstract factory for generation of a {@link ResponseHandler} object for each request message.
     */
    public static abstract class ResponseFactory {
        /**
         * Creates a {@link ResponseHandler} object for a request message.
         *
         * @param request Request message.
         * @return {@link ResponseHandler} object.
         */
        protected abstract ResponseHandler response(String request);
    }

    /**
     * Abstract response handler that processes request messages and generates response messages.
     */
    protected static abstract class ResponseHandler implements Callable<Tuple<RESULT, String>> {
        private final String request;
        private final StringBuilder response = new StringBuilder();

        /**
         * Creates {@link ResponseHandler} object.
         *
         * @param request Request message.
         */
        public ResponseHandler(String request) {
            this.request = request;
        }

        /**
         * Processes a request message, generates a response, and returns a {@link RESULT}.
         *
         * @param request Request message.
         * @param response {@link StringBuilder} for generation of the response.
         * @return {@link RESULT} type.
         */
        protected abstract RESULT response(String request, StringBuilder response);

        @Override
        public Tuple<RESULT, String> call() throws IOException {
            RESULT result = response(request, response);
            return new Tuple<>(result, response.toString());
        }
    }

    private class ClientHandler extends Thread {
        private final Socket client;

        public ClientHandler(Socket client) {
            this.client = client;
        }

        @Override
        public void run() {
            class StopException extends Exception {
                private static final long serialVersionUID = -7806828927072091763L;
            }

            try {
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter writer = new PrintWriter(client.getOutputStream());
                FutureTask<String> requestHandler = new FutureTask<>(new RequestHandler(reader));
                executor.execute(requestHandler);

                String request = null;
                try {
                    request = requestHandler.get(maxRequestTime, TimeUnit.MILLISECONDS);

                    logger.trace("{}:{} request - {}", client.getInetAddress().getHostAddress(),
                            client.getPort(), request);
                } catch (TimeoutException e) {
                    requestHandler.cancel(true);

                    logger.error("{}:{} request handler timeout",
                            client.getInetAddress().getHostAddress(), client.getPort());

                    writer.println("TIMEOUT");
                    writer.flush();

                    throw new StopException();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("{}:{} request handler exception - {}",
                            client.getInetAddress().getHostAddress(), client.getPort(),
                            e.getMessage());

                    throw new StopException();
                }

                FutureTask<Tuple<RESULT, String>> responseHandler =
                        new FutureTask<>(responseFactory.response(request));
                executor.execute(responseHandler);

                try {
                    Tuple<RESULT, String> response =
                            responseHandler.get(maxResponseTime, TimeUnit.MILLISECONDS);

                    logger.trace("{}:{} response code {}", client.getInetAddress().getHostAddress(),
                            client.getPort(), response.one());

                    writer.println(response.one());

                    if (response.one() == RESULT.SUCCESS) {
                        writer.println(response.two());
                    }

                    writer.flush();
                } catch (TimeoutException e) {
                    responseHandler.cancel(true);

                    logger.error("{}:{} response handler timeout",
                            client.getInetAddress().getHostAddress(), client.getPort());

                    writer.println("TIMEOUT");
                    writer.flush();

                    throw new StopException();
                } catch (InterruptedException | ExecutionException e) {
                    logger.error("{}:{} response handler exception - {}",
                            client.getInetAddress().getHostAddress(), client.getPort(),
                            e.getMessage());

                    throw new StopException();
                }
            } catch (StopException e) {
                logger.trace("{}:{} client handler stopped execution",
                        client.getInetAddress().getHostAddress(), client.getPort());
            } catch (Exception e) {
                logger.error("{}:{} client handler stopped due to unexpected error - {}",
                        client.getInetAddress().getHostAddress(), client.getPort(), e.getMessage());
            } finally {
                try {
                    if (!client.isClosed()) {
                        client.close();
                    }
                } catch (IOException e) {
                    logger.error("{}:{} connection lost", client.getInetAddress().getHostAddress(),
                            client.getPort());
                }
                logger.trace("{}:{} connection closed ({} open)",
                        client.getInetAddress().getHostAddress(), client.getPort(),
                        openConnectionCount.decrementAndGet());
            }
        }
    }

    /**
     * Starts server.
     *
     * @throws RuntimeException thrown on fatal configuration errors.
     */
    @SuppressWarnings("finally")
    public void runServer() throws RuntimeException {

        if (server != null || executor != null) {
            throw new RuntimeException();
        }

        executor = Executors.newFixedThreadPool(maxConnectionCount);
        openConnectionCount = new AtomicInteger();

        try {
            server = new ServerSocket(portNumber, maxConnectionCount);
            logger.info("listening on port {} ...", portNumber);
        } catch (Exception e) {
            logger.error("opening server socket failed - {}", e.getMessage());
            throw new RuntimeException();
        }

        try {
            while (!server.isClosed()) {
                /*
                 * Uses this work-around to ensure a maximum number of open connections to clients.
                 * This is necessary because the ServerSocket's backlog parameter has no effect on
                 * the accept method, i.e., it still accepts more connections than specified with
                 * backlog. Note that incoming connections are still queued and, hence, any incoming
                 * connection that exceeds the maximum number is not refused on the client side.
                 */
                while (openConnectionCount.get() == maxConnectionCount) {
                    try {
                        logger.info("maximum connection count reached, sleep a moment");
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        logger.error("maximum connection sleep");
                    }
                }
                /**/
                Socket client = server.accept();
                logger.trace("{}:{} connection accepted ({} open)",
                        client.getInetAddress().getHostAddress(), client.getPort(),
                        openConnectionCount.incrementAndGet());
                ClientHandler handler = new ClientHandler(client);
                handler.setDaemon(true);
                handler.start();
            }
        } catch (SocketException e) {
            logger.info("closed");
        } catch (IOException e) {
            logger.error("I/O exception - {}", e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                if (server == null) {
                    throw new RuntimeException();
                } else if (!server.isClosed()) {
                    server.close();
                    throw new RuntimeException();
                } else {
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException();
            } finally {
                server = null;
                executor = null;
            }
        }
    }

    /**
     * Stops server.
     */
    public void stopServer() {
        logger.info("received shutdown signal");
        if (server != null && !server.isClosed()) {
            try {
                logger.info("closing server ...");
                server.close();
            } catch (IOException e) {
                logger.error("server closing error {}", e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Gets portNumber of the server.
     *
     * @return Port number of the server.
     */
    public int getPortNumber() {
        return this.portNumber;
    }

    /**
     * Gets maximum request time of the server.
     *
     * @return Maximum request time of the server.
     */
    public double getMaxRequestTime() {
        return this.maxRequestTime;
    }

    /**
     * Gets maximum response time of the server.
     *
     * @return Maximum response time of the server.
     */
    public double getMaxResponseTime() {
        return this.maxResponseTime;
    }

    /**
     * Gets maximum connection count of the server.
     *
     * @return Maximum connection count of the server.
     */
    public double getMaxConnectionCount() {
        return this.maxConnectionCount;
    }

    /**
     * Gets {@link ResponseFactory} object of the server.
     *
     * @return {@link ResponseFactory} object of the server.
     */
    public ResponseFactory getResponseFactory() {
        return this.responseFactory;
    }
}
