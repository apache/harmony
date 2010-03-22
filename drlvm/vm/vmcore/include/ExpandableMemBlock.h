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

#ifndef _MEM_BLOCK_H_
#define _MEM_BLOCK_H_

#include "port_malloc.h"

class ExpandableMemBlock
{
public:
    ExpandableMemBlock(long nBlockLen = 2000, long nInc = 1000)
            : m_nBlockLen(nBlockLen), m_nCurPos(0), m_nInc(nInc){
        assert(nInc > 0);
        m_pBlock = STD_MALLOC(m_nBlockLen);
        assert(m_pBlock);
    }
    ~ExpandableMemBlock(){
        if(m_pBlock)
            STD_FREE(m_pBlock);
    }
    void AppendBlock(const char *szBlock, long nLen = -1){
        if(!szBlock)return;
        if(nLen <= 0)nLen = (long) strlen(szBlock);
        if(!nLen)return;
        long nOweSpace = (m_nCurPos + nLen) - m_nBlockLen;
        if(nOweSpace >= 0){ //change > 0 to >= 0, prevents assert in m_free(m_pBlock)
            m_nBlockLen += (nOweSpace / m_nInc + 1)*m_nInc;
            m_pBlock = STD_REALLOC(m_pBlock, m_nBlockLen);
            assert(m_pBlock);
        }
        //memmove((char*)m_pBlock + m_nCurPos, szBlock, nLen);
        memcpy((char*)m_pBlock + m_nCurPos, szBlock, nLen);
        m_nCurPos += nLen;
    }
    void AppendFormatBlock(const char *szfmt, ... ){
        va_list arg;
        //char *buf = (char*)calloc(1024, 1);
        char buf[1024];
        va_start( arg, szfmt );
        vsprintf(buf, szfmt, arg );
        va_end( arg );
        AppendBlock(buf);
        //m_free(buf);
    }
    void SetIncrement(long nInc){
        assert(nInc > 0);
        m_nInc = nInc;
    }
    void SetCurrentPos(long nPos){
        assert((nPos >= 0) && (nPos < m_nBlockLen));
        m_nCurPos = nPos;
    }
    long GetCurrentPos(){
        return m_nCurPos;
    }
    const void *AccessBlock(){
        return m_pBlock;
    }
    const char *toString(){
        *((char*)m_pBlock + m_nCurPos) = '\0';
        return (const char*)m_pBlock;
    }
    void EnsureCapacity(long capacity){
        long nOweSpace = capacity - m_nBlockLen;
        if(nOweSpace >= 0){ //change > 0 to >= 0, prevents assert in m_free(m_pBlock)
            m_nBlockLen += (nOweSpace / m_nInc + 1)*m_nInc;
            m_pBlock = STD_REALLOC(m_pBlock, m_nBlockLen);
            assert(m_pBlock);
        }
    }
    void CopyTo(ExpandableMemBlock &mb, long len = -1){
        if(len == -1)
            len = m_nBlockLen;
        mb.SetCurrentPos(0);
        mb.AppendBlock((char*)m_pBlock, len);
    }
protected:
    void *m_pBlock;
    long m_nBlockLen;
    long m_nCurPos;
    long m_nInc;
};

#endif /* _MEM_BLOCK_H_ */



