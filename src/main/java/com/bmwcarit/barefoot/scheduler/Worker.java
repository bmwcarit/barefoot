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
import java.util.Deque;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Worker extends Thread {
    private final static Logger logger = LoggerFactory.getLogger(Worker.class);
    protected final Deque<Task> queue = new ConcurrentLinkedDeque<Task>();
    private final Scheduler scheduler;
    final AtomicLong syncCount = new AtomicLong();
    final int workerid;
    Task current = null;

    Worker(int id, Scheduler scheduler) {
        this.workerid = id;
        this.scheduler = scheduler;
    }

    @Override
    public void run() {
        scheduler.workersup.getAndIncrement();
        while (!scheduler.stop.get()) {
            execute();
        }
        logger.trace("worker {} stopped", workerid);
        scheduler.workersup.getAndDecrement();
    }

    void enqueue(Task task) {
        if (task.parent != current && task != current && task != current.parent) {
            throw new RuntimeException("A task may no spawn to another worker.");
        }

        if (logger.isTraceEnabled()) {
            logger.trace("enqueue task {} to worker {}", task.toString(), workerid);
        }

        if (scheduler.availTasks.getAndIncrement() == 0) {
            scheduler.availLock.lock();
            logger.trace("scheduler signals workers");
            scheduler.availCond.signalAll();
            scheduler.availLock.unlock();
        }

        this.queue.addFirst(task);
    }

    void execute() {
        // If no tasks are available, go to sleep.
        long spinstart = System.nanoTime();
        while (scheduler.availTasks.get() == 0 && syncCount.get() == 0) {
            // ..., but spin for some time before sleeping.
            if (System.nanoTime() - spinstart < scheduler.spintime) {
                continue;
            }

            scheduler.availLock.lock();
            try {
                if (scheduler.availTasks.get() == 0 && syncCount.get() == 0) {
                    logger.trace("worker {} going to sleep", workerid);
                    scheduler.workersup.getAndDecrement();
                    scheduler.availCond.await();
                    scheduler.workersup.getAndIncrement();
                    logger.trace("worker {} got signaled", workerid);
                }
            } catch (InterruptedException e) {
                logger.error("worker waiting for work interrupted");
            }
            scheduler.availLock.unlock();
            break;
        }

        // First, try fetch task from local queue.
        current = this.queue.pollFirst();

        if (current != null && logger.isTraceEnabled()) {
            logger.trace("worker {} fetched task {} from local queue", workerid,
                    current.toString());
        }

        // Second, try fetch task from global queue.
        if (current == null) {
            current = scheduler.queue.poll();

            if (current != null && logger.isTraceEnabled()) {
                logger.trace("worker {} fetched task {} from global queue", workerid,
                        current.toString());
            }
        }

        // Third, try fetch task from remote queue.
        if (current == null) {
            ArrayList<Worker> workers = new ArrayList<Worker>(scheduler.workers);
            workers.remove(workerid);

            // Permute over remote workers.
            while (!workers.isEmpty()) {
                int remoteid = (int) (Math.random() * workers.size());
                current = workers.get(remoteid).queue.pollLast();

                if (current != null) {
                    if (logger.isTraceEnabled()) {
                        logger.trace("worker {} fetched task {} from remote queue {}", workerid,
                                current.toString(), remoteid);
                    }
                    break;
                }

                workers.remove(remoteid);
            }
        }

        // If task is fetched, execute.
        if (current != null) {
            scheduler.availTasks.getAndDecrement();
            current.worker = this;
            current.execute();
        }
    }
}
