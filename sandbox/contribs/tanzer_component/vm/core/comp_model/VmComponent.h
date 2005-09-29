/**
*
* Copyright 2005 The Apache Software Foundation or its licensors, as applicable
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*  See the License for the specific language governing permissions and
*  limitations under the License.
*/

/* Authors:
 * - David Tanzer - struppi@guglhupf.net - http://deltalabs.at
 */
#ifndef __HARMONY_VM_CORE_COMP_MODEL_VM_COMPONENT_H_
#define __HARMONY_VM_CORE_COMP_MODEL_VM_COMPONENT_H_

/* All components will export these functions and provide a ComponentInfo
 * data object.
 */


/** Data structure which identifies a dependency of a component. 
 * The component can only specify an interface ID and version for dependencies. If there are more
 * loaded components which satisfy these requirements, the configuration of the component model
 * determines which dependencies will be injected. 
 */
typedef struct DependencyInfo {
	/** The component interface ID of the dependency which will be injected. 
	 */
	int componentInterfaceId;
	
	/** The component interface version of the dependency which will be injected. 
	 */
	int componentInterfaceVersion;
	
	/** A function provided by the component to set the dependency identified by this data object. 
	 */
	void (*setter)(void *dependency);
} DependencyInfo;

/** Data structure which provides general informations about a component implementation. 
 * @todo Do we need a licence identifier here?
 * @todo We might need an extra field here to prevent cyclic initialization...
 */
typedef struct ComponentInfo {
	/** This ID determines which component interface this component implements. 
	 * This ID is one of the component interface IDs defined in "ComponentModel.h".
	 * If we allow arbitrary components to be loaded (i.e. components where the interface
	 * is not known at compile time) we need a different mechanism for identifying which
	 * component interface the component implements.
	 */
	int componentInterfaceId;
	
	/** This ID determines which version of the component interface is implemented. 
	 */
	int componentInterfaceVersion;
	
	/** A unique name for the implementation of the component. 
	 */
	char *implementationName;
	
	/** The name of the vendor who created this implementation (i.e. "Apache Harmony"). 
	 */
	char *implementationVendorName;
	
	/** The url of the vendor who created this implementation (i.e. "http://incubator.apache.org/harmony/"). 
	 */
	char *implementationVendorUrl;
	
	/** A version identifier for the implementation of the component. 
	 * This version identifier is requiered to be non decreasing, i.e. later versions
	 * have equal or higher implementationVersion values then earlier ones.
	 */
	int implementationVersion;
	
	/** The component object (i.e. the function pointer talbe). 
	 * For example, for TestComponent1 this a pointer to a struct TestComponent1
	 * as defined in "core/comp_model/interfaces/TestComponent1.h".
	 */
	void *component;
	
	/* The following fields will be set by the component model when the component is loaded. */
	int (*getNumDependencies)();
	DependencyInfo * (*getDependency)(int i);
	void (*initialize)();
} ComponentInfo;

/** Get the ComponentInfo object for this component. 
 */
ComponentInfo *getComponentInfo();

/** Get the number of dependencies of this component. 
 */
int getNumDependencies();

/** Get a dependency of this component. 
 */
DependencyInfo *getDependency(int i);

/** Initialize the component (will not be called before all dependencies have been set). 
 */
void initialize();

#endif /* __HARMONY_VM_CORE_COMP_MODEL_VM_COMPONENT_H_ */

