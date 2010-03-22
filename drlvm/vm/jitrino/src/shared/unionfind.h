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

namespace Jitrino {

struct UnionFind {
private:
    UnionFind(const UnionFind &other) : parent(0), rank(1) {
        assert(0);
    }
    UnionFind &operator=(const UnionFind &other) { assert(0); return *this; };
public:
    UnionFind *parent;
    U_32 rank;
    UnionFind() : parent(0), rank(1) { parent = this; };
    void link(UnionFind *other) {
        assert(other);

        UnionFind *aroot = find();
        UnionFind *broot = other->find();

        if (aroot->rank < broot->rank) {
            aroot->parent = broot;
        } else {
            if (aroot->rank == broot->rank) {
                aroot->rank += 1;
            }
            broot->parent = aroot;
        }
    };

#ifdef UNIONFIND_PATH_HALVING
    UnionFind *find() {
        UnionFind *root = this;

        UnionFind *rparent = root->parent;
        UnionFind *grandparent = rparent->parent;
        while (grandparent != root) {
            root = root->parent = grandparent;
            root = grandparent;
            rparent = root->parent;
            grandparent = rparent->parent;
        }
        return parent;
    }
#else
    UnionFind *find() {
        if (parent == this) return this;
        else {
            UnionFind *root = parent;
            while (root->parent != root)
                root = root->parent;
            UnionFind *iter = this;
            while (iter->parent != iter) {
                UnionFind *next = iter->parent;
                iter->parent = root;
                iter = next;
            }
            return parent;
        }
    }
#endif

};

} //namespace Jitrino 
