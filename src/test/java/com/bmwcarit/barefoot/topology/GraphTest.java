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

package com.bmwcarit.barefoot.topology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

public class GraphTest {

    @Test
    public void testConstruction() {
        {
            Graph<Edge> graph = new Graph<>();
            graph.add(new Edge(0, 0, 0));
            graph.construct();

            Edge edge = graph.get(0);
            assertEquals(edge.id(), edge.successor().id());
            assertEquals(edge.id(), edge.neighbor().id());
        }
        {
            Graph<Edge> graph = new Graph<>();

            graph.add(new Edge(0, 0, 1));
            graph.add(new Edge(1, 1, 0));
            graph.add(new Edge(2, 1, 2));
            graph.add(new Edge(3, 2, 1));
            graph.add(new Edge(4, 3, 1));
            graph.add(new Edge(6, 4, 0));
            graph.add(new Edge(7, 0, 4));
            graph.add(new Edge(8, 0, 5));

            graph.construct();

            Map<Long, Set<Long>> sources = new HashMap<>();

            sources.put(0L, new HashSet<>(Arrays.asList(0L, 7L, 8L)));
            sources.put(1L, new HashSet<>(Arrays.asList(1L, 2L)));
            sources.put(2L, new HashSet<>(Arrays.asList(3L)));
            sources.put(3L, new HashSet<>(Arrays.asList(4L)));
            sources.put(4L, new HashSet<>(Arrays.asList(6L)));
            sources.put(5L, new HashSet<Long>());

            Iterator<Edge> edges = graph.edges();
            while (edges.hasNext()) {
                Edge edge = edges.next();

                Iterator<Edge> outs = edge.successors();
                int count = 0;

                while (outs.hasNext()) {
                    assertTrue(sources.get(edge.target()).contains(outs.next().id()));
                    count += 1;
                }

                assertEquals(sources.get(edge.target()).size(), count);
            }
        }
    }

    @Test
    public void testComponents() {
        Graph<Edge> graph = new Graph<>();

        // Component with dead-end edge.
        graph.add(new Edge(0, 0, 1));
        graph.add(new Edge(1, 1, 0));
        graph.add(new Edge(2, 1, 2));
        graph.add(new Edge(3, 2, 1));
        graph.add(new Edge(4, 3, 1));
        graph.add(new Edge(6, 4, 0));
        graph.add(new Edge(7, 0, 4));
        graph.add(new Edge(8, 0, 5));

        // Component with circle.
        graph.add(new Edge(9, 6, 7));
        graph.add(new Edge(10, 7, 8));
        graph.add(new Edge(11, 8, 9));
        graph.add(new Edge(12, 9, 6));

        // Component with self-loop edge.
        graph.add(new Edge(13, 10, 10));

        // Component with only dead-end edges.
        graph.add(new Edge(14, 11, 12));
        graph.add(new Edge(15, 11, 13));
        graph.add(new Edge(16, 11, 14));

        graph.add(new Edge(17, 15, 16));
        graph.add(new Edge(18, 16, 17));
        graph.add(new Edge(19, 17, 18));
        graph.add(new Edge(20, 18, 19));
        graph.add(new Edge(21, 19, 20));
        graph.add(new Edge(22, 20, 21));
        graph.add(new Edge(23, 21, 22));

        graph.construct();

        Set<Set<Long>> sets = new HashSet<>();

        sets.add(new HashSet<>(Arrays.asList(0L, 1L, 2L, 3L, 4L, 6L, 7L, 8L)));
        sets.add(new HashSet<>(Arrays.asList(9L, 10L, 11L, 12L)));
        sets.add(new HashSet<>(Arrays.asList(13L)));
        sets.add(new HashSet<>(Arrays.asList(14L, 15L, 16L)));
        sets.add(new HashSet<>(Arrays.asList(17L, 18L, 19L, 20L, 21L, 22L, 23L)));

        Set<Set<Edge>> components = graph.components();

        assertEquals(sets.size(), components.size());
        for (Set<Edge> component : components) {
            Set<Long> set = null;

            for (Set<Long> set_ : sets) {
                if (set_.contains(component.iterator().next().id()))
                    set = set_;
            }

            assertNotNull(set);
            assertEquals(set.size(), component.size());

            for (Edge edge : component) {
                assertTrue(set.contains(edge.id()));
            }
        }
    }
}
