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
 * @author Alexander Astapchuk
 */
#include "jframe.h"

/**
 * @file
 * @brief Implementation of JFrame.
 */
namespace Jitrino {
namespace Jet {

void JFrame::push(jtype jt)
{
    assert(m_top<(int)max_size());
    if (jt < i32) {
        jt = i32;
    }
    switch (jt) {
    case i8:
    case i16:
    case u16:
    case i32:
    case jobj:
        m_stack[++m_top] = Val(jt);
        break;
    case i64:
        m_stack[++m_top] = Val(jt);
        m_stack[++m_top] = Val(jt);
        break;
    case flt32:
        m_stack[++m_top] = Val(jt);
        break;
    case dbl64:
        m_stack[++m_top] = Val(jt);
        m_stack[++m_top] = Val(jt);
        break;
    default:
        assert( jt == jretAddr );
        m_stack[++m_top] = Val(jt);
        break;
    }
}

void JFrame::pop(jtype jt)
{
    // cant pop on empty stack
    assert(m_top>=0);
    if (jt < i32) {
        jt = i32;
    }
    assert(top() == jt);
    switch(jt) {
    case i8:
    case i16:
    case u16:
    case i32:
    case jobj:      --m_top;    break;
    case flt32:     --m_top;    break;
    case i64:       m_top -= 2; break;
    case dbl64:     m_top -= 2; break;
    default:
        assert(jt == jretAddr);
        --m_top;
        break;
    }
}

void JFrame::pop2(void)
{
    if (top() == i64 || top() == dbl64) {
        pop(top());
    }
    else {
        pop(top());
        assert(top() != i64 && top() != dbl64);
        pop(top());
    }
}

void JFrame::pop_n(const ::std::vector<jtype>& args)
{
    for (unsigned i=0; i<args.size(); i++) {
        pop(args[(args.size()-i-1)]);
    }
}


};};    // ~namespace Jitrino::Jet
