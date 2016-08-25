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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * Directed graph providing a basic routing topology to be used by {@link Router} implementations.
 *
 * @param <E> {@link AbstractEdge} type of the graph.
 */
public class Graph<E extends AbstractEdge<E>> implements Serializable {
    private static final long serialVersionUID = 1L;
    protected final HashMap<Long, E> edges = new HashMap<>();

    /**
     * Adds an {@link AbstractEdge} to the graph. (Requires construction.)
     *
     * @param edge Edge to be added.
     * @return Returns a self reference to this graph.
     */
    public Graph<E> add(E edge) {
        edges.put(edge.id(), edge);
        return this;
    }

    /**
     * Removes an {@link AbstractEdge} from the graph. (Requires construction.)
     *
     * @param edge Edge to be removed.
     */
    public void remove(E edge) {
        edges.remove(edge.id());
    }

    /**
     * Gets {@link AbstractEdge} by its identifier.
     *
     * @param id {@link AbstractEdge}'s identifier.
     * @return {@link AbstractEdge} object if it is contained in the graph, otherwise returns null.
     */
    public E get(long id) {
        return edges.get(id);
    }

    /**
     * Gets the size of the graph, i.e. the number of edges.
     *
     * @return Size of the graph, i.e. the number of edges.
     */
    public int size() {
        return edges.size();
    }

    /**
     * Gets an iterator over all edges of the graph.
     *
     * @return Iterator over all edges of the graph.
     */
    public Iterator<E> edges() {
        return edges.values().iterator();
    }

    /**
     * Constructs the graph which means edges are connected for iteration between connections.
     *
     * @return Returns a self reference to this graph.
     */
    public Graph<E> construct() {
        Map<Long, ArrayList<E>> map = new HashMap<>();

        for (E edge : edges.values()) {
            if (!map.containsKey(edge.source())) {
                map.put(edge.source(), new ArrayList<>(Arrays.asList(edge)));
            } else {
                map.get(edge.source()).add(edge);
            }
        }

        for (ArrayList<E> edges : map.values()) {
            for (int i = 1; i < edges.size(); ++i) {
                edges.get(i - 1).neighbor(edges.get(i));
                ArrayList<E> successors = map.get(edges.get(i - 1).target());
                edges.get(i - 1).successor(successors != null ? successors.get(0) : null);
            }

            edges.get(edges.size() - 1).neighbor(edges.get(0));
            ArrayList<E> successors = map.get(edges.get(edges.size() - 1).target());
            edges.get(edges.size() - 1).successor(successors != null ? successors.get(0) : null);
        }

        return this;
    }

    /**
     * Discards the network topology (used for reconstruction of the network topology).
     */
    public void deconstruct() {
        for (E edge : edges.values()) {
            edge.successor(null);
            edge.neighbor(null);
        }
    }

    /**
     * Gets the set of (weakly) connected components of the graph. (A weakly connected component is
     * the set of edges that is connected where directed edges are assumed to be undirected.)
     *
     * @return Set of (weakly) connected components.
     */
    public Set<Set<E>> components() {
        Set<E> unvisited = new HashSet<>(edges.values());
        Map<E, Integer> visited = new HashMap<>();
        Map<Integer, Set<E>> components = new HashMap<>();
        Queue<E> queue = new LinkedList<>();

        int componentcounter = 0;

        while (!unvisited.isEmpty()) {
            Iterator<E> it = unvisited.iterator();
            E edge = it.next();
            it.remove();

            queue.add(edge);

            Set<E> buffer = new HashSet<>();
            int componentid = componentcounter++;

            while (!queue.isEmpty()) {
                edge = queue.poll();
                buffer.add(edge);

                if (visited.containsKey(edge.neighbor())) {
                    componentid = visited.get(edge.neighbor());
                    Set<E> component = components.get(componentid);
                    component.addAll(buffer);
                    buffer = component;
                } else if (unvisited.contains(edge.neighbor())) {
                    unvisited.remove(edge.neighbor());
                    queue.add(edge.neighbor());
                }

                Iterator<E> successors = edge.successors();
                while (successors.hasNext()) {
                    E successor = successors.next();
                    if (visited.containsKey(successor)) {
                        componentid = visited.get(successor);
                        Set<E> component = components.get(componentid);
                        component.addAll(buffer);
                        buffer = component;
                    } else if (unvisited.contains(successor)) {
                        unvisited.remove(successor);
                        queue.add(successor);
                    }
                }
            }

            for (E member : buffer) {
                visited.put(member, componentid);
            }

            if (!components.containsKey(componentid)) {
                components.put(componentid, buffer);
            }
        }
        return new HashSet<>(components.values());
    }
}
