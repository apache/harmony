/* Copyright 1998, 2002 The Apache Software Foundation or its licensors, as applicable
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
 * Observer must be implemented by objects which are added to an Observable.
 */
public interface Observer {
	/*
	 * When the specified observable object's <code>notifyObservers</code>
	 * method is called and the observable object has changed, this method is
	 * called.
	 * 
	 * @param observable the observable object @param data the data passed to
	 * <code>notifyObservers</code>
	 */
	void update(Observable observable, Object data);
}
