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

#ifndef _STL_H_
#define _STL_H_

/**
 * This file provides MemoryManager-based STL containers.  
 **/

#include <assert.h>
#include <stddef.h>

#ifndef PLATFORM_POSIX
// Turnoff gratuitous VC++ warnings on template expanded identifier lengths.
#pragma warning(disable : 4786)
#endif

#include <deque>

#ifdef PLATFORM_POSIX

#include <ext/hash_set>
#include <ext/hash_map>
#if (defined __GNUC__) && ((__GNUC__ >= 4 && __GNUC_MINOR__ > 2))
#include <backward/hash_fun.h>
#else
#if (defined __GNUC__) && ((__GNUC__ >= 3 && __GNUC_MINOR__ > 3) || __GNUC__ > 3)
#include <ext/hash_fun.h>
#else
#include <ext/stl_hash_fun.h>
#endif
#endif


#else

#undef _STDEXT_BEGIN
#define _STDEXT_BEGIN namespace stdext {
#undef _STDEXT_END
#define _STDEXT_END }
#undef _STDEXT
#define _STDEXT stdext::

#include <hash_set>
#include <hash_map>

#endif

#include <list>
#include <map>
#include <set>
#include <vector>
#include <memory>
#include <algorithm>

#include "MemoryManager.h"

namespace Jitrino {

/**
 * A MemoryManager based STL Allocator.  This class is a light-weight wrapper 
 * around MemoryManager.  Any STL containers using this allocator class will 
 * require and use a MemoryManager for allocation of private data.
 **/

template <class T>
class StlMMAllocator
{
 public:
  // Standard constructor from MemoryManager.
  StlMMAllocator(MemoryManager& mm) : pmm(&mm) {}

  // Type-parameterized copy constructor.
  template <class U> StlMMAllocator(const StlMMAllocator<U>& allocator) : 
    pmm(&allocator.getMemoryManager()) {}

  // Destructor.
  ~StlMMAllocator() {}

  // Assignment operator - new underlying MemoryManager.
  void operator=(const StlMMAllocator& allocator) { pmm = allocator.pmm; } 

  // Underlying pointer, reference, etc types for this allocator.
  typedef T* pointer;
  typedef const T* const_pointer;
  typedef T& reference;
  typedef const T& const_reference;
  typedef T value_type;
  typedef size_t size_type;
  typedef ptrdiff_t difference_type;

  // Pointer/reference conversion.
  pointer address(reference x) const { return &x; }
  const_pointer address(const_reference x) const { return &x; }

  // Allocation/deallocation operations.
  pointer allocate(size_type n, const void* = 0)
    { pointer p = (pointer) pmm->alloc(n * sizeof(T)); assert(n==0||p!=NULL); return p; }
  void deallocate(void *p, size_type) {}

  char *_Charalloc(size_t size) { return (char*)allocate(size); }

  // Maximum allocatable size based upon size_type.
  size_type max_size() const { return ((size_type) -1) / sizeof(value_type); }

  // Initialization/finalization operations.
  void construct(pointer p, const value_type& x) { new (p) value_type(x); }
  void destroy(pointer p) { p->~value_type(); }

  // Allocator equality tests
  template <class U>
  bool operator==(const StlMMAllocator<U>& allocator) const { return pmm == allocator.pmm; }
  template <class U>
  bool operator!=(const StlMMAllocator<U>& allocator) const { return pmm != allocator.pmm; }

  // Type conversion utility to obtain StlMMAllocator for different underlying type.
  template <class U> struct rebind { typedef StlMMAllocator<U> other; };


  // Underlying MemoryManager.
  MemoryManager& getMemoryManager() const { return *pmm; }

 private:
  // Disable.  An StlMMAllocator cannot be instantiated without a MemoryManager.
  StlMMAllocator();

  MemoryManager* pmm;
};

/**
 * A specialization of the above for void types.  
 **/
template <>
class StlMMAllocator<void>
{
  typedef void value_type;
  typedef void* pointer;
  typedef const void* const_pointer;

  template <class U> struct rebind { typedef StlMMAllocator<U> other; };
};

#if defined (__SGI_STL_PORT) && !defined (_STLP_MEMBER_TEMPLATE_CLASSES)
__STL_BEGIN_NAMESPACE
// Work-arounds used by STLport for VC++ 6.0, other compilers with substandard
// template support.  

template <typename T, typename U>
inline StlMMAllocator<U>&
__stl_alloc_rebind(StlMMAllocator<T>& a, U const *)
{
  return (StlMMAllocator<U>&) a;
}

template <typename T, typename U>
inline StlMMAllocator<U>&
__stl_alloc_create(StlMMAllocator<T>& a, U const *)
{
  return StlMMAllocator<U>();
}

__STL_END_NAMESPACE
#endif
/**
 * A MemoryManager-based STL doubly-linked list container.
 **/
template<class T, class Allocator = StlMMAllocator<T> >
class StlList : public ::std::list<T, Allocator>
{
  typedef ::std::list<T, Allocator> List;
public:
  StlList(Allocator const& a) : List(a) {}
};


/**
 * A MemoryManager-based STL vector container.
 **/
template<class T, class Allocator = StlMMAllocator<T> >
class StlVector : public ::std::vector<T, Allocator>
{
typedef ::std::vector<T, Allocator> Vector;
public:
  typedef typename Vector::size_type size_type;

  StlVector(Allocator const& a) : Vector(a) {}
  StlVector(Allocator const& a, size_type n, const T& x = T()) : Vector(n, x, a) {}
};

/**
 * A MemoryManager-based STL sorted vector container to use as a set.
 **/
template<class T, class Allocator = StlMMAllocator<T> >
class StlVectorSet : public ::std::vector<T, Allocator>
{
  typedef ::std::vector<T, Allocator> Vector;

  typedef typename Vector::size_type size_type;

public:

#ifdef PLATFORM_POSIX
  typedef typename Vector::iterator iterator;
  typedef typename Vector::const_iterator const_iterator;
#endif

  StlVectorSet(Allocator const& a) : Vector(a) {}
  StlVectorSet(Allocator const& a, size_type n, const T& x = T()) : Vector(n, x, a) {}
  StlVectorSet(const Vector & a) :  Vector(a) {
      sort(StlVectorSet<T,Allocator>::begin(), StlVectorSet<T,Allocator>::end());
  }
  StlVectorSet& operator=(const Vector & a) { Vector::operator=(a); return *this; };
  StlVectorSet& operator=(const StlVectorSet & a) { 
      Vector::operator=(a); 
      return *this; 
  };
  ::std::pair<iterator, bool> insert(const T& x) {
      ::std::pair<iterator, iterator> found = equal_range(x);
      bool res = false;
      if (found.first == found.second) {
          Vector::insert(found.second, x);
          found = equal_range(x);
          res = true;
      }
      return ::std::pair<iterator,bool>(found.first, res);
  };
  void insert(iterator pos, const T& x) {
      ::std::pair<iterator, iterator> found = equal_range(x);
      if (found.first == found.second) {
          Vector::insert(found.second, x);
      }
  }
  void insert(iterator i1, iterator i2) {
      Vector::insert(StlVectorSet<T>::end(), i1, i2);
      ::std::sort(StlVectorSet<T>::begin(), StlVectorSet<T>::end());
  }
  size_type erase(const T& x) { 
      ::std::pair<iterator, iterator> found= equal_range(x);
      size_type delta = found.first - found.second;
      if (delta == 0) return 0;
      else
          Vector::erase(found.first, found.second);
      return delta;
  };
  void erase(iterator __position) { Vector::erase(__position); };
  void erase(iterator __first, iterator __last) {
      Vector::erase(__first, __last);
  }
  void clear() { Vector::clear(); };
  size_type count(const T& x) const { 
      ::std::pair<const_iterator, const_iterator> found= equal_range(x);
      if (found.first != found.second) return 1;
      else return 0;
  }
    const_iterator lower_bound(const T& x) const {
      return ::std::lower_bound(StlVectorSet<T,Allocator>::begin(), StlVectorSet<T,Allocator>::end(), x);
  }
  iterator lower_bound(const T& x) {
      return ::std::lower_bound(StlVectorSet<T,Allocator>::begin(), StlVectorSet<T,Allocator>::end(), x);
  }
  const_iterator upper_bound(const T& x) const {
      return ::std::upper_bound(StlVectorSet<T,Allocator>::begin(), StlVectorSet<T,Allocator>::end(), x);
  }
  iterator upper_bound(const T& x) {
      return ::std::upper_bound(StlVectorSet<T,Allocator>::begin(), StlVectorSet<T,Allocator>::end(), x);
  }
  ::std::pair<const_iterator, const_iterator> equal_range(const T& x) const {
      return ::std::equal_range(StlVectorSet<T,Allocator>::begin(), StlVectorSet<T,Allocator>::end(), x);
  }
  ::std::pair<iterator, iterator> equal_range(const T& x) {
      return ::std::equal_range(StlVectorSet<T,Allocator>::begin(), StlVectorSet<T,Allocator>::end(), x);
  }
  const_iterator find(const T& x) const {
      ::std::pair<const_iterator, const_iterator> found= equal_range(x);
      if (found.first == found.second) return StlVectorSet<T,Allocator>::end();
      else return found.first;
  }
  iterator find(const T& x) { 
      ::std::pair<iterator, iterator> found= equal_range(x);
      if (found.first == found.second) return StlVectorSet<T,Allocator>::end();
      else return found.first;
  }
  bool has(const T& x) const { 
      ::std::pair<const_iterator, const_iterator> found= equal_range(x);
      return (found.first != found.second);
  };
};

/**
 * A MemoryManager-based STL vector<bool> container.  
 * vector<bool> uses an efficient bit representation underneath.  However, not all 
 * generic vector methods will work correctly.
 **/
template<class Allocator = StlMMAllocator<bool> >
class StlBoolVector : public ::std::vector<bool, Allocator>
{
  typedef ::std::vector<bool, Allocator> Vector;
  typedef typename Vector::size_type size_type;
public:
  StlBoolVector(Allocator const& a) : Vector(a) {}
  StlBoolVector(Allocator const& a, size_type n, bool x = false) : Vector(n, x, a) {}

  // Automatically resize as needed.
  bool getBit(size_type n) { if(n < StlBoolVector<Allocator>::size()) return at(n); else return false; }
  bool setBit(size_type n, bool value=true) { if(n >= StlBoolVector<Allocator>::size()) resize(n+1); bool old = at(n); at(n) = value; return old; }
};
typedef StlBoolVector<> StlBitVector;

/**
 * A MemoryManager-based STL deque container.
 **/
template<class T, class Allocator = StlMMAllocator<T> >
class StlDeque : public ::std::deque<T, Allocator>
{
  typedef ::std::deque<T, Allocator> Deque;
public:
  StlDeque(Allocator const& a) : Deque(a) {}
};

/**
 * A MemoryManager-based STL set container.
 **/
template<class KeyT, class Traits = ::std::less<KeyT>, class Allocator = StlMMAllocator<KeyT> >
class StlSet : public ::std::set<KeyT, Traits, Allocator>
{
  typedef ::std::set<KeyT, Traits, Allocator> Set;
public:
  StlSet(Allocator const& a) : Set(Traits(), a) {}
  StlSet(Traits const& t, Allocator const& a) : Set(t, a) {}

  bool has(const KeyT& k) const { return (find(k) != StlSet<KeyT,Traits,Allocator>::end()); };
};

/**
 * A MemoryManager-based STL multiset container.
 **/
template<class KeyT, class Traits = ::std::less<KeyT>, class Allocator = StlMMAllocator<KeyT> >
class StlMultiSet : public ::std::multiset<KeyT, Traits, Allocator>
{
  typedef ::std::multiset<KeyT, Traits, Allocator> MultiSet;
public:
  StlMultiSet(Allocator const& a) : MultiSet(Traits(), a) {}
  StlMultiSet(Traits const& t, Allocator const& a) : MultiSet(t, a) {}

  bool has(const KeyT& k) const { return (find(k) != StlMultiSet<KeyT>::end()); };  
};

/**
 * A MemoryManager-based STL map container.
 **/
template<class KeyT, class ValueT, class Traits = ::std::less<KeyT>, class Allocator = StlMMAllocator<std::pair<const KeyT, ValueT> > >
class StlMap : public ::std::map<KeyT, ValueT, Traits, Allocator>
{
  typedef ::std::map<KeyT, ValueT, Traits, Allocator> Map;
public:
  StlMap(Allocator const& a) : Map(Traits(), a) {}
  StlMap(Traits const& t, Allocator const& a) : Map(t, a) {}

  bool has(const KeyT& k) const { return (find(k) != StlMap<KeyT,ValueT,Traits,Allocator>::end()); };
};

/**
 * A MemoryManager-based STL multimap container.
 **/
template<class KeyT, class ValueT, class Traits = ::std::less<KeyT>, class Allocator = StlMMAllocator<std::pair<const KeyT, ValueT> > >
class StlMultiMap : public ::std::multimap<KeyT, ValueT, Traits, Allocator>
{
  typedef ::std::multimap<KeyT, ValueT, Traits, Allocator> MultiMap;
public:
  StlMultiMap(Allocator const& a) : MultiMap(Traits(), a) {}
  StlMultiMap(Traits const& t, Allocator const& a) : MultiMap(t, a) {}

  bool has(const KeyT& k) const { return (find(k) != StlMultiMap<KeyT,ValueT,Traits,Allocator>::end()); };
};

/**
 * A simple hash function.  We use this as the default as it covers more cases 
 * (e.g., pointers) that stdext::hash<T> does not.  Note, for char* or ::std::string use 
 * stdext::hash<T> to get string equality instead of pointer equality.
 **/
struct StlSimpleHash
{
    // For primitive types, just return the value.
    size_t operator() (size_t __x) const { return (size_t) __x; }

    // For pointers, shift away the last bits which should always be zero.
    size_t operator() (void* __x) const { return (size_t) __x >> 3; }
};
#if !defined (__SGI_STL_PORT)
#if defined (PLATFORM_POSIX)
template<class T>
class StlHash : public __gnu_cxx::hash<T> {};
#else
template<class T>
class StlHash : public stdext::hash_compare<T> {};
#endif
#else
template<class T>
class StlHash : public ::std::hash<T> {};
#endif


/**
 * A MemoryManager-based STL hash_set container.
 **/
#if !defined (__SGI_STL_PORT)
template<class T, class HashCompareFun = ::std::less<T>, class Allocator = StlMMAllocator<T> >
class StlHashSet : public ::std::set<T, HashCompareFun, Allocator>
{
  typedef ::std::set<T, HashCompareFun, Allocator> HashSet;
  typedef typename HashSet::size_type size_type;
public:
  StlHashSet(Allocator const& a) : HashSet(HashCompareFun(), a) {}
  StlHashSet(Allocator const& a, size_type n) : HashSet(HashCompareFun(), a) {}

  bool has(const T& k) const { return (find(k) != StlHashSet<T,HashCompareFun,Allocator>::end()); };
};
#else
template<class T, class HashFun = StlSimpleHash, class CompareFun = ::std::equal_to<T>, class Allocator = StlMMAllocator<T> >
class StlHashSet : public ::std::hash_set<T, HashFun, CompareFun, Allocator>
{
  typedef ::std::hash_set<T, HashFun, CompareFun, Allocator> HashSet;
  typedef typename Vector::size_type size_type;
public:
  StlHashSet(Allocator const& a) : HashSet(100, HashFun(), CompareFun(), a) {}
  StlHashSet(Allocator const& a, size_type n) : HashSet(n, HashFun(), CompareFun(), a) {}
  StlHashSet(Allocator const& a, size_type n, HashFun const& h) : HashSet(n, h, CompareFun(), a) {}
  StlHashSet(Allocator const& a, size_type n, HashFun const& h, CompareFun const& c) : HashSet(n, h, c, a) {}

  bool has(const T& k) const { return (find(k) != StlHashSet<T,HashCompareFun,Allocator>::end()); };
};
#endif

/**
 * A MemoryManager-based STL hash_multiset container.
 **/
#if !defined (__SGI_STL_PORT)
template<class T, class HashCompareFun = ::std::less<T>, class Allocator = StlMMAllocator<T> >
class StlHashMultiSet : public ::std::multiset<T, HashCompareFun, Allocator>
{
  typedef ::std::multiset<T, HashCompareFun, Allocator> HashMultiSet;
  typedef typename HashMultiSet::size_type size_type;
public:
  StlHashMultiSet(Allocator const& a) : HashMultiSet(HashCompareFun(), a) {}
  StlHashMultiSet(Allocator const& a, size_type n) : HashMultiSet(HashCompareFun(), a) {}

  bool has(const T& k) const { return (find(k) != StlHashMultiSet<T,HashCompareFun,Allocator>::end()); };
};
#else
template<class T, class HashFun = StlSimpleHash, class CompareFun = ::std::equal_to<T>, class Allocator = StlMMAllocator<T> >
class StlHashMultiSet : public ::std::hash_multiset<T, HashFun, CompareFun, Allocator>
{
  typedef ::std::hash_multiset<T, HashFun, CompareFun, Allocator> HashMultiSet;
  typedef typename Vector::size_type size_type;
public:
  StlHashMultiSet(Allocator const& a) : HashMultiSet(100, HashFun(), CompareFun(), a) {}
  StlHashMultiSet(Allocator const& a, size_type n) : HashMultiSet(n, HashFun(), CompareFun(), a) {}
  StlHashMultiSet(Allocator const& a, size_type n, HashFun const& h) : HashMultiSet(n, h, CompareFun(), a) {}
  StlHashMultiSet(Allocator const& a, size_type n, HashFun const& h, CompareFun const& c) : HashMultiSet(n, h, c, a) {}

  bool has(const T& k) const { return (find(k) != StlHashMultiSet<T,HashCompareFun,Allocator>::end()); };
};
#endif

/**
 * A MemoryManager-based STL hash_map container.
 **/
#if !defined (__SGI_STL_PORT)
template<class KeyT, class ValueT, class HashCompareFun = ::std::less<KeyT>, class Allocator = StlMMAllocator<std::pair<const KeyT, ValueT> > >
class StlHashMap : public ::std::map<KeyT, ValueT, HashCompareFun, Allocator>
{
  typedef ::std::map<KeyT, ValueT, HashCompareFun, Allocator> HashMap;
  typedef typename HashMap::size_type size_type;
public:
  StlHashMap(Allocator const& a) : HashMap(HashCompareFun(), a) {}
  StlHashMap(Allocator const& a, size_type n) : HashMap(HashCompareFun(), a) {}

  bool has(const KeyT& k) const { return (find(k) != StlHashMap<KeyT,ValueT,HashCompareFun,Allocator>::end()); };
};

#else
template<class KeyT, class ValueT, class HashFun = StlSimpleHash, class CompareFun = ::std::equal_to<KeyT>, class Allocator = StlMMAllocator<std::pair<const KeyT, ValueT> > >
class StlHashMap : public ::std::hash_map<KeyT, ValueT, HashFun, CompareFun, Allocator>
{
  typedef ::std::hash_map<KeyT, ValueT, HashFun, CompareFun, Allocator> HashMap;
  typedef typename Vector::size_type size_type;
public:
  StlHashMap(Allocator const& a) : HashMap(100, HashFun(), CompareFun(), a) {}
  StlHashMap(Allocator const& a, size_type n) : HashMap(n, HashFun(), CompareFun(), a) {}
  StlHashMap(Allocator const& a, size_type n, HashFun const& h) : HashMap(n, h, CompareFun(), a) {}
  StlHashMap(Allocator const& a, size_type n, HashFun const& h, CompareFun const& c) : HashMap(n, h, c, a) {}

  bool has(const KeyT& k) const { return (find(k) != StlHashMap<KeyT,ValueT,HashCompareFun,Allocator>::end()); };
};
#endif

/**
 * A MemoryManager-based STL hash_map container.
 **/
#if !defined (__SGI_STL_PORT)
template<class KeyT, class ValueT, class HashCompareFun = ::std::less<KeyT>, class Allocator = StlMMAllocator<std::pair<const KeyT, ValueT> > >
class StlHashMultiMap : public ::std::multimap<KeyT, ValueT, HashCompareFun, Allocator>
{
  typedef ::std::multimap<KeyT, ValueT, HashCompareFun, Allocator> HashMultiMap;
  typedef typename HashMultiMap::size_type size_type;
public:
  StlHashMultiMap(Allocator const& a) : HashMultiMap(HashCompareFun(), a) {}
  StlHashMultiMap(Allocator const& a, size_type n) : HashMultiMap(HashCompareFun(), a) {}

  bool has(const KeyT& k) const { return (find(k) != StlHashMultiMap<KeyT,ValueT,HashCompareFun,Allocator>::end()); };
};
#else
template<class KeyT, class ValueT, class HashFun = StlSimpleHash, class CompareFun = ::std::equal_to<KeyT>, class Allocator = StlMMAllocator<std::pair<const KeyT, ValueT> > >
class StlHashMultiMap : public ::std::hash_multimap<KeyT, ValueT, HashFun, CompareFun, Allocator>
{
  typedef ::std::hash_multimap<KeyT, ValueT, HashFun, CompareFun, Allocator> HashMultiMap;
  typedef typename Vector::size_type size_type;
public:
  StlHashMultiMap(Allocator const& a) : HashMultiMap(100, HashFun(), CompareFun(), a) {}
  StlHashMultiMap(Allocator const& a, size_type n) : HashMultiMap(n, HashFun(), CompareFun(), a) {}
  StlHashMultiMap(Allocator const& a, size_type n, HashFun const& h) : HashMultiMap(n, h, CompareFun(), a) {}
  StlHashMultiMap(Allocator const& a, size_type n, HashFun const& h, CompareFun const& c) : HashMultiMap(n, h, c, a) {}

  bool has(const KeyT& k) const { return (find(k) != StlHashMultiMap<KeyT,ValueT,HashCompareFun,Allocator>::end()); };
};
#endif


} //namespace Jitrino 

#endif // _STL_H_
