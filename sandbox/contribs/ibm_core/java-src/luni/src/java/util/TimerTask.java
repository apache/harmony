/* Copyright 2000, 2004 The Apache Software Foundation or its licensors, as applicable
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package java.util;


/**
 * The TimerTask class is represents a task to run at specified time. The task
 * may be run onces or repeatedly.
 * 
 * @see Timer
 * @see java.lang.Object#wait(long)
 */
public abstract class TimerTask implements Runnable {

	/* The timer object which launches this task */
	private Timer timer = null;

	/* If timer was cancelled */
	private boolean cancelled = false;

	/* Slots used by Timer */
	long when;

	long period;

	boolean fixedRate;

	/*
	 * The time when task will be executed, or the time when task was launched
	 * if this is task in progress.
	 */
	private long scheduledTime = 0;

	/*
	 * Method called from the Timer object when scheduling an event
	 * @param time 
	 */
	void setScheduledTime(long time) {
		scheduledTime = time;
	}

	/*
	 * Is TimerTask scheduled into any timer?
	 * 
	 * @return <code>true</code> if the timer task is scheduled,
	 *         <code>false</code> otherwise.
	 */
	boolean isScheduled() {
		return when > 0 || scheduledTime > 0;
	}

	/*
	 * Is TimerTask cancelled?
	 * 
	 * @return <code>true</code> if the timer task is cancelled,
	 *         <code>false</code> otherwise.
	 */
	boolean isCancelled() {
		return cancelled;
	}

	protected TimerTask() {
		super();
	}

	/**
	 * Cancels the Task and removes it from the Timer's queue. Generally, it
	 * returns false if the call did not prevent a TimerTask from running at
	 * least once. Subsequent calls have no effect.
	 * 
	 * @return <code>true</code> if the call prevented a scheduled execution
	 *         from taking place, <code>false</code> otherwise.
	 */
	public boolean cancel() {
		boolean willRun = !cancelled && when > 0;
		cancelled = true;
		return willRun;
	}

	/**
	 * Returns the scheduled execution time. If the task execution is in
	 * progress returns the execution time of ongoing task. Tasks which have not
	 * yet run return an undefined value.
	 * 
	 * @return the most recent execution time.
	 */
	public long scheduledExecutionTime() {
		return scheduledTime;
	}

	/**
	 * The task to run should be specified in the implemenation of the run()
	 * method.
	 */
	public abstract void run();

}
