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

#ifndef _PRINTDOTFILE_
#define _PRINTDOTFILE_
//
// interface to print dot files, you should subclass this class and
// implement the printBody() method
//

#include <ostream>

namespace Jitrino {

class MethodDesc;

class PrintDotFile {
public:
    PrintDotFile(): os(NULL), count(0) {}
    //
    // dump out to a filename composed of the method descriptor and a suffix
    //
    virtual void printDotFile(MethodDesc& mh, const char * /*suffix*/);

    virtual void printDotHeader(MethodDesc& mh);
    virtual void printDotEnd   ();
    virtual void printDotBody  ();
protected:
    void printDotFile(MethodDesc& mh, ::std::ostream& fos);
    virtual ~PrintDotFile() {};
    ::std::ostream* os;
    int count;
};
} //namespace Jitrino 

#endif
