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

#ifndef __HASHTABLE_H__
#define __HASHTABLE_H__

#include <assert.h>
#include "cxxlog.h"
#include <map>

// FIXME: we expect POINTER_SIZE_INT is already defined by some includes...

#ifdef USE_HASH_CLASS
template <class Key, unsigned hashtable_len>
class HashMaker
{
public:
    HashMaker() {}

    unsigned operator ()(Key key) {
        return HashFunc(key);
    }
private:
    unsigned HashFunc(Key key)
    {
        return (unsigned) ((((POINTER_SIZE_INT)key)>>2)%hashtable_len);
    }
};
#else
template <class Key, unsigned hashtable_len>
static unsigned default_hash_func(Key key)
{
    return (unsigned) ((((POINTER_SIZE_INT)key)>>2)%hashtable_len);
}
#endif

static const unsigned DEFAULT_HASH_TABLE_LENGTH = 101;

/*
 * Users of this hash table, please,
 * NOTE: Elem MUST be ASSIGNABLE or COPY-CONSTRUCTABLE
 */
template <class Key, class Elem, unsigned hashtable_len = DEFAULT_HASH_TABLE_LENGTH>
class HashTable
{
    template <class _Key, class _Elem>
    class IntHashItem
    {
    public:
        _Key m_key;
        _Elem m_data;
        IntHashItem* m_prev;
        IntHashItem* m_next;
        IntHashItem() : m_prev(NULL), m_next(NULL) {}
    };

    template <class _Key, class _Elem, unsigned hashlink_len>
    class IntHashLink
    {
    public:
        IntHashItem<_Key, _Elem> m_entry[hashlink_len];
        IntHashLink<_Key, _Elem, hashlink_len>* m_next;
        IntHashLink() : m_next(NULL) {
            m_entry[0].m_prev = NULL;
            m_entry[0].m_next = &(m_entry[1]);
            for( unsigned i = 1; i < hashlink_len - 1; i++ )
            {
                m_entry[i].m_prev = &(m_entry[i-1]);
                m_entry[i].m_next = &(m_entry[i+1]);
            }
            m_entry[hashlink_len - 1].m_prev = &(m_entry[hashlink_len - 2]);
            m_entry[hashlink_len - 1].m_next = NULL;
        }
    };

    template <class _Key, class _Elem>
    class IntHashEntry
    {
    public:
        IntHashItem<_Key, _Elem>* m_free;
        IntHashItem<_Key, _Elem>* m_used;
        IntHashLink<_Key, _Elem, 32>* m_link;
        IntHashEntry() : m_free(NULL), m_used(NULL), m_link(NULL) {}
    };

private:
    unsigned m_itemsnumber;
    unsigned m_hashtable_len;
    IntHashEntry<Key, Elem> m_hashed[hashtable_len];
#ifdef USE_HASH_CLASS
    HashMaker<Key, hashtable_len>* m_hashfunc;
#else
    typedef unsigned (*HashFunc_t)(Key key);
    HashFunc_t m_hashfunc;
#endif

public:
    HashTable(
#ifdef USE_HASH_CLASS
        HashMaker<Key, hashtable_len>* fnptr = 0
#else
        HashFunc_t fnptr = 0
#endif
        ):m_itemsnumber(0), m_hashtable_len(hashtable_len), m_hashfunc(fnptr) {
        if(!m_hashfunc) {
#ifdef USE_HASH_CLASS
            m_hashfunc = new HashMaker<Key, hashtable_len>();
#else
            m_hashfunc = (HashFunc_t)default_hash_func<Key, hashtable_len>;
#endif
        }
    }

    Elem* Lookup(Key key) {
        // hash on key
        unsigned h = (*m_hashfunc)(key);
        IntHashEntry<Key, Elem>* curEntry = &(m_hashed[h]);
        if(!curEntry->m_link) return 0; // this HT entry is empty
        for( IntHashItem<Key, Elem>* entry = curEntry->m_used; entry != 0; entry = entry->m_next ) {
            if(entry->m_key == key) return &(entry->m_data);
        }
        return 0;
    }

    Elem* Insert(Key key, Elem& data, bool unique = true) {
        if(unique) {
            // check that this hash table does not contain element for this key
            Elem* elem = Lookup(key);
            if(elem) {
                DIE(("Element is inserted second time"));
            }
        }
        unsigned h = (*m_hashfunc)(key);
        IntHashEntry<Key, Elem>* curEntry = &(m_hashed[h]);
        // setup hash entry if first used
        if(curEntry->m_link == 0) {
            curEntry->m_link = new IntHashLink<Key, Elem, 32>();
            curEntry->m_free = &(curEntry->m_link->m_entry[0]);
        }
        IntHashItem<Key, Elem>* use = curEntry->m_free;
        // only one free element left in this link (which will be used now); allocate more
        if( curEntry->m_free->m_next == 0 ) {
            IntHashLink<Key, Elem, 32>* newL = new IntHashLink<Key, Elem, 32>();
            if( newL == 0 ) return NULL;
            newL->m_next = curEntry->m_link;
            curEntry->m_free = &(newL->m_entry[0]);
        } else {
            curEntry->m_free = curEntry->m_free->m_next;
        }
        use->m_prev = 0;
        use->m_next = curEntry->m_used;
        if( use->m_next ) use->m_next->m_prev = use;
        curEntry->m_used = use;
        use->m_key = key;
        use->m_data = data;
        m_itemsnumber++;
        return &(use->m_data);
    }

    void Remove(Key key) {
        // 2005/06/21 psrebriy: need to remove entry with key 0!
        // if(key == 0) return;
        // hash on string name address
        unsigned h = (*m_hashfunc)(key);
        IntHashEntry<Key, Elem>* curEntry = &(m_hashed[h]);

        for(IntHashItem<Key, Elem>* entry = curEntry->m_used; entry != NULL; entry = entry->m_next) {
            if(entry->m_key == key) {
                // found; remove
                entry->m_key = NULL;
                // first, unlink it from used list
                if(entry->m_prev != NULL)
                    entry->m_prev->m_next = entry->m_next;
                else // entry is curEntry->m_used; move it on
                    curEntry->m_used = entry->m_next;
                if(entry->m_next) entry->m_next->m_prev = entry->m_prev;
                // then, link it to free list
                entry->m_prev = NULL;
                entry->m_next = curEntry->m_free;
                if(curEntry->m_free) curEntry->m_free->m_prev = entry;
                curEntry->m_free = entry;
                // decrement number of classes registered in this table
                m_itemsnumber--;
                return;
            }
        }
        // this table entry is empty; thus contains no data at all
        DIE(("Trying to remove data which was not inserted before"));
    }

    unsigned GetItemCount() { return m_itemsnumber; }

    // ppervov: temporary - simple iterators
    //          will be removed after class enumeration will be changed
    Elem* get_first_item()
    {
        for(unsigned i = 0; i < hashtable_len; i++) {
            if(m_hashed[i].m_used != NULL) {
                IntHashItem<Key, Elem>* first = m_hashed[i].m_used;
                if(!first) continue;
                return &(first->m_data);
            }
        }
        return NULL;
    }

    Elem* get_next_item(Key prev_key, Elem& prev)
    {
        // hash on string name address
        unsigned h = (*m_hashfunc)(prev_key), i;
        IntHashItem<Key, Elem>* hit;
        for( hit = m_hashed[h].m_used; hit != NULL; hit = hit->m_next ) {
            if(hit->m_data == prev) {
                break;
            }
        }
        assert(hit != NULL);
        for(i = h, hit = hit->m_next; i < hashtable_len; i++, hit = m_hashed[i].m_used)
        {
            if(!hit) continue;
            IntHashItem<Key, Elem>* first = hit;
            if(!first) continue;
            return &(first->m_data);
        }
        return NULL;
    }
};

// MapEx based on std::map and customized to repeat HashTable functionality
template <class Key, class Elem>
class MapEx : public std::map<Key, Elem >
{
public:
    inline Elem* Lookup(Key key)
    {
        typename std::map<Key, Elem >::iterator it = this->find(key);
        if (it == this->end())
            return NULL;
        else 
            return &(it->second);
    }
    inline Elem* Insert(Key key, Elem& elem) 
    {
        // typename is used to avoid g++ warning
        std::pair<typename std::map<Key, Elem >::iterator, bool> pr = 
            this->insert(std::pair<Key, Elem >(key, elem));
        if (pr.second)
            return &(pr.first->second);
        else
        {
            DIE(( "Element was inserted second time in MapEx!"));
            return NULL; // not reachable; to satisfy compiler warning
        }
    }
    inline void Remove(Key key)
    {
        this->erase(key);
    }
    inline unsigned int GetItemCount()
    {
        return (unsigned int)(this->size());
    }
};

#endif // __HASHTABLE_H__
