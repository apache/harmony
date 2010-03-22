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

#include <iostream>
#include <streambuf>

#include "PrintDotFile.h"
#include "Inst.h"
#include "optimizer.h"
#include "Log.h"


namespace Jitrino {

//
// For dot files, certain characters need to be escaped in certain situations. dotbuf and dotstream do the 
// appropriate filtering.
//

typedef int int_type;

class dotbuf : public ::std::streambuf
{
public:
    dotbuf(::std::streambuf* sb) :
      inquotes(false), bracedepth(0), m_sb(sb) {}
private:
    // copy ctor to make msvc compiler happy
    dotbuf(dotbuf& sb){}

protected:
    int_type overflow(int_type c) {
        switch(c) {
        case '"':
            inquotes = !inquotes;
            break;
        case '{':
            if(inquotes) {
                if(bracedepth > 0)
                    m_sb->sputc('\\');
                ++bracedepth;
            }
            break;
        case '}':
            if(inquotes) {
                --bracedepth;
                if(bracedepth > 0)
                    m_sb->sputc('\\');
            }
            break;
        }
        return m_sb->sputc((char)c);
    }

    int_type underflow() {
        return m_sb->sgetc();
    }

private:
    bool inquotes;
    int bracedepth;
    ::std::streambuf* m_sb;
};

class dotstream : public ::std::ostream
{
public:
    dotstream(::std::ostream& out) :
      ::std::ostream(&m_buf), m_buf(out.rdbuf()) {}

private:
    // copy ctor to make msvc compiler happy
    dotstream(dotstream& sb) : ::std::ostream(NULL), m_buf(NULL) {}

    dotbuf m_buf;
};


void PrintDotFile::printDotFile(MethodDesc& mh, const char * suffix) {
    if (Log::isLogEnabled(LogStream::DOTDUMP)) {
        if (suffix != 0) {
            char* fname = Log::makeDotFileName(suffix);

            LogStream logs(fname);
            printDotFile(mh, logs.out());

            delete [] fname;
        }
        else {
            printDotFile(mh, Log::log(LogStream::DOTDUMP).out());
        }
    }
}

void PrintDotFile::printDotFile(MethodDesc& mh, ::std::ostream& fos) {
    dotstream dos(fos);
    os = &dos;
    printDotHeader(mh);
    printDotBody();
    printDotEnd();
}

void PrintDotFile::printDotHeader(MethodDesc& mh) {
    *os << "digraph dotgraph {" << ::std::endl
        << "node [shape=record,fontname=\"Courier\",fontsize=9];" << ::std::endl
        << "label=\""
        << mh.getParentType()->getName()
        << "::"
        << mh.getName()
        << "\";" << ::std::endl;
}

void PrintDotFile::printDotBody() {}

void PrintDotFile::printDotEnd() {
    *os << "}" << ::std::endl;
}

} //namespace Jitrino 
