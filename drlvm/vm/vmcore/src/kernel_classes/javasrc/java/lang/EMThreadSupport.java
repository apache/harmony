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
package java.lang;

/** 
 * @author Mikhail Y. Fursov
 */  
class EMThreadSupport {

	private static final Object lock = new Object();
	private static boolean active = false;
	private static int timeout = 0;
	private static Thread profilerThread = null;
    static void initialize() {
		boolean needThreadsSuport = needProfilerThreadSupport();
		if (!needThreadsSuport) {
			return;
		}
 		timeout = getTimeout();
		if (timeout < 0) {
			throw new RuntimeException("Illegal timeout value:"+timeout);
		}
		if (timeout == 0) {
			return;
		}
		active=true;
		Runnable emWorker = new Runnable() {
			public void run() {
				EMThreadSupport.run();			
			}
		};
		profilerThread = new Thread(Thread.systemThreadGroup, emWorker, "profiler thread");
		profilerThread.setDaemon(true);
		profilerThread.start();
	}


    static void shutdown() {
		active = false;
		synchronized(lock) {
		    lock.notify();
		}
		try  {
			if(profilerThread != null) {
				profilerThread.join();
			}
		} catch (InterruptedException e) {
		}
	}

	static void run() {
		while(active) {
			onTimeout();
			waitTimeout(timeout);
		}
	}

	private static void waitTimeout(long timeout) {
		try {
			synchronized(lock) {
				lock.wait(timeout);
			}
		} catch (InterruptedException e) {
		} 
	}

	private static native boolean needProfilerThreadSupport();

	private static native void onTimeout();

	private static native int getTimeout();


}
