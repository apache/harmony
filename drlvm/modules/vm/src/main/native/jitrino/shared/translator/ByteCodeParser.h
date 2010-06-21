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
 * @author Intel, George A. Timoshenko
 *
 */

#ifndef _BYTECODEPARSER_H_
#define _BYTECODEPARSER_H_

#include <assert.h>
#include "open/types.h"

namespace Jitrino {

class ByteCodeParserCallback {
public:
    virtual ~ByteCodeParserCallback() {}

    // parses one byte code starting at given offset,
    // updates nextOffset to point at next byte code,
    // returns false if parsing should stop
    virtual bool parseByteCode(const U_8* byteCodes,U_32 byteCodeOffset) = 0;
    // called before the parsing starts
    virtual void parseInit() = 0;
    // called after the parsing ends, but not if an error occurs
    virtual void parseDone() = 0;
    // if a callback during its construction determines some error
    // the parsing can be ommitted with help of this:
    virtual bool skipParsing() {return false;};
    // called when an error occurs during the byte code parsing
    virtual void parseError() = 0;
    U_32       getNextOffset() {return nextOffset;}
protected:
    U_32    nextOffset;
};

class ByteCodeParser {
public:
    //
    // creates a new ByteCodeParser
    //
    ByteCodeParser(const U_8* bc, U_32 length)  {
        byteCodes = bc;    byteCodeLength = length; byteCodeOffset = 0;
    }
    //
    // parses the byte code stream, makes calls to the ByteCodeParserCallback
    //
    void parse(ByteCodeParserCallback* cb) {
        cb->parseInit();
        if (!cb->skipParsing()) {
            byteCodeOffset = 0;
            while (byteCodeOffset < byteCodeLength) {
                if (cb->parseByteCode(byteCodes,byteCodeOffset) != true) {
                    return;
                }
                byteCodeOffset = cb->getNextOffset();
            }
        }
        cb->parseDone();
    }

    // Export these values to implement translator optimizations
    const U_8* getByteCodes() {return byteCodes;}
    U_32 getByteCodeLength()  {return byteCodeLength;}

private:
    const U_8* byteCodes;
    U_32       byteCodeLength;
    U_32       byteCodeOffset;
};

//
// byte code parsing utilities
//
#define readU4Le(bytes) (((U_32)(bytes)[3]<<24) | ((U_32)(bytes)[2]<<16) | \
                         ((U_32)(bytes)[1]<<8)  | (bytes)[0])

#define readU4Be(bytes) (((U_32)(bytes)[0]<<24) | ((U_32)(bytes)[1]<<16) | \
                         ((U_32)(bytes)[2]<<8)  | (bytes)[3])

inline I_8     si8(const U_8* bcp)    {return ((I_8*)bcp)[0];}
inline U_8    su8(const U_8* bcp)    {return bcp[0];}
inline float    sr4(const U_8* bcp)    {return ((float*)bcp)[0];} 
inline double   sr8(const U_8* bcp)    {return ((double*)bcp)[0];} 
} //namespace Jitrino 

#endif // _BYTECODEPARSER_H_
