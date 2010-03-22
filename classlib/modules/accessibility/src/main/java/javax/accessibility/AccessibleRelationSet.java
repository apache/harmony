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

package javax.accessibility;

import java.util.Iterator;
import java.util.Vector;

public class AccessibleRelationSet {

    protected Vector<AccessibleRelation> relations;

    public AccessibleRelationSet() {
        super();
    }

    public AccessibleRelationSet(final AccessibleRelation[] relations) {
        if (relations.length != 0) {
            this.relations = new Vector<AccessibleRelation>(relations.length);
            for (AccessibleRelation relation : relations) {
                add(relation);
            }
        }
    }

    public boolean add(AccessibleRelation relation) {
        initStorage();

        AccessibleRelation currentRelation = get(relation.getKey());
        if (currentRelation == null) {
            relations.addElement(relation);
            return true;
        }

        Object[] currentTarget = currentRelation.getTarget();
        int combinedLength = currentTarget.length + relation.getTarget().length;
        Object[] combinedTarget = new Object[combinedLength];

        for (int i = 0; i < currentTarget.length; i++) {
            combinedTarget[i] = currentTarget[i];
        }
        int index = 0;
        for (int i = currentTarget.length; i < combinedLength; i++) {
            combinedTarget[i] = relation.getTarget()[index++];
        }

        currentRelation.setTarget(combinedTarget);

        return true;
    }

    public void addAll(AccessibleRelation[] relations) {
        if (relations.length != 0) {
            initStorage();
            for (AccessibleRelation relation : relations) {
                add(relation);
            }
        }
    }

    public boolean remove(AccessibleRelation relation) {
        return relations != null && relations.remove(relation);
    }

    public void clear() {
        if (relations != null) {
            relations.clear();
        }
    }

    public int size() {
        return relations == null ? 0 : relations.size();
    }

    public boolean contains(String key) {
        return get(key) != null;
    }

    public AccessibleRelation get(String key) {
        if (relations == null) {
            return null;
        }
        for (AccessibleRelation rel : relations) {
            if (key.equals(rel.key)) {
                return rel;
            }
        }
        return null;
    }

    public AccessibleRelation[] toArray() {
        return relations == null ? new AccessibleRelation[0] : relations
                .toArray(new AccessibleRelation[relations.size()]);
    }

    @Override
    public String toString() {
        if (relations == null) {
            return ""; //$NON-NLS-1$
        }
        StringBuilder result = new StringBuilder();
        for (Iterator<AccessibleRelation> it = relations.iterator(); it
                .hasNext();) {
            result.append(it.next());
            if (it.hasNext()) {
                result.append(","); //$NON-NLS-1$
            }
        }
        return result.toString();
    }

    private void initStorage() {
        if (relations == null) {
            relations = new Vector<AccessibleRelation>();
        }
    }
}
