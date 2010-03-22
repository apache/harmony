/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

/**
 * @file
 * Util.h
 *
 * Add some util classes to replace C++ STL
 */

#ifndef _UTIL_H_
#define _UTIL_H_
#define JDWP_DEFAULT_VECTOR_SIZE 32

#include "stdlib.h"
#include "string.h"
#include "jni.h"
#include "PacketParser.h"

namespace jdwp{
     // element in queue
     template <class U>
     struct Element {
         U* element;
         Element* previous;
         Element* next;
     };

     // A queue/deque class for jdwp use
     class JDWPQueue {
         private:
         Element<EventComposer>* header;
         Element<EventComposer>* tail;
         jint queuesize;

         public:

         /**
          * A constructor.
          * Creates a new instance.
          *
          */
         JDWPQueue(){
            header = NULL;
            tail = NULL;
            queuesize = 0;
         }

         /**
          * A destructor.
          * Destroys the given instance.
         */
         ~JDWPQueue(){
            clear();
         }

         void clear(){
             Element<EventComposer>* delp, * walkp = header;
             while((delp = walkp) != NULL){
                walkp = walkp->next;
                free(delp);
             }
	     header = tail = NULL;
             queuesize = 0;
         }

         bool empty(){
             return queuesize == 0;
         }

         EventComposer* front(){
            if (header != NULL){
                 return header->element;
            }
            return NULL;
         }

         void pop(){
             Element<EventComposer>* ret = header;
             if (queuesize > 0){
                header = header->next;
                if (header != NULL){
                    header->previous = NULL;
                } else {
		    // the list is empty, the tail is null as the header 
                    tail = NULL;
		}
                queuesize--;
		free(ret);
             }
         }

         jint size(){
             return queuesize;
         }

         void push(EventComposer* ec){
             Element<EventComposer>* newElement = (Element<EventComposer>*)malloc(sizeof(Element<EventComposer>));
             newElement->element = ec;
             newElement->next = NULL;

             if (tail == NULL){
                // header should be null as well    
                tail = header = newElement;
                header->previous = NULL;
             } else {
                newElement->previous = tail;
                tail->next = newElement;
                tail = newElement;
	     }
             queuesize ++;
         }
     };

     // a simple jdwp vector, replace the standard lib one.
     // WARNING: only for current JDWP using now, no guarantee its correctness for new functions.
     template <class T>
     class JDWPVector {

         private:
         T** elements;
         jint vectorsize;
	 jint vectorcount;
	
         public:
         // a simple jdwp vector iterator, replace the standard lib one.
         // WARNING: only for current JDWP using now, no guarantee its correctness for new functions.
	 // WARNING: it do not check the synchronization of its vector
	 class iterator{
		 private:
                        int currentIndex;
			JDWPVector* vector;
	         public:
			iterator(JDWPVector* const jvector){
                                 vector = jvector;
                                 currentIndex = 0;
			}

			T* getNext(){
				if (currentIndex < vector->vectorcount){
				        T* ret = vector->getIndexof(currentIndex);
					currentIndex ++;
					return ret;
				}
				return NULL;
			}

			bool hasNext(){
				if (currentIndex < vector->vectorcount){
					return true;
				}
				// Tricky here: make hasCurrent correct here.
				// In iterator search, we call hasNext before getNext
				// however, if check hasNext is false, we should set 
				// hasCurrent to false to show we've searched the whole iterator
				currentIndex++;
				return false;
			}

			// Note: c++ iterator is different from java vection, it can take current element
			// again and again, in this case we can check if we have a current element, then
			// use getCurrent()
			bool hasCurrent(){
                                return (currentIndex >0 && currentIndex <= vector->vectorcount);
			}

			T* getCurrent(){
				return vector->getIndexof(currentIndex -1);
			}

			jint getIndex(){
				return currentIndex - 1;
			}

			// avoid a compiler error, reset this iterator
			void reset(){
				currentIndex = 0;
			}

			// do not move pointer
			void remove(){
				vector->remove(currentIndex - 1);
				backwards();
			}

			// step back
			// Warning: no check here, for inner use only
			void backwards(){
				currentIndex --;
			}

	 };

         /**
          * A constructor.
          * Creates a new instance.
          *
          */
         JDWPVector(){
	    // default size
            vectorsize = JDWP_DEFAULT_VECTOR_SIZE;
            vectorcount = 0;
            elements = (T**) malloc(sizeof(T*)*vectorsize);
         }

         /**
          * A destructor.
          * Destroys the given instance.
         */
         ~JDWPVector(){
             free(elements);
             vectorsize = 0;
             vectorcount = 0;
         }

	 // WARNING: no check if the slot is empty
	 void insert(int index, T* in){
            //T* newElement = (T*)malloc(sizeof(T));
            //memcpy(newElement,in,sizeof(T));
	    elements[index] = in;
	 }

	 // learn from  c++ vector, clear a slot but do not delete
	 void insertNULL(int index){
            elements[index] = NULL;
	 }

	 T* getIndexof(int index){
	    return elements[index];
	 }

         void clear(){
	     free(elements);
             vectorsize = JDWP_DEFAULT_VECTOR_SIZE;
             vectorcount = 0;
             elements = (T**) malloc(sizeof(T*)*vectorsize);
         }

        bool empty(){
             return vectorcount == 0;
        }

	T* back(){
	     return getIndexof(vectorcount-1);
	}

	iterator begin(){
	     return iterator(this);
	}

        jint size(){
            return vectorcount;
        }

        void push_back(T* ec){
           //T* newElement = (T*)malloc(sizeof(T));
           //memcpy(newElement,ec,sizeof(T));
           if (vectorcount >= vectorsize*0.75){
		// vector is full, extends the vector
                vectorsize = vectorsize << 1;
                T** newelements = (T**) malloc(sizeof(T *)*vectorsize);
		for (jint i = 0; i < vectorcount; i++){
                    newelements[i] = elements[i];
                }
	        free(elements);
		elements = newelements;
           }
           elements[vectorcount] = ec;
           vectorcount++;
        }

	void pop_back(){
		remove(vectorcount-1);
	}

	 // delete an vector element related to the iterator current element
	void erase(iterator iter){
	     jint eraseIndex = iter.getIndex();
             remove(eraseIndex);
        }

	void remove(int eraseIndex){
             if ((vectorcount<<2) < vectorsize && vectorsize > JDWP_DEFAULT_VECTOR_SIZE){
		    // vector has too small elements, compact the vector
		    vectorsize = vectorsize >> 1;
                    T** newelements = (T**) malloc(sizeof(T *)*vectorsize);
                    jint i = 0;
		    for (; i < eraseIndex; i++){
                        newelements[i] = elements[i];
                    }
		    for (; i < vectorcount - 1; i++){
                        newelements[i] = elements[i+1];
                    }
	            free(elements);
		    elements = newelements;
	     } else {
		    for (jint i = eraseIndex ; i < vectorcount - 1; i++){
                        elements[i] = elements[i+1];
                    }
	     }
             vectorcount--;
	 }
     };
}
#endif //_UTIL_H_
