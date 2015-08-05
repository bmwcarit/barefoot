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

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Group {
	private final static Logger logger = LoggerFactory.getLogger(Group.class);
	private final Scheduler scheduler;
	final Queue<Exception> exceptions = new ConcurrentLinkedQueue<Exception>();
	final Lock syncLock = new ReentrantLock();
	final Condition syncCond = syncLock.newCondition();
	final AtomicInteger syncTasks = new AtomicInteger();

	Group(Scheduler scheduler) {
		this.scheduler = scheduler;
	}

	public void spawn(Task task) {
		if (logger.isTraceEnabled()) {
			logger.trace("group {} spawns task {}", this.toString(),
					task.toString());
		}

		task.group = this;
		syncTasks.getAndIncrement();

		if (scheduler.availTasks.getAndIncrement() == 0) {
			scheduler.availLock.lock();
			logger.trace("scheduler signals workers");
			scheduler.availCond.signalAll();
			scheduler.availLock.unlock();
		}

		scheduler.queue.add(task);
	}

	public boolean sync() {
		logger.trace("group {} waits for tasks", this.toString());
		if (scheduler.workers.contains(Thread.currentThread())) {
			Worker worker = (Worker) Thread.currentThread();

			logger.trace("group {} waits with worker {}", this.toString(), worker.workerid);
			worker.syncCount.getAndIncrement();
			while (syncTasks.get() > 0) {
				worker.execute();
			}
			worker.syncCount.getAndDecrement();
		} else {
			syncLock.lock();
			while (syncTasks.get() > 0) {
				try {
					logger.trace("group {} thread sleeping", this.toString());
					syncCond.await();
					logger.trace("group {} thread signaled", this.toString());
				} catch (InterruptedException e) {
					logger.error("group {} sync interrupted", this.toString());
				}
			}
			syncLock.unlock();
		}
		logger.trace("group {} synchronized", this.toString());

		return exceptions.isEmpty();
	}
}
