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
#include <core/comp_model/ComponentModel.h>
#include <core/comp_model/VmComponent.h>
#include <core/comp_model/interfaces/TestComponent1.h>

#include <stdio.h>
#include <stdlib.h>

int main(int argc, char **argv) {
	char **directories;
	
	directories = (char **)calloc(2, sizeof(char *));
	directories[0] = "components/TestComponent1Impl/.libs";
	directories[1] = "components/TestComponent2Impl/.libs";
	
	fprintf(stdout, "Starting test case: component model\n");
	componentModelInitialize(directories, 2);
	
	TestComponent1 *comp = (TestComponent1 *)componentModelGetComponent(TEST_COMPONENT_1, 1);
	comp->printMessage();
}
