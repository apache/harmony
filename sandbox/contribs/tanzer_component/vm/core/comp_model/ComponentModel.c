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
#include "ComponentModel.h"
#include "VmComponent.h"

#include <stdlib.h>
#include <stdio.h>
#include <string.h>

#include <sys/types.h>
#include <dlfcn.h>
#include <dirent.h>

/** @todo This only allows one implementation of a component at any time and it
 *        doesn't support loading of arbitrary components, but since this is a
 *        proof-of-concept I guess this is OK so far.
 */

ComponentInfo **components;

void loadComponent(char *compName, void *handle, ComponentInfo *componentInfo) {
	fprintf(stdout, "loadint component: %s\n", compName);
	char *error;
	
	*(void **)(&(componentInfo->getNumDependencies)) = dlsym(handle, "getNumDependencies");
	if((error = dlerror()) != NULL) {
		fprintf(stderr, "Could get function getNumDependencies() from component \"%s\": %s\n", compName, error);
		return;
	}
	*(void **)(&(componentInfo->getDependency)) = dlsym(handle, "getDependency");
	if((error = dlerror()) != NULL) {
		fprintf(stderr, "Could get function getDependency() from component \"%s\": %s\n", compName, error);
		return;
	}
	*(void **)(&(componentInfo->initialize)) = dlsym(handle, "initialize");
	if((error = dlerror()) != NULL) {
		fprintf(stderr, "Could get function initialize() from component \"%s\": %s\n", compName, error);
		return;
	}

	if(components[componentInfo->componentInterfaceId] == NULL) {
		components[componentInfo->componentInterfaceId] = componentInfo;
	} else {
		/* FIXME I guess in a later version we won't have this problem when we use
		 *       a better way for managing / storing the components.
		 */
	}
}

void initializeComponents() {
	int i, j;
	
	/* Ok, I know two-phase construction is generally a bad idea, so if we really
	 * use this component model we should resolve dependencies while initializing
	 * components rather than using this simple two-phase approach.
	 */
	 for(i=0; i<NUM_COMPONENTS; i++) {
		 if(components[i] != NULL) {
			 for(j=0; j<components[i]->getNumDependencies(); j++) {
				 DependencyInfo *dep = components[i]->getDependency(j);
				 if(components[dep->componentInterfaceId] != NULL) {
					 /* TODO check for the component's version and the size of the array too.
					  */
					 dep->setter(components[dep->componentInterfaceId]->component);
				 } else {
					 fprintf(stderr, "ERROR: could not resolve dependency of component %d to component %d\n", i, dep->componentInterfaceId);
				 }
			 }
		 }
	 }
	 for(i=0; i<NUM_COMPONENTS; i++) {
		 if(components[i] != NULL) {
			 components[i]->initialize();
		 }
	 }
}

void componentModelInitialize(char **directories, int num) {
	int i;
	
	fprintf(stdout, "Initializing Component Model\n");
	/* Initialize all entries of the components array with NULL.
	 */
	components = (ComponentInfo **) calloc(NUM_COMPONENTS, sizeof(ComponentInfo *));
	
	/* Load and initialize all components from all specified component directories.
	 */
	for(i=0; i<num; i++) {
		fprintf(stdout, "Loading components from directory %s\n", directories[i]);
		DIR *dir;
		
		dir = opendir(directories[i]);
		if(dir == NULL) {
			/* FIXME report a warning or an error. should we exit the vm here? */
			fprintf(stderr, "Could not open directory %s\n", directories[i]);
		} else {
			unsigned int fd;
			struct dirent *entry;
			
			while((entry = readdir(dir)) != NULL) {
				/* FIXME use OS-given length. */
				char fileName[2048];
				void *handle;
				char *error;
				
				/* FIXME use OS dependent directory separator. */
				sprintf(fileName, "%s/%s", directories[i], entry->d_name);
				handle = dlopen(fileName, RTLD_NOW);
				
				if(handle != NULL) {
					ComponentInfo *(*getComponentInfo)();
					
					dlerror(); /* We should call this function here according to the man page*/
					*(void **)(&getComponentInfo) = dlsym(handle, "getComponentInfo");
					if((error = dlerror()) == NULL) {
						loadComponent(entry->d_name, handle, getComponentInfo());
					} else {
						/* FIXME should this stop the vm? */
						fprintf(stderr, "Could not initialize component \"%s\": %s (component info function not found)\n", entry->d_name, error);
					}
				} else {
					/* FIXME we should add additional logic when dlopen fails to determine if it was
					 *       an error or just something like a subdirectory or a readme file.
					 */
					error = dlerror();
					fprintf(stderr, "Could not initialize component \"%s\": %s (not a dl library)\n", entry->d_name, error);
				}
			}
			
			closedir(dir);
		}
	}
	
	initializeComponents();
}

void *componentModelGetComponent(int interfaceId, int interfaceVersion) {
	/* TODO check for the array size and the component's version.
	 */
	return components[interfaceId]->component;
}
