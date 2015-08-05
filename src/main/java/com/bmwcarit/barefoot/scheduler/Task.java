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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Task implements Runnable {
	private final static Logger logger = LoggerFactory.getLogger(Task.class);
	protected final AtomicBoolean syncExecute = new AtomicBoolean();
	protected final Queue<Exception> exceptions = new ConcurrentLinkedQueue<Exception>();
	final AtomicInteger syncChildren = new AtomicInteger(1);
	Group group = null;
	Task parent = null;
	Worker worker = null;

	void execute() {
		try {
			run();
		} catch (Exception e) {
			syncExecute.set(false);
			if (parent != null) {
				parent.exceptions.add(e);
			} else if (group != null) {
				group.exceptions.add(e);
			}
		}

		if (syncExecute.get() == true) {
			if (syncChildren.decrementAndGet() == 0) {
				worker.enqueue(this);
				return;
			}
		} else {
			if (group != null) {
				if (group.syncTasks.decrementAndGet() == 0) {
					group.syncLock.lock();
					logger.trace("task {} signals group {}", this.toString(),
							group.toString());
					group.syncCond.signalAll();
					group.syncLock.unlock();
				}

				if (logger.isTraceEnabled()) {
					logger.trace(
							"task {} on worker {} decrements group {} task counter",
							this.toString(), worker.workerid, group.toString());
				}
			}

			if (parent != null) {
				if (parent.syncChildren.decrementAndGet() == 0) {
					logger.trace(
							"task {} on worker {} spawns sync execute of parent {}",
							this.toString(), worker.workerid, parent.toString());
					worker.enqueue(parent);
				}

				if (logger.isTraceEnabled()) {
					logger.trace(
							"task {} on worker {} decrements parent {} children counter",
							this.toString(), worker.workerid, parent.toString());
				}
			}
		}
	}

	protected void spawn(Task task) {
		if (logger.isTraceEnabled()) {
			logger.trace("task {} spawns child {}", this.toString(),
					task.toString());
		}

		task.parent = this;
		task.group = this.group;
		syncChildren.getAndIncrement();
		group.syncTasks.getAndIncrement();
		worker.enqueue(task);
	}

	protected boolean sync() {
		logger.trace("task {} waits for children", this.toString());
		worker.syncCount.getAndIncrement();
		while (syncChildren.get() > 1) {
			worker.execute();
		}
		worker.syncCount.getAndDecrement();
		worker.current = this;
		logger.trace("task {} synchronized", this.toString());

		return exceptions.isEmpty();
	}
}
