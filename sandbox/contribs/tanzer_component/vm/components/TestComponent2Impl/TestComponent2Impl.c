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
#include <core/comp_model/VmComponent.h>
#include <core/comp_model/interfaces/TestComponent2.h>

#include <stdlib.h>
#include <stdio.h>

static ComponentInfo *info=NULL;
static TestComponent2 component;

char * getMessage() {
	fprintf(stdout, "TestComponent2 [getMessage]: returning message\n");
	return "A message from TestComponent2";
}

ComponentInfo *getComponentInfo() {
	if(info==NULL) {
		/* We definitely need a logging system. */
		fprintf(stdout, "TestComponent2 [getComponentInfo]: creating new ComponentInfo\n");
		component.getMessage = getMessage;
		
		info = (ComponentInfo *) calloc(1, sizeof(ComponentInfo));
		
		info->componentInterfaceId      = TEST_COMPONENT_2;
		info->componentInterfaceVersion = TEST_COMPONENT_2_INTERFACE_VERSION;
		info->implementationName        = "SimpleTestingImplementation";
		info->implementationVendorName  = "Apache Harmony Project";
		info->implementationVendorUrl   = "http://incubator.apache.org/harmony";
		info->implementationVersion     = 1;
		info->component = &component;
	}
	return info;
}

int getNumDependencies() {
	return 0;
}

DependencyInfo *getDependency(int i) {
	return NULL;
}

void initialize() {
	fprintf(stdout, "TestComponent2 [initialize]: initializing...\n");
}

