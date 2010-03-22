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
 * @author Dennis Ushakov
 */
package javax.swing;

public class SizeSequence {    
    private int[] tree;   
    private int sizesCount;
    
    public SizeSequence() {
        this(new int[0]);
    }
    
    public SizeSequence(final int numEntries) {
        this(numEntries, 0);
    }
    
    public SizeSequence(final int numEntries, final int value) {
        int[] sizes = new int[numEntries];
        for (int i = 0; i < numEntries; i++) {
            sizes[i] = value;
        }
        setSizes(sizes);
    }
    
    public SizeSequence(final int[] sizes) {
        setSizes(sizes);
    }
    
    public void setSizes(final int[] sizes) {
        if (sizes.length == 0) {
            tree = new int[0];
            sizesCount = 0; 
            return;
        }
        int upper = 1;
        while (upper - 1 < sizes.length) {
            upper <<= 1; 
        }
        tree = new int[upper - 1];        
        sizesCount = sizes.length;
        setSizesImpl(sizes, 0, upper - 1, 0);
}
    
    public int[] getSizes() {
        if (sizesCount == 0) {
            return new int[0];
        }
        int[] allSizes = new int[tree.length];
        getSizesImpl(allSizes, 0, tree.length - 1);        
        int[] result = new int[sizesCount];
        System.arraycopy(allSizes, 0, result, 0, sizesCount);
        return result;
    }    
    
    public int getPosition(final int index) {
        if (index < 0 || sizesCount == 0) {
            return 0;
        }
        if (index >= sizesCount) {
            return tree[(tree.length - 1) / 2];
        }
        int position = 0;
        int left = 0;
        int right = tree.length - 1;
        int cur = (left + right) / 2;
      
        do {
            if (cur > index) {
                right = cur - 1;
            } else if (cur < index) {
                position += tree[cur];
                position -= (right + cur + 1) / 2 == cur ? 0 : tree[(right + cur + 1) / 2];
                left = cur + 1;
            }
            cur = (left + right) / 2;
        } while(cur != index);
        position += (cur + left) / 2 == cur ? 0 : tree[(cur + left) / 2];
        return position;
    }
    
    public int getIndex(final int position) {                
        if (position < 0 || sizesCount == 0) {
            return 0;
        }
        if (position >= tree[(tree.length - 1) / 2]) {
            return sizesCount;
        }
        
        int pos = position;
        int left = 0;
        int right = tree.length - 1;
        int cur = (left + right) / 2;
        
        while (left != right) {
            if (pos - tree[(cur + left) / 2] < 0) {
                right = cur - 1;
            } else {
                pos -= tree[(cur + left) / 2];
                int size = tree[cur];                
                size -= (right + cur + 1) / 2 == cur ? 0 : tree[(right + cur + 1) / 2];
                size -= (cur + left) / 2 == cur ? 0 : tree[(cur + left) / 2];
                if (pos < size) {
                    return cur < sizesCount ? cur : sizesCount;
                } else {
                    pos -= size;
                    left = cur + 1;
                }
            }
            cur = (left + right) / 2;
        }
        return cur < sizesCount ? cur : sizesCount; 
    }
    
    public int getSize(final int index) {
        if (!isValidIndex(index)) {
            return 0;
        }        
        int result = tree[index];
        int left = 0;
        int right = tree.length - 1;
        int cur = (left + right) / 2;
        while (cur != index) {
            if (cur < index) {
                left = cur + 1; 
            } 
            if (cur > index) {
                right = cur - 1;
            }                
            cur = (left + right) / 2;
        }        
        result -= (right + cur + 1) / 2 == cur ? 0 : tree[(right + cur + 1) / 2];
        result -= (cur + left) / 2 == cur ? 0 : tree[(cur + left) / 2];
        return result;
    }
    
    public void setSize(final int index, final int size) {
        if (!isValidIndex(index)) {
            return;
        }
        int delta = size - getSize(index); 
        int left = 0;
        int right = tree.length - 1;
        int cur = (left + right) / 2;
        tree[cur] += delta;
        while (cur != index) {
            if (cur < index) {
                left = cur + 1; 
            } 
            if (cur > index) {
                right = cur - 1;
            }                
            cur = (left + right) / 2;
            tree[cur] += delta;
        }
    }
    
    public void insertEntries(final int start, final int length, final int value) {
        int[] newSizes = new int[sizesCount + length];
        int[] sizes = getSizes();
        System.arraycopy(sizes, 0, newSizes, 0, start);
        for (int i = start; i < start + length; i++) {
            newSizes[i] = value;
        }
        System.arraycopy(sizes, start, newSizes, start + length, sizes.length - start);        
        setSizes(newSizes);
    }
    
    public void removeEntries(final int start, final int length) {
        int[] newSizes = new int[sizesCount - length];
        int[] sizes = getSizes();
        System.arraycopy(sizes, 0, newSizes, 0, start);
        System.arraycopy(sizes, start + length, newSizes, start, sizes.length - start - length);
        setSizes(newSizes);
    }
    
    private boolean isValidIndex(final int index) {
        return (index >= 0 && index < sizesCount);
    }    
    
    private int setSizesImpl(final int[] sizes, final int left, final int right, final int depth) {
        int cur = (right + left) / 2;
        int size = (cur < sizes.length) ? sizes[cur] : 0;
        if ((right - left) / 2 > 0) {
            tree[cur] = size + setSizesImpl(sizes, left, cur - 1, depth + 1) + setSizesImpl(sizes, cur + 1, right, depth + 1);
        } else if ((right - left) / 2 == 0) {            
            tree[cur] = size;
        } else {
            return 0;
        }
        return tree[cur];
    }
    
    private int getSizesImpl(final int[] sizes, final int left, final int right) {        
        int cur = (right + left) / 2;        
        int length = tree[cur];
        if ((right - left) / 2 > 0) {
            sizes[cur] = length - getSizesImpl(sizes, left, cur - 1) - getSizesImpl(sizes, cur + 1, right);
        } else if ((right - left) / 2 == 0) {            
            sizes[cur] = length;
        }
        return (right - left) / 2 < 0 ? 0 : length;
    }        
}
