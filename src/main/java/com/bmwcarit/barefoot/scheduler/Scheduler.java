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

package com.bmwcarit.barefoot.scheduler;

import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Scheduler {
    private final static Logger logger = LoggerFactory.getLogger(Scheduler.class);
    final ArrayList<Worker> workers = new ArrayList<Worker>();
    final AtomicInteger workersup = new AtomicInteger();
    final Queue<Task> queue = new ConcurrentLinkedQueue<Task>();
    final AtomicInteger availTasks = new AtomicInteger();
    final AtomicBoolean stop = new AtomicBoolean();
    final Lock availLock = new ReentrantLock();
    final Condition availCond = availLock.newCondition();
    final long spintime;

    public Scheduler(int numWorkers) {
        this.spintime = (long) 1E9;
        start(numWorkers);
    }

    public Scheduler(int numWorkers, long spintime) {
        this.spintime = spintime;
        start(numWorkers);
    }

    private void start(int numWorkers) {
        this.availTasks.set(0);
        this.workersup.set(0);
        this.stop.set(false);

        for (int i = 0; i < numWorkers; ++i) {
            workers.add(new Worker(i, this));
        }

        for (Worker worker : workers) {
            worker.setDaemon(true);
            worker.start();
        }
        logger.trace("scheduler started with {} workers", numWorkers);
    }

    public Group group() {
        return new Group(this);
    }

    public void shutdown() {
        logger.trace("scheduler shutting donw");
        availTasks.getAndIncrement();
        stop.set(true);

        availLock.lock();
        availCond.signalAll();
        availLock.unlock();
    }

    public Task self() {
        if (workers.contains(Thread.currentThread())) {
            Worker worker = (Worker) Thread.currentThread();
            return worker.current;
        } else {
            return null;
        }
    }
}
