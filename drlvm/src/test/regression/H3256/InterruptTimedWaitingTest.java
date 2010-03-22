/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.apache.harmony.drlvm.tests.regression.h3256;

public class InterruptTimedWaitingTest {

	Object lock = new Object();
	int threadCount = 100;
	int THREAD_WAIT_TIME = 10000;
	int WAIT_CONDITION_TIME = 2000;
	int SLEEP_TIME = 100;
	int loopCountBegin = WAIT_CONDITION_TIME / SLEEP_TIME;
	int loopCount;
	int waitedTime;

	class ThreadWaiting extends Thread {
		volatile boolean exceptionReceived = false;
		volatile boolean working = false;

		public void run () {
			synchronized (lock) {
				this.working = true;
				lock.notify();
			}
			synchronized (this) {
				try {
					this.wait(THREAD_WAIT_TIME);
				} catch (InterruptedException e) {
					exceptionReceived = true;
				}
			}
		}
	}

	public void testInterrupt_Waiting() {
		for (int i = 0; i < threadCount; i++) {
			ThreadWaiting t = new ThreadWaiting();
			try {
				synchronized (lock) {
					t.start();
					while (!t.working) {
						lock.wait();
					}
				}
			} catch (InterruptedException e) {
					e.printStackTrace();
			}
			
			// wait for Thread.State.TIMED_WAITING
			Thread.State ts = t.getState();
			loopCount = loopCountBegin;
			while ((ts != Thread.State.TIMED_WAITING) && (loopCount-- > 0)) {
				ts = t.getState();
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

			// interrupt the thread
			t.interrupt();

			// wait for InteruptedException
			loopCount = loopCountBegin;
			while (!t.exceptionReceived && (loopCount-- > 0)) {
				try {
					Thread.sleep(SLEEP_TIME);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			waitedTime = (loopCountBegin - loopCount) * SLEEP_TIME;
     		System.out.println(i + " exception waited for " + waitedTime + " ms");

			// check for exception received
			if (loopCount < 0) {
				System.out.println(i + " FAILED: waiting thread has not received the InterruptedException");
				System.exit(-1);
			}
			// check for interrupted status cleared
			if (t.isInterrupted()) {
				System.out.println(i + " FAILED: interrupt status has not been cleared");
				System.exit(-2);
			}
		}
	}

	public static void main(String args[]) {
		new InterruptTimedWaitingTest().testInterrupt_Waiting();
	}
}