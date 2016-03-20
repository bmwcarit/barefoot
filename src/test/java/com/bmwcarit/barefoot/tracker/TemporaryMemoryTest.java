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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;

import com.bmwcarit.barefoot.tracker.TemporaryMemory.Factory;
import com.bmwcarit.barefoot.tracker.TemporaryMemory.Publisher;
import com.bmwcarit.barefoot.tracker.TemporaryMemory.TemporaryElement;

public class TemporaryMemoryTest {

    private class Tint extends TemporaryElement<Tint> {
        private int value = 0;

        public Tint(String id) {
            super(id);
        }

        public void set(int value) {
            this.value = value;
        }

        public int get() {
            return this.value;
        }
    }

    @Test
    public void testTemporaryMemory1() throws InterruptedException {
        TemporaryMemory<Tint> memory = new TemporaryMemory<Tint>(new Factory<Tint>() {
            @Override
            public Tint newInstance(String id) {
                return new Tint(id);
            }
        });

        Tint value = memory.getLocked("abc");
        value.set(42);
        value.updateAndUnlock(5);

        Thread.sleep(2 * 1000);
        Tint element = memory.getIfExistsLocked("abc");
        assertNotNull(element);
        assertEquals(42, element.get());
        element.unlock();

        Thread.sleep(4 * 1000);
        assertNull(memory.getIfExistsLocked("abc"));
    }

    @Test
    public void testTemporaryMemory2() throws InterruptedException {
        TemporaryMemory<Tint> memory = new TemporaryMemory<Tint>(new Factory<Tint>() {
            @Override
            public Tint newInstance(String id) {
                return new Tint(id);
            }
        });

        Tint value = memory.getLocked("abc");
        value.set(42);
        value.updateAndUnlock(5);

        Thread.sleep(2 * 1000);
        Tint element = memory.getIfExistsLocked("abc");
        assertNotNull(element);
        assertEquals(42, element.get());
        element.unlock();

        while (!memory.tryDelete("abc"));
        element = memory.getIfExistsLocked("abc");
        assertNull(element);

        Thread.sleep(4 * 1000);
        assertNull(memory.getIfExistsLocked("abc"));
    }

    @Test
    public void testTemporaryMemory3() throws InterruptedException {
        final Map<Integer, Object> deleted = new ConcurrentHashMap<Integer, Object>();
        final int ttl = 3, N = 1000000;
        final TemporaryMemory<Tint> memory = new TemporaryMemory<Tint>(new Factory<Tint>() {
            @Override
            public Tint newInstance(String id) {
                return new Tint(id);
            }
        }, new Publisher<Tint>() {
            @Override
            public void publish(String id, Tint element) {}

            @Override
            public void delete(String id, long time) {
                deleted.put(Integer.parseInt(id), new Object());
            }
        });

        for (int i = 0; i < N; ++i) {
            Tint value = memory.getLocked(Integer.toString(i));
            value.set(i);
            value.updateAndUnlock(ttl);
        }

        Thread runner = new Thread(new Runnable() {
            @Override
            public void run() {
                while (memory.size() > 0) {
                    int i = (int) (Math.random() * N);

                    if (!deleted.containsKey(i)) {
                        Tint value = memory.getLocked(Integer.toString(i));

                        if (value != null) {
                            value.set(value.get() + 1);
                            value.updateAndUnlock(ttl);
                        }
                    }

                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        fail();
                        return;
                    }
                }
            }
        });
        runner.start();
        runner.join(60 * 1000);
        assertFalse(runner.isAlive());
        assertEquals(0, memory.size());
    }
}
