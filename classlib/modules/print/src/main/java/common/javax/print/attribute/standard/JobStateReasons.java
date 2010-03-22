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

package javax.print.attribute.standard;

import java.util.Collection;
import java.util.HashSet;
import javax.print.attribute.Attribute;
import javax.print.attribute.PrintJobAttribute;

public final class JobStateReasons extends HashSet<JobStateReason> implements PrintJobAttribute {
    private static final long serialVersionUID = 8849088261264331812L;

    public JobStateReasons() {
        super();
    }

    public JobStateReasons(Collection<JobStateReason> collection) {
        super();
        for (JobStateReason reason : collection) {
            add(reason);
        }
    }

    public JobStateReasons(int initialCapacity) {
        super(initialCapacity);
    }

    public JobStateReasons(int initialCapacity, float loadFactor) {
        super(initialCapacity, loadFactor);
    }

    @Override
    public boolean add(JobStateReason reason) {
        if (reason == null) {
            throw new NullPointerException("Null JobStateReason");
        }
        return super.add(reason);
    }

    public final Class<? extends Attribute> getCategory() {
        return JobStateReasons.class;
    }

    public final String getName() {
        return "job-state-reasons";
    }
}
