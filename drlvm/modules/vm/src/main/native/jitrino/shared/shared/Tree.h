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

#ifndef _TREE_H_
#define _TREE_H_

#include "PrintDotFile.h"
#include "open/types.h"

#include <assert.h>
#include <string>

namespace Jitrino {

class Tree;

class TreeNode {
    friend class Tree;
public:
    TreeNode() : child(NULL), siblings(NULL), parent(NULL), preNum((U_32) -1), postNum((U_32) -1) {}
    void addChild(TreeNode* chd) {
        chd->parent = this;
        chd->siblings = child;
        child = chd;
    }

    virtual ~TreeNode() {}

    virtual void print(::std::ostream& os) { os << "T"; }
    virtual void printTag(::std::ostream& os) { print(os); }

    /**** utilities to print Dot files */

    virtual void printDotNodesInTreeNode(::std::ostream &os) { }

    void printDotTreeNode(::std::ostream& os) {
        printDotTreeNode(os,NULL);
    }

    void printDotTreeNode(::std::ostream& os, TreeNode *parent) {
        printTag(os);
        os << "[label= \""; 
        print(os);
        printDotNodesInTreeNode(os);
        os << "\"];";

        if (child != NULL)
            child->printDotTreeNode(os,this);
        if (siblings != NULL)
            siblings->printDotTreeNode(os,parent);

        // dump child and sibling edges
        if (child != NULL) {
            printTag(os);
            os << " -> "; child->printTag(os);
            os << ";" << ::std::endl;
        }
        if (siblings != NULL) {
            if (parent != NULL) {
                parent->printTag(os);
                os << " -> "; siblings->printTag(os);
                os << ";" << ::std::endl;
            }
        }
    }

    void printDotRank(::std::ostream &os) {
        // make all siblings are in the same rank
        if (siblings != NULL) {
            os << "{ rank = same; ";
            os << "\""; printTag(os); os << "\"; ";
            for (TreeNode* sb = siblings; sb != NULL; sb = sb->siblings) {
                os << "\""; sb->printTag(os); os << "\"; ";
            }
            os << "}" << ::std::endl;
        }

        // dump children's rank
        if (child != NULL)
            child->printDotRank(os);

        // dump rank of siblings' children
        for (TreeNode* sb = siblings; sb != NULL; sb = sb->siblings) {
            TreeNode *sibl = sb->child;
            if (sibl != NULL)
                sibl->printDotRank(os);
        }
    }

    void printIndentedNode(::std::ostream& os, const ::std::string& indentstr = "") {
        os << indentstr.c_str();
        print(os);
        os << ::std::endl;
        if(child) child->printIndentedNode(os, indentstr + "  ");
        if(siblings) siblings->printIndentedNode(os, indentstr);
    }

    U_32 getHeight() const {
        U_32 i = 0;
        if(child != NULL) {
            i = child->getHeight();

            for (TreeNode *siblings = child->siblings; siblings!=NULL; siblings = siblings->siblings) {
                U_32 j = siblings->getHeight();
                if(j > i) i = j;
            }
        }
        return i + 1;
    }

    U_32 getCount() const {
        U_32 i = 1;
        if(child != NULL)
            i += child->getCount();
        if(siblings != NULL)
            i += siblings->getCount();
        return i;
    }
    
    //return number of parents
    U_32 getDepth() const {
        return parent != NULL ? parent->getDepth() + 1 : 0; 
    }

    U_32 getPreNum() const { return preNum; }
    U_32 getPostNum() const { return postNum; }

    // Return true if this is a proper ancestor of n.
    bool isAncestorOf(TreeNode* n) {
        return (preNum < n->preNum) && (postNum > n->postNum); 
    }

protected:
    TreeNode* child;
    TreeNode* siblings;
    TreeNode* parent;
    U_32 preNum;
    U_32 postNum;
};


class Tree: public PrintDotFile {
public:
    Tree(): root(NULL) {}
    TreeNode* getRoot() {return root;}

    virtual void printDotFile(MethodDesc& mh, const char *suffix) {
        if (root == NULL) return;
        PrintDotFile::printDotFile(mh,suffix);
    }
    virtual void printDotBody() {
        if (root == NULL) return;
        root->printDotTreeNode(*os);
        //
        // lay out ranks
        //
        root->printDotRank(*os);
    }

    virtual void printIndentedTree(::std::ostream& os, const ::std::string& indentstr = "") {
        if(root == NULL) return;
        root->printIndentedNode(os, indentstr);
    }

    U_32 getHeight() const { return root==NULL ? 0 : root->getHeight(); }
    U_32 getCount() const { return root == NULL ? 0: root->getCount(); }

    // Return true if n1 is a proper ancestor of n2.
    bool isAncestor(const TreeNode* n1, const TreeNode* n2) const {
        return (n1->preNum < n2->preNum) && (n1->postNum > n2->postNum); 
    }

protected:
    void computeOrder() {
        U_32 preNum = 0;
        U_32 postNum = 0;
        computeNodeOrder(root, preNum, postNum);
        assert(preNum == postNum);
    }

    void computeNodeOrder(TreeNode* node, U_32& preNum, U_32& postNum) {
        node->preNum = preNum++;
        if(node->child != NULL)
            computeNodeOrder(node->child,preNum,postNum);
        node->postNum = postNum++;
        if(node->siblings != NULL)
            computeNodeOrder(node->siblings,preNum,postNum);
    }

    TreeNode *root;
};

} //namespace Jitrino 

#endif // _TREE_H_
