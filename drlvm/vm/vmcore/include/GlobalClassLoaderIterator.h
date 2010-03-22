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
/** 
 * @author Aleksey Ignatenko
 */  


#ifndef __GlobalClassLoaderIterator__
#define __GlobalClassLoaderIterator__

#include "classloader.h"

class GlobalClassLoaderIterator {
public:
    typedef ClassLoader::ReportedClasses::iterator ClassIterator;
    typedef ClassLoader::ReportedClasses* ReportedClasses;
    // iteration through classloaders
    ClassLoader* first();
    ClassLoader* next();
private:
    unsigned int _loader_index;
};

inline ClassLoader* GlobalClassLoaderIterator::first(){ 
    _loader_index = 0;
    return VM_Global_State::loader_env->bootstrap_class_loader;
}
inline ClassLoader* GlobalClassLoaderIterator::next(){ 
    if(_loader_index >= ClassLoader::m_nextEntry)
        return NULL;
    ClassLoader* cl = ClassLoader::m_table[_loader_index];
    _loader_index++;
    return cl;
}

#endif //__GlobalClassLoaderIterator__

