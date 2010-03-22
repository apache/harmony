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

#ifndef _EXCEPTIONINFO_H_
#define _EXCEPTIONINFO_H_

namespace Jitrino {

class LabelInst;
class CatchBlock;
class Type;

class ExceptionInfo {
public:
    virtual ~ExceptionInfo() {}

    U_32  getId()            {return id;}
    U_32  getBeginOffset(){return beginOffset;}
    U_32  getEndOffset()    {return endOffset;}
    void    setEndOffset(U_32 offset)    { endOffset = offset; }
    bool    equals(U_32 begin,U_32 end) {
        return (begin == beginOffset && end == endOffset);
    }
    ExceptionInfo*  getNextExceptionInfoAtOffset() {return nextExceptionAtOffset;}
    void            setNextExceptionInfoAtOffset(ExceptionInfo* n) {nextExceptionAtOffset = n;}
    virtual bool isCatchBlock()        {return false;}
    virtual bool isCatchHandler()    {return false;}

    void setLabelInst(LabelInst *lab) { label = lab; }
    LabelInst *getLabelInst()         { return label; }
protected:
    ExceptionInfo(U_32 _id,
                  U_32 _beginOffset,
                  U_32 _endOffset) 
    : id(_id), beginOffset(_beginOffset), endOffset(_endOffset),
      nextExceptionAtOffset(NULL), label(NULL)
    {}
private:
    U_32 id;
    U_32 beginOffset;
    U_32 endOffset;
    ExceptionInfo*    nextExceptionAtOffset;
    LabelInst* label;
};

class CatchHandler : public ExceptionInfo {
public:
    CatchHandler(U_32 id,
                 U_32 beginOffset,
                 U_32 endOffset,
                 Type* excType) 
                 : ExceptionInfo(id, beginOffset, endOffset), 
                 exceptionType(excType), nextHandler(NULL), order(0) {}
    virtual ~CatchHandler() {}

    Type*          getExceptionType()              {return exceptionType;}
    U_32         getExceptionOrder()             {return order;        }
    CatchHandler*  getNextHandler()                {return nextHandler;  }
    void           setNextHandler(CatchHandler* n) {nextHandler=n;       }
    void           setOrder(U_32 ord)            {order = ord;         }
    bool           isCatchHandler()                {return true;         }
private:
    Type*          exceptionType;
    CatchHandler*  nextHandler;
    U_32         order;
};

class CatchBlock : public ExceptionInfo {
public:
    CatchBlock(U_32 id,
               U_32 beginOffset,
               U_32 endOffset,
               U_32 exceptionIndex) 
    : ExceptionInfo(id,beginOffset,endOffset), handlers(NULL), excTableIndex(exceptionIndex) {}
    virtual ~CatchBlock() {}

    bool isCatchBlock()                {return true;}
    U_32 getExcTableIndex() { return excTableIndex; }
    void addHandler(CatchHandler* handler) {
        U_32 order = 0;
        if (handlers == NULL) {
            handlers = handler;
        } else {
            order++;
            CatchHandler *h = handlers;
            for ( ;
                 h->getNextHandler() != NULL;
                 h = h->getNextHandler())
                order++;
            h->setNextHandler(handler);
        }
        handler->setOrder(order);

    }
    bool hasOffset(U_32 offset)
    {
        return (getBeginOffset() <= offset) && (offset < getEndOffset());
    }
    bool offsetSplits(U_32 offset)
    {
        return (getBeginOffset() < offset) && (offset + 1 < getEndOffset());
    }
    CatchHandler*    getHandlers()    {return handlers;}
private:
    CatchHandler* handlers;
    U_32 excTableIndex;
};

} //namespace Jitrino 

#endif // _EXCEPTIONINFO_H_
