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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

import com.bmwcarit.barefoot.matcher.MatcherServer.GeoJSONOutputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.InputFormatter;
import com.bmwcarit.barefoot.matcher.MatcherServer.OutputFormatter;

public class ServerTest {

    private class ServerRunnable implements Runnable {
        @Override
        public void run() {
            ServerControl.initServer(ServerTest.class.getResource("server.properties").getPath(),
                    ServerTest.class.getResource("oberbayern.properties").getPath(),
                    new InputFormatter(), new OutputFormatter());
            ServerControl.runServer();
        }

        public void stop() {
            ServerControl.stopServer();
        }
    }

    private void sendRequest(InetAddress host, int port, List<MatcherSample> samples)
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
                    throw new IOException(e.getMessage());
                } else {
                    trials -= 1;
                }
            }
        }

        JSONArray jsonsamples = new JSONArray();
        for (MatcherSample sample : samples) {
            jsonsamples.put(sample.toJSON());
        }

        PrintWriter writer = new PrintWriter(client.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        writer.println(jsonsamples.toString());
        writer.flush();

        String code = reader.readLine();
        assertEquals("SUCCESS", code);

        String response = reader.readLine();
        client.close();

        MatcherKState state =
                new MatcherKState(new JSONObject(response), new MatcherFactory(ServerControl
                        .getServer().getMap()));

        OutputFormatter output = new GeoJSONOutputFormatter();
        PrintWriter out =
                new PrintWriter(ServerTest.class.getResource("").getPath()
                        + "ServerTest-matching.json");
        out.println(output.format(null, state));
        out.close();

        assertEquals(samples.size(), state.sequence().size());
    }

    @Test
    public void TestServer() throws IOException, JSONException, InterruptedException,
            ParseException {
        ServerRunnable serverRunnable = new ServerRunnable();
        Thread serverThread = new Thread(serverRunnable);
        InetAddress host = InetAddress.getLocalHost();
        Properties properties = new Properties();
        properties.load(ServerTest.class.getResource("server.properties").openStream());
        int port = Integer.parseInt(properties.getProperty("server.port"));

        serverThread.start();
        {
            List<MatcherSample> samples = new LinkedList<MatcherSample>();
            String json =
                    new String(Files.readAllBytes(Paths.get(ServerTest.class.getResource(
                            "x0001-015.json").getPath())), Charset.defaultCharset());
            JSONArray jsonsamples = new JSONArray(json);
            for (int i = 0; i < jsonsamples.length(); ++i) {
                samples.add(new MatcherSample(jsonsamples.getJSONObject(i)));
            }
            sendRequest(host, port, samples);
        }
        serverRunnable.stop();
    }
}
