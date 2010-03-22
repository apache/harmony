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
 * @author Intel, Mikhail Y. Fursov
 *
 */

#ifndef _STL_PRIORITY_QUEUE_H_
#define _STL_PRIORITY_QUEUE_H_

#include <Stl.h>
#include <algorithm>

namespace Jitrino {


template <class T, class Sequence = StlVector<T>, class Compare = ::std::less<typename Sequence::value_type> >
class  StlPriorityQueue {
public:
  typedef          StlMMAllocator<T>         Allocator;
  typedef typename Sequence::value_type      value_type;
  typedef typename Sequence::size_type       size_type;
  typedef          Sequence                  container_type;

  typedef typename Sequence::reference       reference;
  typedef typename Sequence::const_reference const_reference;
protected:
  Sequence c;
  Compare comp;
public:
  StlPriorityQueue(Allocator const& a) : c(a) {}

  StlPriorityQueue(Allocator const& a, const Compare& __x) :  c(a), comp(__x) {}

  bool empty() const { return c.empty(); }

  size_type size() const { return c.size(); }

  const_reference top() const { return c.front(); }

  void push(const value_type& __x) {
      c.push_back(__x); 
      ::std::push_heap(c.begin(), c.end(), comp);
  }

  void pop() {
      ::std::pop_heap(c.begin(), c.end(), comp);
      c.pop_back();
  }

  void clear() { c.clear(); }
};

} //namespace Jitrino 

#endif // _STL_PRIORITY_QUEUE_H_
