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
#ifndef __HARMONY_VM_CORE_COMP_MODEL_COMPONENT_MODEL_H_
#define __HARMONY_VM_CORE_COMP_MODEL_COMPONENT_MODEL_H_

/********
 * The component interface IDs for the component interfaces known at compile time. 
 ********/
 
/** TestComponent1 defined in "interfaces/TestComponent1.h".
 * @note Just for testing in the proof-of-concept implementation.
 */
#define TEST_COMPONENT_1 0

/** TestComponent1 defined in "interfaces/TestComponent1.h". 
 * @note Just for testing in the proof-of-concept implementation.
 */
#define TEST_COMPONENT_2 1

#define NUM_COMPONENTS 2

void componentModelInitialize(char **directories, int num);

void *componentModelGetComponent(int interfaceId, int interfaceVersion);

#endif /* __HARMONY_VM_CORE_COMP_MODEL_COMPONENT_MODEL_H_ */
