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

#ifndef ULIST_H
#define ULIST_H

#include <assert.h>

#ifdef UNIT_TEST
#include "unit_test.h"
#endif // UNIT_TEST

/**
 * Unsorted list container. Features:
 *     common insertion time O(1), worst case O(log N) if collection has N elements
 *     linear iteration with ability to erase elements as we go
 *     mapping from payload pointer to iterator O(log N)
 *     notification about moved elements
 *     memory overhead O(log N)
 *     interface similar to std::list
 *
 * BEWARE:
 *     element ordering is not preserved on modifications
 *     iterator-- may not usable for dereference operation
 */
template <class T>
class ulist {
    T* chunk;       // elements' store
    size_t capacity;    // capacity
    size_t used;    // number of used elements
    ulist<T>* next; // next in linked list of chunks

public:
    struct iterator {
        ulist<T> *current;
        int index;
        iterator()
            : current(NULL), index(0) {}
        iterator(ulist<T> *current, int index)
            : current(current), index(index) {}

        iterator & operator++(int) {
            assert(current);
            index++;
            assert(index >= 0);
            if ((size_t)index >= current->used) {
                index = 0;
                do {
                    current = current->next;
                    // skip emptied chunk during iteration,
                    // as they have no elements to iterate
                } while (current && current->used == 0);
            }
            assert(0 <= index); assert(current == NULL || ((size_t)index < current->used));
            return *this;
        }

        // BEWARE x-- is only good for subsequent x++
        iterator operator--(int) {
            assert(current);
            return iterator(current, index--);
        }

        T & operator*() {
            assert(0 <= index); assert((size_t)index < current->used);
            return current->chunk[index];
        }

        T* operator->() {
            return &current->chunk[index];
        }

        bool operator!=(iterator i) {
            return (current != i.current) || (index != i.index);
        }
    };

    ulist(size_t initial)
        : used(0), capacity(initial), next(NULL)
    {
        chunk = new T[capacity];
        assert(chunk && "out of memory");
    }

protected:
    // swap the chunk payload
    void swap(ulist<T>* other) {
        T* tmp_chunk = other->chunk;
        size_t tmp_capacity = other->capacity;
        size_t tmp_used = other->used;

        other->chunk = chunk;
        other->capacity = capacity;
        other->used = used;

        chunk = tmp_chunk;
        capacity = tmp_capacity;
        used = tmp_used;
    }

public:

    void push_back(T& t) {
        // allocate locally
        if (used < capacity) {
            chunk[used++] = t;
            return;
        }
        // try to find available space in existing chunks
        // approximate the collection size, as well as find the last
        size_t size = capacity;
        ulist<T>* other = next;
        ulist<T>* last = this;
        while (other) {
            if (other->used < other->capacity) break;
            size += other->capacity;
            last = other;
            other = other->next;
        }

        if (other) {
            // found the other chunk with available space
            swap(other);
            // should have space after swap
            assert(used < capacity);
            chunk[used++] = t;
            return;
        }

        // all existing chunks are full, allocate new one, double the capacity
        assert(last && last->next == NULL);
        last->next = new ulist<T>(size);
        assert(last->next && "out of memory");
        swap(last->next);

        assert(used == 0 && used < capacity);
        chunk[used++] = t;
    }

    T & back() {
        // addition is always performed on the current chunk
        assert(used > 0 && "can't use back() before push_back()");
        return chunk[used-1];
    }

    void erase(iterator i) {
        assert(0 <= i.index); assert((size_t)i.index < i.current->used);
        i.current->used--;

        // compact array if the erased element was not the last
        if ((size_t)i.index < i.current->used) {
            i.current->chunk[i.index] = i.current->chunk[i.current->used];
            // moving element notification
            element_moved(&i.current->chunk[i.current->used], &i.current->chunk[i.index]);
        }
    }

    iterator find(T* t) {
        ulist<T>* current = this;
        while (current) {
            if (current->chunk <= t && t < current->chunk + current->capacity) {
                int index = (int)(t - current->chunk);
                assert(0 <= index && (size_t)index < current->used && "deleted or moved element");
                return iterator(current, (int)(t - current->chunk));
            }
            current = current->next;
        }
        assert(!"can't find element in collection");
        return iterator(NULL, 0);
    }

    // iteration is not compatible with adding elements !!!
    iterator begin() {
        ulist<T> *current = this;
        while (current && current->used == 0) {
            current = current->next;
            // skip empty chunk during iteration,
            // as they have no elements to iterate
        }
        return iterator(current, 0);
    }

    iterator end() {
        return iterator(NULL, 0);
    }

    // returns the overall size of the unsorted list
    size_t size() {
        return used + (next ? next->size() : 0);
    }
};

#endif // ULIST_H

#ifdef UNIT_TEST

TEST(push_few) {
    int N = 10;
    ulist<int> list(N);
    for (int j = 0; j < N; j++) {
        list.push_back(j);
    }
    ulist<int>::iterator i;
    int c = 0;
    for (i = list.begin(); i != list.end(); i++) {
        int x = *i;
        assert(0 <= *i && *i < N);
        c++;
    }
    TRACE("expected " << N << " elements, got " << c);
    assert(c == N);
    for (int j = 0; j < N; j++) {
        list.push_back(j);
    }
    c = 0;
    for (i = list.begin(); i != list.end(); i++) {
        assert(0 <= *i && *i < N);
        c++;
    }
    TRACE("expected " << (2*N) << "  elements, got " << c);
    assert(c == 2*N);
    assert(c == list.size());
}

TEST(push_many) {
    ulist<int> list(10);
    int N = 10000;
    int c = 0;
    for (int j = 0; j < N; j++) {
        list.push_back(j);
        c++;
    }
    TRACE("expected " << N << " elements, got " << c);
    assert(c == N);
}

TEST(back) {
    ulist<int> list(10);
    int N = 773;
    for (int j = 0; j < N; j++) {
        list.push_back(j);
        assert(j == list.back());
    }
}

TEST(erase) {
    ulist<int> list(10);
    int N = 78;
    for (int j = 0; j < N; j++) {
        list.push_back(j);
    }
    ulist<int>::iterator i;
    for (i = list.begin(); i != list.end(); i++) {
        if (*i % 2 == 0) {
            list.erase(i--);
        }
    }
    int c = 0;
    for (i = list.begin(); i != list.end(); i++) {
        assert(*i % 2 != 0);
        c++;
    }
    assert(c == N/2);
}

TEST(erase_random) {
    ulist<int> list(3);
    int N = 77;
    int j;
    for (j = 0; j < N; j++) {
        list.push_back(j);
    }
    ulist<int>::iterator i;
    int removed = 0;
    for (i = list.begin(), j = 0; i != list.end(); i++, j++) {
        if (j % 3 == 0) {
            list.erase(i--);
            removed++;
        }
    }
    assert(j == N);
    N -= removed;
    removed = 0;
    for (i = list.begin(), j = 0; i != list.end(); i++, j++) {
        if (j % 3 == 0) {
            list.erase(i--);
            removed++;
        }
    }
    assert(j == N);
    N -= removed;
    removed = 0;
    for (i = list.begin(), j = 0; i != list.end(); i++, j++) {
        if (j < N/3 || j > N-N/3) {
            list.erase(i--);
            removed++;
        }
    }
    assert(j == N);
    N -= removed;
    removed = 0;
    for (i = list.begin(), j = 0; i != list.end(); i++, j++) {
        list.erase(i--);
        removed++;
    }
    assert(j == N);
    assert(removed == N);
    for (i = list.begin(); i != list.end(); i++) {
        assert(!"should not have any elements");
    }
}

TEST(erase_all) {
    ulist<int> list(10);
    int N = 33;
    for (int j = 0; j < N; j++) {
        list.push_back(j);
    }
    ulist<int>::iterator i;
    for (i = list.begin(); i != list.end(); i++) {
        list.erase(i--);
    }
    assert(list.size() == 0);
}

TEST(erase_all_and_back) {
    ulist<int> list(10);
    int N = 333;
    int j;
    for (j = 0; j < N; j++) {
        list.push_back(j);
    }
    ulist<int>::iterator i;
    for (i = list.begin(); i != list.end(); i++) {
        list.erase(i--);
    }
    assert(list.size() == 0);
    for (j = 0; j < N*2; j++) {
        list.push_back(j);
        assert(j == list.back());
    }
    for (j = 0, i = list.begin(); i != list.end(); i++, j++) {
        if (j>1 && j<N*2) list.erase(i--);
    }
    assert(list.size() == 2);
    for (j = 0; j < N*2; j++) {
        list.push_back(j);
        assert(j == list.back());
    }
    assert(list.size() == 2+N*2);
}

int **elements = NULL;

// receive notifications about list elements being moved
void element_moved(int* from, int* to) {
    assert(*from == *to);
    // only for the relevant test cases
    if (!elements) return;
    // and update elements array to the new element location
    elements[*from] = to;
}

TEST(find) {
    ulist<int> list(10);
    int N = 333;
    elements = new int*[N]; assert(elements && "out of memory");
    int j;
    // fill the ulist with numbers
    for (j = 0; j < N; j++) {
        list.push_back(j);
        elements[j] = &list.back();
    }

    ulist<int>::iterator i;
    for (j = 0; j < N; j++) {
        i = list.find(elements[j]);
        assert(j == *i);
    }

    // drop some of the elements
    for (j = 0, i = list.begin(); i != list.end() && j < N; i++, j++) {
        if (N/3 < j && j < 2*N/3 || j%3 == 0) {
            elements[*i] = NULL;
            list.erase(i--);
        }
    }

    for (j = 0; j < N; j++) {
        if (elements[j]) {
            i = list.find(elements[j]);
            assert(j == *i);
        }
    }

    delete[] elements;
    elements = NULL;
}

TEST(iterate_empty) {
    ulist<int> list(10);
    assert(!(list.begin() != list.end()));
}

TEST(iterate_emptied) {
    ulist<int> list(10);
    int N = 33;
    int j;
    for (j = 0; j < N; j++) {
        list.push_back(j);
    }
    ulist<int>::iterator i;
    for (i = list.begin(); i != list.end(); i++) {
        list.erase(i--);
    }
    assert(!(list.begin() != list.end()));
}

#endif // UNIT_TEST
