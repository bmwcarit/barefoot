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

package com.bmwcarit.barefoot.matcher;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.bmwcarit.barefoot.matcher.MatcherServer.GeoJSONOutputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.InputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.OutputFormatter;

public class ServerTest {

    private class Server implements Runnable {
        @Override
        public void run() {
            ServerControl.initServer("config/server.properties", "config/oberbayern.properties",
                    new InputFormatter(), new OutputFormatter());
            ServerControl.runServer();
        }

        public void start() {
            (new Thread(this)).start();
        }

        public void stop() {
            ServerControl.stopServer();
        }
    }

    private void sendRequest(InetAddress host, int port, JSONArray samples)
            throws InterruptedException, IOException, JSONException {
        int trials = 120;
        int timeout = 500;
        Socket client = null;

        while (client == null || !client.isConnected()) {
            try {
                client = new Socket(host, port);
            } catch (IOException e) {
                Thread.sleep(timeout);

                if (trials == 0) {
                    client.close();
                    throw new IOException(e.getMessage());
                } else {
                    trials -= 1;
                }
            }
        }

        PrintWriter writer = new PrintWriter(client.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        writer.println(samples.toString());
        writer.flush();

        String code = reader.readLine();
        assertEquals("SUCCESS", code);

        String response = reader.readLine();
        client.close();

        MatcherKState state = new MatcherKState(new JSONObject(response),
                new MatcherFactory(ServerControl.getServer().getMap()));

        OutputFormatter output = new GeoJSONOutputFormatter();
        PrintWriter out = new PrintWriter(
                ServerTest.class.getResource("").getPath() + "ServerTest-matching.json");
        out.println(output.format(null, state));
        out.close();

        assertEquals(samples.length(), state.sequence().size());
    }

    @Test
    public void testServer()
            throws IOException, JSONException, InterruptedException, ParseException {
        Server server = new Server();
        InetAddress host = InetAddress.getLocalHost();
        Properties properties = new Properties();
        properties.load(new FileInputStream("config/server.properties"));
        int port = Integer.parseInt(properties.getProperty("server.port"));

        server.start();
        {
            String json = new String(
                    Files.readAllBytes(
                            Paths.get(ServerTest.class.getResource("x0001-015.json").getPath())),
                    Charset.defaultCharset());
            sendRequest(host, port, new JSONArray(json));
        }
        server.stop();
    }
}
