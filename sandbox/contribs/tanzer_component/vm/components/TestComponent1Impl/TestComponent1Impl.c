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
#include <core/comp_model/interfaces/TestComponent1.h>
#include <core/comp_model/interfaces/TestComponent2.h>

#include <stdlib.h>
#include <stdio.h>

static DependencyInfo *testComp2Dependency=NULL;
static ComponentInfo *info=NULL;
static TestComponent1 component;
static TestComponent2 *testComponent2;

void printMessage(char *message) {
	char *messageOfComp2 = testComponent2->getMessage();
	fprintf(stdout, "TestComponent1 [printMessage]: got message from TestComponent2: \"%s\"\n", messageOfComp2);
}

ComponentInfo *getComponentInfo() {
	if(info==NULL) {
		/* We definitely need a logging system. */
		fprintf(stdout, "TestComponent1 [getComponentInfo]: creating new ComponentInfo\n");
		component.printMessage = printMessage;
		
		info = (ComponentInfo *) calloc(1, sizeof(ComponentInfo));
		
		info->componentInterfaceId      = TEST_COMPONENT_1;
		info->componentInterfaceVersion = TEST_COMPONENT_1_INTERFACE_VERSION;
		info->implementationName        = "SimpleTestingImplementation";
		info->implementationVendorName  = "Apache Harmony Project";
		info->implementationVendorUrl   = "http://incubator.apache.org/harmony";
		info->implementationVersion     = 1;
		info->component = &component;
	}
	return info;
}

int getNumDependencies() {
	return 1;
}

void setTestComponent2(void *dependency) {
	fprintf(stdout, "TestComponent1 [setTestComponent2]: setting dependency\n");
	testComponent2 = (TestComponent2 *) dependency;
}

DependencyInfo *getDependency(int i) {
	if(i != 0) {
		/* TODO report an error or a warning... */
		return NULL;
	}
	
	if(testComp2Dependency == NULL) {
		testComp2Dependency = (DependencyInfo *)calloc(1, sizeof(testComp2Dependency));
		
		testComp2Dependency->componentInterfaceId      = TEST_COMPONENT_2;
		testComp2Dependency->componentInterfaceVersion = TEST_COMPONENT_2_INTERFACE_VERSION;
		testComp2Dependency->setter                    = setTestComponent2;
	}
	fprintf(stdout, "TestComponent1 [getDependency]: returning dependency for TestComponent2\n");
	return testComp2Dependency;
}

void initialize() {
	fprintf(stdout, "TestComponent1 [initialize]: initializing...\n");
}

