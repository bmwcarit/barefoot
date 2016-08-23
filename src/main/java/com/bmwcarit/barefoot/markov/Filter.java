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

package com.bmwcarit.barefoot.markov;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.util.Tuple;

/**
 * Hidden Markov Model (HMM) filter for online and offline inference of states in a stochastic
 * process.
 *
 * @param <C> Candidate inherits from {@link StateCandidate}.
 * @param <T> Transition inherits from {@link StateTransition}.
 * @param <S> Sample inherits from {@link Sample}.
 */
public abstract class Filter<C extends StateCandidate<C, T, S>, T extends StateTransition, S extends Sample> {
    private final static Logger logger = LoggerFactory.getLogger(Filter.class);

    /**
     * Gets state vector, which is a set of {@link StateCandidate} objects and with its emission
     * probability.
     *
     * @param predecessors Predecessor state candidate <i>s<sub>t-1</sub></i>.
     * @param sample Measurement sample.
     * @return Set of tuples consisting of a {@link StateCandidate} and its emission probability.
     */
    protected abstract Set<Tuple<C, Double>> candidates(Set<C> predecessors, S sample);

    /**
     * Gets transition and its transition probability for a pair of {@link StateCandidate}s, which
     * is a candidate <i>s<sub>t</sub></i> and its predecessor <i>s<sub>t</sub></i>
     *
     * @param predecessor Tuple of predecessor state candidate <i>s<sub>t-1</sub></i> and its
     *        respective measurement sample.
     * @param candidate Tuple of state candidate <i>s<sub>t</sub></i> and its respective measurement
     *        sample.
     * @return Tuple consisting of the transition from <i>s<sub>t-1</sub></i> to
     *         <i>s<sub>t</sub></i> and its transition probability, or null if there is no
     *         transition.
     */
    protected abstract Tuple<T, Double> transition(Tuple<S, C> predecessor, Tuple<S, C> candidate);

    /**
     * Gets transitions and its transition probabilities for each pair of state candidates
     * <i>s<sub>t</sub></i> and <i>s<sub>t-1</sub></i>.
     * <p>
     * <b>Note:</b> This method may be overridden for better performance, otherwise it defaults to
     * the method {@link Filter#transition} for each single pair of state candidate and its possible
     * predecessor.
     *
     * @param predecessors Tuple of a set of predecessor state candidate <i>s<sub>t-1</sub></i> and
     *        its respective measurement sample.
     * @param candidates Tuple of a set of state candidate <i>s<sub>t</sub></i> and its respective
     *        measurement sample.
     * @return Maps each predecessor state candidate <i>s<sub>t-1</sub> &#8712; S<sub>t-1</sub></i>
     *         to a map of state candidates <i>s<sub>t</sub> &#8712; S<sub>t</sub></i> containing
     *         all transitions from <i>s<sub>t-1</sub></i> to <i>s<sub>t</sub></i> and its
     *         transition probability, or null if there no transition.
     */
    protected Map<C, Map<C, Tuple<T, Double>>> transitions(Tuple<S, Set<C>> predecessors,
            Tuple<S, Set<C>> candidates) {
        S sample = candidates.one();
        S previous = predecessors.one();

        Map<C, Map<C, Tuple<T, Double>>> map = new HashMap<>();

        for (C predecessor : predecessors.two()) {
            map.put(predecessor, new HashMap<C, Tuple<T, Double>>());

            for (C candidate : candidates.two()) {
                map.get(predecessor).put(candidate, transition(new Tuple<>(previous, predecessor),
                        new Tuple<>(sample, candidate)));
            }
        }

        return map;
    }

    /**
     * Executes Hidden Markov Model (HMM) filter iteration that determines for a given measurement
     * sample <i>z<sub>t</sub></i>, which is a {@link Sample} object, and of a predecessor state
     * vector <i>S<sub>t-1</sub></i>, which is a set of {@link StateCandidate} objects, a state
     * vector <i>S<sub>t</sub></i> with filter and sequence probabilities set.
     * <p>
     * <b>Note:</b> The set of state candidates <i>S<sub>t-1</sub></i> is allowed to be empty. This
     * is either the initial case or an HMM break occured, which is no state candidates representing
     * the measurement sample could be found.
     *
     * @param predecessors State vector <i>S<sub>t-1</sub></i>, which may be empty.
     * @param sample Measurement sample <i>z<sub>t</sub></i>.
     * @param previous Previous measurement sample <i>z<sub>t-1</sub></i>.
     *
     * @return State vector <i>S<sub>t</sub></i>, which may be empty if an HMM break occured.
     */
    public Set<C> execute(Set<C> predecessors, S previous, S sample) {
        if (logger.isTraceEnabled()) {
            try {
                logger.trace("execute sample {}", sample.toJSON().toString());
            } catch (JSONException e) {
                logger.trace("execute sample (not JSON parsable sample: {})", e.getMessage());
            }
        }

        assert (predecessors != null);
        assert (sample != null);

        Set<C> result = new HashSet<>();
        Set<Tuple<C, Double>> candidates = candidates(predecessors, sample);
        logger.trace("{} state candidates", candidates.size());

        double normsum = 0;

        if (!predecessors.isEmpty()) {
            Set<C> states = new HashSet<>();
            for (Tuple<C, Double> candidate : candidates) {
                states.add(candidate.one());
            }

            Map<C, Map<C, Tuple<T, Double>>> transitions =
                    transitions(new Tuple<>(previous, predecessors), new Tuple<>(sample, states));

            for (Tuple<C, Double> candidate : candidates) {
                C candidate_ = candidate.one();
                candidate_.seqprob(Double.NEGATIVE_INFINITY);

                if (logger.isTraceEnabled()) {
                    try {
                        logger.trace("state candidate {} ({}) {}", candidate_.id(), candidate.two(),
                                candidate_.toJSON().toString());
                    } catch (JSONException e) {
                        logger.trace("state candidate (not JSON parsable candidate: {})",
                                e.getMessage());
                    }
                }

                for (C predecessor : predecessors) {
                    Tuple<T, Double> transition = transitions.get(predecessor).get(candidate_);

                    if (transition == null || transition.two() == 0) {
                        continue;
                    }

                    candidate_.filtprob(
                            candidate_.filtprob() + (transition.two() * predecessor.filtprob()));

                    double seqprob = predecessor.seqprob() + Math.log10(transition.two())
                            + Math.log10(candidate.two());

                    if (logger.isTraceEnabled()) {
                        try {
                            logger.trace("state transition {} -> {} ({}, {}, {}) {}",
                                    predecessor.id(), candidate_.id(), predecessor.seqprob(),
                                    Math.log10(transition.two()), Math.log10(candidate.two()),
                                    transition.one().toJSON().toString());
                        } catch (JSONException e) {
                            logger.trace("state transition (not JSON parsable transition: {})",
                                    e.getMessage());
                        }
                    }

                    if (seqprob > candidate_.seqprob()) {
                        candidate_.predecessor(predecessor);
                        candidate_.transition(transition.one());
                        candidate_.seqprob(seqprob);
                    }
                }

                if (candidate_.predecessor() != null) {
                    logger.trace("state candidate {} -> {} ({}, {})", candidate_.predecessor().id(),
                            candidate_.id(), candidate_.filtprob(), candidate_.seqprob());
                } else {
                    logger.trace("state candidate - -> {} ({}, {})", candidate_.id(),
                            candidate_.filtprob(), candidate_.seqprob());
                }


                if (candidate_.filtprob() == 0) {
                    continue;
                }

                candidate_.filtprob(candidate_.filtprob() * candidate.two());
                result.add(candidate_);

                normsum += candidate_.filtprob();
            }
        }

        if (!candidates.isEmpty() && result.isEmpty() && !predecessors.isEmpty()) {
            logger.info("HMM break - no state transitions");
        }

        if (result.isEmpty() || predecessors.isEmpty()) {
            for (Tuple<C, Double> candidate : candidates) {
                if (candidate.two() == 0) {
                    continue;
                }
                C candidate_ = candidate.one();
                normsum += candidate.two();
                candidate_.filtprob(candidate.two());
                candidate_.seqprob(Math.log10(candidate.two()));
                result.add(candidate_);

                if (logger.isTraceEnabled()) {
                    try {
                        logger.trace("state candidate {} ({}) {}", candidate_.id(), candidate.two(),
                                candidate_.toJSON().toString());
                    } catch (JSONException e) {
                        logger.trace("state candidate (not JSON parsable candidate: {})",
                                e.getMessage());
                    }
                }
            }
        }

        if (result.isEmpty()) {
            logger.info("HMM break - no state emissions");
        }

        for (C candidate : result) {
            candidate.filtprob(candidate.filtprob() / normsum);
        }

        logger.trace("{} state candidates for state update", result.size());
        return result;
    }
}
