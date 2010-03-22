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
#ifndef _PACKAGE_H_
#define _PACKAGE_H_

#include "String_Pool.h"
#include <map>

class Package {
public:
    Package(const String *n, const char *jar) : name(n), jar_url(jar) {}
    const String * get_name()   {return name;}
    const char * get_jar()  {return jar_url;}

private:
    const String * name;            // fully qualified name of this package
    const char * jar_url;      // code source url
};

///////////////////////////////////////////////////////////////////////////////
// Table of packages' loaded by the class loader.
///////////////////////////////////////////////////////////////////////////////

class Package_Table : public std::map<const String *, Package * >
{
public:
    inline Package *lookup(const String *name){
        std::map<const String *, Package * >::const_iterator it;
        it = this->find(name);
        if (it != this->end())
            return (*it).second;
        return NULL;
    }
    inline void Insert(Package *p){
        const String *name = p->get_name();
        this->insert(std::make_pair(name, p));
    }
};

#endif // _PACKAGE_H_
