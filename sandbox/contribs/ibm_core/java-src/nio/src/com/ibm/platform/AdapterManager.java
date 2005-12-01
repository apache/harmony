/* Copyright 2004 The Apache Software Foundation or its licensors, as applicable
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

package com.ibm.platform;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

/**
 * Adapter Manager
 *  
 */
public final class AdapterManager implements IAdapterManager {

	/*
	 * key is adaptable type, and value is list of adapter factories for that
	 * type.
	 */
	private final HashMap factories = new HashMap();

	public Object getAdapter(IAdaptable adaptable, Class adapterType) {
		List factoryList = (List) factories.get(adaptable);
		if (factoryList != null) {
			for (Iterator factoryItr = factoryList.iterator(); factoryItr
					.hasNext();) {
				IAdapterFactory factory = (IAdapterFactory) factoryItr.next();
				Object adapter = factory.getAdapter(adaptable, adapterType);
				if (adapter != null) {
					return adapter;
				}
			}
		}
		return null;
	}

	public boolean hasAdapter(IAdaptable adaptable, Class adapterType) {
		return null == getAdapter(adaptable, adapterType);
	}

	public void registerAdapters(IAdapterFactory factory, Class adaptable) {
		List factoryList = (List) factories.get(adaptable);
		if (factoryList == null) {
			factoryList = new ArrayList();
			factories.put(adaptable, factoryList);
		}
		factoryList.add(factory);
	}

	public void unregisterAdapters(IAdapterFactory factory, Class adaptable) {
		List factoryList = (List) factories.get(adaptable);
		if (factoryList != null) {
			factoryList.remove(factory);
		}
	}

	public void unregisterAdapters(IAdapterFactory factory) {
		for (Iterator knownAdaptablesItr = factories.keySet().iterator(); knownAdaptablesItr
				.hasNext();) {
			Class adaptable = (Class) knownAdaptablesItr.next();
			unregisterAdapters(factory, adaptable);
		}
	}
}
