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

import java.util.Calendar;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bmwcarit.barefoot.tracker.TemporaryMemory.TemporaryElement;
import com.bmwcarit.barefoot.util.Tuple;

/**
 * Memory class for storing elements temporarily by defining a time to live (TTL). This class is a
 * work-around solution and will be very likely replaced in future releases.
 */
class TemporaryMemory<E extends TemporaryElement<E>> {
    private final static Logger logger = LoggerFactory.getLogger(TemporaryMemory.class);
    private final Map<String, E> map = new HashMap<>();
    private final Queue<Tuple<Long, E>> queue =
            new PriorityBlockingQueue<>(1, new Comparator<Tuple<Long, E>>() {
                @Override
                public int compare(Tuple<Long, E> left, Tuple<Long, E> right) {
                    return (int) (left.one() - right.one());
                }
            });
    private final Publisher<E> publisher;
    private final Factory<E> factory;
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final Thread cleaner = new Thread(new Runnable() {
        @Override
        public void run() {
            while (!stop.get()) {
                Tuple<Long, E> entry = queue.poll();
                if (entry == null) {
                    try {
                        Thread.sleep(1000 + 1);
                        continue;
                    } catch (InterruptedException e) {
                        logger.warn("cleaner thread sleep interrupted");
                    }
                }

                while (entry.one() > Calendar.getInstance().getTimeInMillis()) {
                    try {
                        long timeout = entry.one() - Calendar.getInstance().getTimeInMillis() + 1;

                        if (timeout > 0) {
                            Thread.sleep(timeout);
                        }
                    } catch (InterruptedException e) {
                        logger.warn("cleaner thread sleep interrupted");
                    }
                }

                tryKill(entry.two());
            }
        }
    });

    public static interface Publisher<E> {
        public abstract void publish(String id, E element);

        public abstract void delete(String id, long time);
    };

    public static abstract class Factory<E> {
        public abstract E newInstance(String id);
    }

    public static abstract class TemporaryElement<E extends TemporaryElement<E>> {
        final Lock lock = new ReentrantLock();
        final String id;
        TemporaryMemory<E> memory;
        long death = 0;

        public TemporaryElement(String id) {
            this.id = id;
        }

        public void updateAndUnlock(int ttl) {
            updateAndUnlock(ttl, true);
        }

        @SuppressWarnings("unchecked")
        public void updateAndUnlock(int ttl, boolean publish) {
            death = Math.max(death + 1, Calendar.getInstance().getTimeInMillis() + ttl * 1000);
            logger.debug("element '{}' updated with ttl {} (death in {})", id, ttl, death);
            memory.queue.add(new Tuple<>(death, (E) this));
            if (publish) {
                memory.publisher.publish(id, (E) this);
            }
            lock.unlock();
        }

        @SuppressWarnings("unchecked")
        public void unlock() {
            lock.unlock();
            memory.tryKill((E) this);
        }
    }

    public TemporaryMemory(Factory<E> factory) {
        this.factory = factory;
        this.publisher = new Publisher<E>() {
            @Override
            public void publish(String id, E element) {
                return;
            }

            @Override
            public void delete(String id, long time) {
                return;
            }
        };
        this.cleaner.start();
    }

    public TemporaryMemory(Factory<E> factory, Publisher<E> publisher) {
        this.factory = factory;
        this.publisher = publisher;
        this.cleaner.start();
    }

    public void stop() {
        stop.set(true);
        try {
            cleaner.join();
        } catch (InterruptedException e) {
            logger.warn("cleaner thread stop interrupted");
        }
    }

    public synchronized int size() {
        return map.size();
    }

    public synchronized E getLocked(String id) {
        E element = map.get(id);
        if (element == null) {
            element = factory.newInstance(id);
            element.memory = this;
            map.put(id, element);
        }
        element.lock.lock();
        return element;
    }

    public synchronized E getIfExistsLocked(String id) {
        E element = map.get(id);
        if (element == null) {
            return null;
        } else {
            element.lock.lock();
            return element;
        }
    }

    public synchronized boolean tryDelete(String id) {
        E element = map.get(id);
        if (element == null) {
            logger.debug("element '{}' is deleted", id);
            return true;
        } else {
            if (!element.lock.tryLock()) {
                return false;
            }
            map.remove(id);
            element.lock.unlock();
            publisher.delete(id, Calendar.getInstance().getTimeInMillis());
            logger.debug("element '{}' deleted", id);
            return true;
        }
    }

    private synchronized void tryKill(E element) {
        element.lock.lock();
        if (element.death <= Calendar.getInstance().getTimeInMillis()) {
            if (map.remove(element.id) != null) {
                publisher.delete(element.id, element.death);
                logger.debug("element '{}' expired and deleted", element.id);
            }
        }
        element.lock.unlock();
    }
}
