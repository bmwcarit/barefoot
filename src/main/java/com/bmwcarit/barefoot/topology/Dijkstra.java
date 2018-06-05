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

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.util.Quadruple;
import com.bmwcarit.barefoot.util.Tuple;

/**
 * Dijkstra's algorithm implementation of a {@link Router}. The routing functions use the Dijkstra
 * algorithm for finding shortest paths according to a customizable {@link Cost} function.
 *
 * @param <E> Implementation of {@link AbstractEdge} in a directed {@link Graph}.
 * @param <P> {@link Point} type of positions in the network.
 */
public class Dijkstra<E extends AbstractEdge<E>, P extends Point<E>> implements Router<E, P> {
    private static Logger logger = LoggerFactory.getLogger(Dijkstra.class);

    @Override
    public List<E> route(P source, P target, Cost<E> cost) {
        return ssst(source, target, cost, null, null);
    }

    @Override
    public List<E> route(P source, P target, Cost<E> cost, Cost<E> bound, Double max) {
        return ssst(source, target, cost, bound, max);
    }

    @Override
    public Map<P, List<E>> route(P source, Set<P> targets, Cost<E> cost) {
        return ssmt(source, targets, cost, null, null);
    }

    @Override
    public Map<P, List<E>> route(P source, Set<P> targets, Cost<E> cost, Cost<E> bound,
            Double max) {
        return ssmt(source, targets, cost, bound, max);
    }

    @Override
    public Map<P, Tuple<P, List<E>>> route(Set<P> sources, Set<P> targets, Cost<E> cost) {
        return msmt(sources, targets, cost, null, null);
    }

    @Override
    public Map<P, Tuple<P, List<E>>> route(Set<P> sources, Set<P> targets, Cost<E> cost,
            Cost<E> bound, Double max) {
        return msmt(sources, targets, cost, bound, max);
    }

    private List<E> ssst(P source, P target, Cost<E> cost, Cost<E> bound, Double max) {
        return ssmt(source, new HashSet<>(Arrays.asList(target)), cost, bound, max).get(target);
    }

    private Map<P, List<E>> ssmt(P source, Set<P> targets, Cost<E> cost, Cost<E> bound,
            Double max) {
        Map<P, Tuple<P, List<E>>> map =
                msmt(new HashSet<>(Arrays.asList(source)), targets, cost, bound, max);
        Map<P, List<E>> result = new HashMap<>();
        for (Entry<P, Tuple<P, List<E>>> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().two());
        }
        return result;
    }

    private Map<P, Tuple<P, List<E>>> msmt(final Set<P> sources, final Set<P> targets, Cost<E> cost,
            Cost<E> bound, Double max) {

        /*
         * Route mark representation.
         */
        class Mark extends Quadruple<E, E, Double, Double> implements Comparable<Mark> {
            private static final long serialVersionUID = 1L;

            /**
             * Constructor of an entry.
             *
             * @param one {@link AbstractEdge} defining the route mark.
             * @param two Predecessor {@link AbstractEdge}.
             * @param three Cost value to this route mark.
             * @param four Bounding cost value to this route mark.
             */
            public Mark(E one, E two, Double three, Double four) {
                super(one, two, three, four);
            }

            @Override
            public int compareTo(Mark other) {
                return (this.three() < other.three()) ? -1 : (this.three() > other.three()) ? 1 : 0;
            }
        }

        /*
         * Initialize map of edges to target points.
         */
        Map<E, Set<P>> targetEdges = new HashMap<>();
        for (P target : targets) {
            logger.trace("initialize target {} with edge {} and fraction {}", target,
                    target.edge().id(), target.fraction());

            if (!targetEdges.containsKey(target.edge())) {
                targetEdges.put(target.edge(), new HashSet<>(Arrays.asList(target)));
            } else {
                targetEdges.get(target.edge()).add(target);
            }
        }

        /*
         * Setup data structures
         */
        PriorityQueue<Mark> priorities = new PriorityQueue<>();
        Map<E, Mark> entries = new HashMap<>();
        Map<P, Mark> finishs = new HashMap<>();
        Map<Mark, P> reaches = new HashMap<>();
        Map<Mark, P> starts = new HashMap<>();

        /*
         * Initialize map of edges with start points
         */
        for (P source : sources) { // initialize sources as start edges
            double startcost = cost.cost(source.edge(), 1 - source.fraction());
            double startbound =
                    bound != null ? bound.cost(source.edge(), 1 - source.fraction()) : 0.0;

            logger.trace("init source {} with start edge {} and fraction {} with {} cost", source,
                    source.edge().id(), source.fraction(), startcost);

            if (targetEdges.containsKey(source.edge())) { // start edge reaches target edge
                for (P target : targetEdges.get(source.edge())) {
                    if (target.fraction() < source.fraction()) {
                        continue;
                    }
                    double reachcost = startcost - cost.cost(source.edge(), 1 - target.fraction());
                    double reachbound = bound != null
                            ? startcost - bound.cost(source.edge(), 1 - target.fraction())
                            : 0.0;

                    logger.trace("reached target {} with start edge {} from {} to {} with {} cost",
                            target, source.edge().id(), source.fraction(), target.fraction(),
                            reachcost);

                    Mark reach = new Mark(source.edge(), null, reachcost, reachbound);
                    reaches.put(reach, target);
                    starts.put(reach, source);
                    priorities.add(reach);
                }
            }

            Mark start = entries.get(source.edge());
            if (start == null) {
                logger.trace("add source {} with start edge {} and fraction {} with {} cost",
                        source, source.edge().id(), source.fraction(), startcost);

                start = new Mark(source.edge(), null, startcost, startbound);
                entries.put(source.edge(), start);
                starts.put(start, source);
                priorities.add(start);
            } else if (startcost < start.three()) {
                logger.trace("update source {} with start edge {} and fraction {} with {} cost",
                        source, source.edge().id(), source.fraction(), startcost);

                start = new Mark(source.edge(), null, startcost, startbound);
                entries.put(source.edge(), start);
                starts.put(start, source);
                priorities.remove(start);
                priorities.add(start);
            }
        }

        /*
         * Dijkstra algorithm.
         */
        while (priorities.size() > 0) {
            Mark current = priorities.poll();

            if (targetEdges.isEmpty()) {
                logger.trace("finshed all targets");
                break;
            }

            if (max != null && current.four() > max) {
                logger.trace("reached maximum bound");
                break;
            }

            /*
             * Finish target if reached.
             */
            if (reaches.containsKey(current)) {
                P target = reaches.get(current);

                if (finishs.containsKey(target)) {
                    continue;
                } else {
                    logger.trace("finished target {} with edge {} and fraction {} with {} cost",
                            target, current.one(), target.fraction(), current.three());

                    finishs.put(target, current);

                    Set<P> edges = targetEdges.get(current.one());
                    edges.remove(target);

                    if (edges.isEmpty()) {
                        targetEdges.remove(current.one());
                    }
                    continue;
                }
            }

            logger.trace("succeed edge {} with {} cost", current.one().id(), current.three());

            Iterator<E> successors = current.one().successors();

            while (successors.hasNext()) {
                E successor = successors.next();

                double succcost = current.three() + cost.cost(successor);
                double succbound = bound != null ? current.four() + bound.cost(successor) : 0.0;

                if (targetEdges.containsKey(successor)) { // reach target edge
                    for (P target : targetEdges.get(successor)) {
                        double reachcost = succcost - cost.cost(successor, 1 - target.fraction());
                        double reachbound = bound != null
                                ? succbound - bound.cost(successor, 1 - target.fraction())
                                : 0.0;

                        logger.trace(
                                "reached target {} with successor edge {} and fraction {} with {} cost",
                                target, successor.id(), target.fraction(), reachcost);

                        Mark reach = new Mark(successor, current.one(), reachcost, reachbound);
                        reaches.put(reach, target);
                        priorities.add(reach);
                    }
                }

                if (!entries.containsKey(successor)) {
                    logger.trace("added successor edge {} with {} cost", successor.id(), succcost);
                    Mark mark = new Mark(successor, current.one(), succcost, succbound);

                    entries.put(successor, mark);
                    priorities.add(mark);
                }
            }
        }

        Map<P, Tuple<P, List<E>>> paths = new HashMap<>();

        for (P target : targets) {
            if (!finishs.containsKey(target)) {
                paths.put(target, null);
            } else {
                LinkedList<E> path = new LinkedList<>();
                Mark iterator = finishs.get(target);
                Mark start = null;
                while (iterator != null) {
                    path.addFirst(iterator.one());
                    start = iterator;
                    iterator = iterator.two() != null ? entries.get(iterator.two()) : null;
                }
                paths.put(target, new Tuple<P, List<E>>(starts.get(start), path));
            }
        }

        entries.clear();
        finishs.clear();
        reaches.clear();
        priorities.clear();

        return paths;
    }
}
