/*
 * Copyright (C) 2016, BMW Car IT GmbH
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
package com.bmwcarit.barefoot.tracker;

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
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.matcher.MatcherFactory;
import com.bmwcarit.barefoot.matcher.MatcherKState;
import com.bmwcarit.barefoot.matcher.MatcherSample;
import com.bmwcarit.barefoot.matcher.ServerTest;

public class TrackerServerTest {
    private final static Logger logger = LoggerFactory.getLogger(TrackerServerTest.class);

    private class Server implements Runnable {
        @Override
        public void run() {
            TrackerControl.initServer("config/tracker.properties", "config/oberbayern.properties");
            TrackerControl.runServer();
        }

        public void start() {
            (new Thread(this)).start();
        }

        public void stop() {
            TrackerControl.stopServer();
        }
    }

    public void sendSample(InetAddress host, int port, JSONObject sample)
            throws InterruptedException, IOException {
        int trials = 120;
        int timeout = 500;
        Socket client = null;

        while (client == null || !client.isConnected()) {
            try {
                client = new Socket(host, port);
            } catch (IOException e) {
                Thread.sleep(timeout);

                if (trials == 0) {
                    logger.error(e.getMessage());
                    client.close();
                    throw new IOException();
                } else {
                    trials -= 1;
                }
            }
        }

        PrintWriter writer = new PrintWriter(client.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        writer.println(sample.toString());
        writer.flush();

        String code = reader.readLine();
        assertEquals("SUCCESS", code);
    }

    public MatcherKState requestState(InetAddress host, int port, String id)
            throws JSONException, InterruptedException, IOException {
        int trials = 120;
        int timeout = 500;
        Socket client = null;

        while (client == null || !client.isConnected()) {
            try {
                client = new Socket(host, port);
            } catch (IOException e) {
                Thread.sleep(timeout);

                if (trials == 0) {
                    logger.error(e.getMessage());
                    client.close();
                    throw new IOException();
                } else {
                    trials -= 1;
                }
            }
        }

        JSONObject json = new JSONObject();
        json.put("id", id);

        PrintWriter writer = new PrintWriter(client.getOutputStream());
        BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
        writer.println(json.toString());
        writer.flush();

        String code = reader.readLine();
        assertEquals("SUCCESS", code);

        String response = reader.readLine();
        client.close();

        return new MatcherKState(new JSONObject(response),
                new MatcherFactory(TrackerControl.getServer().getMap()));
    }

    @Test
    public void testTrackerServer() throws IOException, JSONException, InterruptedException {
        Server server = new Server();
        InetAddress host = InetAddress.getLocalHost();
        Properties properties = new Properties();
        properties.load(new FileInputStream("config/tracker.properties"));
        int port = Integer.parseInt(properties.getProperty("server.port"));

        server.start();
        {
            String json = new String(
                    Files.readAllBytes(
                            Paths.get(ServerTest.class.getResource("x0001-015.json").getPath())),
                    Charset.defaultCharset());
            List<MatcherSample> samples = new LinkedList<>();
            JSONArray jsonsamples = new JSONArray(json);
            for (int i = 0; i < jsonsamples.length(); ++i) {
                MatcherSample sample = new MatcherSample(jsonsamples.getJSONObject(i));
                samples.add(sample);
                sendSample(host, port, sample.toJSON());
            }

            String id = new MatcherSample(jsonsamples.getJSONObject(0)).id();
            MatcherKState state = requestState(host, port, id);
            MatcherKState check = TrackerControl.getServer().getMatcher().mmatch(samples, 0, 0);

            assertEquals(check.sequence().size(), state.sequence().size());

            for (int i = 0; i < state.sequence().size(); i++) {
                assertEquals(check.sequence().get(i).point().edge().id(),
                        state.sequence().get(i).point().edge().id());
                assertEquals(check.sequence().get(i).point().fraction(),
                        state.sequence().get(i).point().fraction(), 1E-10);
            }
        }
        server.stop();
    }
}
